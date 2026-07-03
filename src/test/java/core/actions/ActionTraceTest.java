package core.actions;

import core.actions.trace.ActionTrace;
import core.actions.trace.TraceStatus;
import core.engine.LocatorDescriptor;
import core.engine.LocatorStrategy;
import core.engine.UIEngine;
import core.interactions.hooks.After;
import core.interactions.hooks.Before;
import elements.api.capability.Clickable;
import elements.api.capability.Typeable;
import elements.meta.ElementRole;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for the action execution trace ({@link HookedAction#performAndTrace}).
 *
 * <p>Verifies that the trace data model captures hook order, timing, status,
 * and failure information correctly — and that raw() executes without
 * going through the traced path.</p>
 */
public class ActionTraceTest {

    private static final LocatorDescriptor STUB_DESCRIPTOR =
            new LocatorDescriptor("//*[@id='stub']", LocatorStrategy.XPATH);

    private static final Clickable CLICKABLE = new Clickable() {
        @Override public String getTriggerLocator()  { return "CLICK_KEY"; }
        @Override public String getExternalFileName() { return "stub.json"; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    };

    private static final Typeable TYPEABLE = new Typeable() {
        @Override public String getInputLocator()     { return "INPUT_KEY"; }
        @Override public String getExternalFileName() { return "stub.json"; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    };

    private UIEngine stubEngine;

    @BeforeMethod
    public void setUp() {
        HookedAction.clearLastTrace();
        stubEngine = buildEngine();
    }

    @AfterMethod
    public void tearDown() {
        HookedAction.clearLastTrace();
    }

    // ── Successful trace ──────────────────────────────────────────────────────

    @Test
    public void safeClickTrace_recordsCorrectHookNamesAndStatus() {
        HookedAction hooked = new HookedAction(
                ElementActions.of(CLICKABLE, ElementRole.TRIGGER, (e, d) -> {}),
                STUB_DESCRIPTOR,
                List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE),
                List.of(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT),
                "SAFE");

        ActionTrace trace = hooked.performAndTrace(stubEngine);

        assertEquals(trace.status(), TraceStatus.SUCCESS);
        assertEquals(trace.profileName(), "SAFE");
        assertEquals(trace.beforeHooks(), List.of("WAIT_FOR_ELEMENT_CLICKABLE"));
        assertEquals(trace.afterHooks(),  List.of("WAIT_FOR_ANGULAR_LOADER", "HIGHLIGHT_ELEMENT"));
        assertNull(trace.failure());
        assertTrue(trace.durationMs() >= 0);
    }

    @Test
    public void safeTypeTrace_recordsElementAndOperation() {
        Action base = ElementActions.of(TYPEABLE, ElementRole.INPUT, (e, d) -> {});
        HookedAction hooked = new HookedAction(
                base, STUB_DESCRIPTOR,
                List.of(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE),
                List.of(After.HIGHLIGHT_ELEMENT),
                "SAFE");

        ActionTrace trace = hooked.performAndTrace(stubEngine);

        assertEquals(trace.operation(), "type");
        assertEquals(trace.status(), TraceStatus.SUCCESS);
    }

    // ── Hook failure ──────────────────────────────────────────────────────────

    @Test
    public void beforeHookThrows_recordsHookFailedStatus() {
        RuntimeException boom = new RuntimeException("hook exploded");
        HookedAction hooked = new HookedAction(
                (e) -> fail("action must not run"),
                STUB_DESCRIPTOR,
                List.of((e, d) -> { throw boom; }),
                List.of(),
                "SAFE");

        assertThrows(RuntimeException.class, () -> hooked.performAndTrace(stubEngine));

        ActionTrace trace = HookedAction.lastTrace();
        assertNotNull(trace);
        assertEquals(trace.status(), TraceStatus.HOOK_FAILED);
        assertSame(trace.failure(), boom);
        assertFalse(trace.beforeHooks().isEmpty(), "Failing hook name must be recorded");
        assertTrue(trace.afterHooks().isEmpty(), "After hooks must not run when before hook fails");
    }

    @Test
    public void afterHookThrows_recordsHookFailedStatus() {
        RuntimeException boom = new RuntimeException("after hook exploded");
        HookedAction hooked = new HookedAction(
                (e) -> {},
                STUB_DESCRIPTOR,
                List.of(),
                List.of((e, d) -> { throw boom; }),
                "SAFE");

        assertThrows(RuntimeException.class, () -> hooked.performAndTrace(stubEngine));

        ActionTrace trace = HookedAction.lastTrace();
        assertNotNull(trace);
        assertEquals(trace.status(), TraceStatus.HOOK_FAILED);
        assertSame(trace.failure(), boom);
    }

    // ── Action failure ────────────────────────────────────────────────────────

    @Test
    public void actionThrows_recordsFailedStatus() {
        RuntimeException boom = new RuntimeException("action failed");
        HookedAction hooked = new HookedAction(
                (e) -> { throw boom; },
                STUB_DESCRIPTOR,
                List.of(Before.HIGHLIGHT_ELEMENT),
                List.of(After.HIGHLIGHT_ELEMENT),
                "SAFE");

        assertThrows(RuntimeException.class, () -> hooked.performAndTrace(stubEngine));

        ActionTrace trace = HookedAction.lastTrace();
        assertNotNull(trace);
        assertEquals(trace.status(), TraceStatus.FAILED);
        assertSame(trace.failure(), boom);
        assertFalse(trace.beforeHooks().isEmpty(), "Before hooks that ran must be recorded");
        assertTrue(trace.afterHooks().isEmpty(), "After hooks must not run when action fails");
    }

    // ── raw() path ────────────────────────────────────────────────────────────

    @Test
    public void raw_doesNotGoThroughHookChain() {
        Action rawAction = CLICKABLE.click().raw();

        assertFalse(rawAction instanceof HookChainAction,
                "raw() with empty hooks must return the original action, bypassing HookChainAction");
    }

    @Test
    public void raw_executesWithoutEmittingTrace() {
        CLICKABLE.click().raw().perform(stubEngine);

        assertNull(HookedAction.lastTrace(),
                "raw() must not emit a trace — no HookedAction is involved");
    }

    // ── Hook naming ───────────────────────────────────────────────────────────

    @Test
    public void hookNaming_resolveNamedConstants() {
        assertEquals("HIGHLIGHT_ELEMENT",
                core.actions.trace.ActionTraceLogger.nameOf(Before.HIGHLIGHT_ELEMENT));
        assertEquals("WAIT_FOR_ELEMENT_CLICKABLE",
                core.actions.trace.ActionTraceLogger.nameOf(Before.WAIT_FOR_ELEMENT_CLICKABLE));
        assertEquals("HIGHLIGHT_ELEMENT",
                core.actions.trace.ActionTraceLogger.nameOf(After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void hookNaming_lambdaFallsBackToLambdaLabel() {
        String name = core.actions.trace.ActionTraceLogger.nameOf((e, d) -> {});
        assertEquals(name, "lambda", "anonymous lambda must resolve to 'lambda'");
    }

    // ── Profile name threading ────────────────────────────────────────────────

    @Test
    public void safely_threadsSafeProfileNameIntoTrace() {
        CLICKABLE.click().safely().perform(stubEngine);

        ActionTrace trace = HookedAction.lastTrace();
        assertNotNull(trace);
        assertEquals(trace.profileName(), "SAFE");
    }

    @Test
    public void debug_threadsDebugProfileNameIntoTrace() {
        TYPEABLE.type("test").debug().perform(stubEngine);

        ActionTrace trace = HookedAction.lastTrace();
        assertNotNull(trace);
        assertEquals(trace.profileName(), "DEBUG");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UIEngine buildEngine() {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("resolve".equals(method.getName())) return STUB_DESCRIPTOR;
            return defaultFor(method.getReturnType());
        };
        return (UIEngine) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ UIEngine.class },
                handler);
    }

    private static Object defaultFor(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == long.class)    return 0L;
        if (type == double.class)  return 0d;
        if (type == float.class)   return 0f;
        return 0;
    }
}
