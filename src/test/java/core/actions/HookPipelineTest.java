package core.actions;

import core.actions.hooks.ActionHandler;
import core.engine.Executor;
import elements.locator.LocatorDescriptor;
import elements.locator.LocatorStrategy;
import core.engine.UIEngine;
import core.executor.FlowExecutor;
import core.flow.Flow;
import elements.api.UIElement;
import elements.api.actions.ElementActions;
import elements.meta.ElementRole;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

/**
 * Unit tests for the hook pipeline: {@link HookChainAction}, {@link ElementActions},
 * {@link Action#withHooks}, {@link ActionHandler}, and {@link FlowExecutor}.
 *
 * <p>Uses a minimal stub {@link UIEngine} via {@link Proxy} — no real browser needed.
 * Tests verify ordering, failure semantics, descriptor passing, and the fluent API.</p>
 */
public class HookPipelineTest {

    // ── Fixtures ─────────────────────────────────────────────────────────

    /** A stable descriptor returned by the stub engine. */
    private static final LocatorDescriptor STUB_DESCRIPTOR =
            new LocatorDescriptor("//button[@id='ok']", LocatorStrategy.XPATH);

    /** Ordered log of events — hooks and actions append tokens here. */
    private List<String> executionLog;

    /** Stub UIEngine that records resolve() calls and returns STUB_DESCRIPTOR. */
    private UIEngine stubEngine;

    /** Minimal UIElement stub for ElementActions tests. */
    private static final UIElement STUB_ELEMENT = new UIElement() {
        @Override public String getExternalFileName()              { return "stub.json"; }
        @Override public String getPrimaryLocator()                { return "STUB_KEY"; }
        @Override public Object[] getArgs()                        { return new Object[0]; }
        @Override public String getDisplayText()                   { return "StubElement"; }
        @Override public java.util.Map<ElementRole, String> getAllLocatorRoles() {
            return java.util.Map.of(ElementRole.TRIGGER, "STUB_KEY");
        }
    };

    @BeforeMethod
    public void setUp() {
        executionLog = new ArrayList<>();
        stubEngine = buildStubEngine();
    }

    /**
     * Builds a stub UIEngine via Proxy. Only resolve() is meaningful —
     * it returns STUB_DESCRIPTOR. All other methods are no-ops.
     */
    private UIEngine buildStubEngine() {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("resolve".equals(method.getName())) {
                return STUB_DESCRIPTOR;
            }
            return defaultFor(method.getReturnType());
        };
        return (UIEngine) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ UIEngine.class },
                handler);
    }

    private static Object defaultFor(Class<?> t) {
        if (!t.isPrimitive()) return null;
        if (t == boolean.class) return Boolean.FALSE;
        if (t == void.class)   return null;
        if (t == long.class)   return 0L;
        if (t == double.class) return 0d;
        if (t == float.class)  return 0f;
        return 0;
    }

    // ═════════════════════════════════════════════════════════════════════
    // HookChainAction -- core orchestration
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void hookedAction_executesInOrder_before_action_after() {
        ActionHandler before1 = (e, d) -> executionLog.add("before1");
        ActionHandler before2 = (e, d) -> executionLog.add("before2");
        ActionHandler after1  = (e, d) -> executionLog.add("after1");
        ActionHandler after2  = (e, d) -> executionLog.add("after2");
        Action delegate       = (e)    -> executionLog.add("action");

        HookChainAction hooked = HookChainAction.forTesting(
                delegate, STUB_DESCRIPTOR,
                List.of(before1, before2),
                List.of(after1, after2));

        hooked.perform((Executor) stubEngine);

        assertEquals(executionLog,
                List.of("before1", "before2", "action", "after1", "after2"),
                "Execution order must be: before hooks (list order) → action → after hooks (list order)");
    }

    @Test
    public void hookedAction_passesDescriptorToHooks() {
        AtomicReference<LocatorDescriptor> beforeReceived = new AtomicReference<>();
        AtomicReference<LocatorDescriptor> afterReceived  = new AtomicReference<>();

        HookChainAction hooked = HookChainAction.forTesting(
                (e) -> {}, STUB_DESCRIPTOR,
                List.of((e, d) -> beforeReceived.set(d)),
                List.of((e, d) -> afterReceived.set(d)));

        hooked.perform((Executor) stubEngine);

        assertSame(beforeReceived.get(), STUB_DESCRIPTOR,
                "Before hook must receive the descriptor");
        assertSame(afterReceived.get(), STUB_DESCRIPTOR,
                "After hook must receive the descriptor");
    }

    @Test
    public void hookedAction_passesEngineToHooks() {
        AtomicReference<UIEngine> capturedEngine = new AtomicReference<>();

        HookChainAction hooked = HookChainAction.forTesting(
                (e) -> {}, STUB_DESCRIPTOR,
                List.of((executor, d) -> capturedEngine.set((UIEngine) executor)),
                null);

        hooked.perform((Executor) stubEngine);

        assertSame(capturedEngine.get(), stubEngine,
                "Hook must receive the same engine instance");
    }

    @Test
    public void hookedAction_beforeHookThrows_actionNotExecuted() {
        AtomicBoolean actionRan = new AtomicBoolean(false);
        AtomicBoolean afterRan  = new AtomicBoolean(false);

        HookChainAction hooked = HookChainAction.forTesting(
                (e) -> actionRan.set(true), STUB_DESCRIPTOR,
                List.of((e, d) -> { throw new RuntimeException("before failed"); }),
                List.of((e, d) -> afterRan.set(true)));

        assertThrows(RuntimeException.class, () -> hooked.perform((Executor) stubEngine));
        assertFalse(actionRan.get(), "Action must NOT execute when a before hook throws");
        assertFalse(afterRan.get(), "After hooks must NOT execute when a before hook throws");
    }

    @Test
    public void hookedAction_afterHookThrows_propagates() {
        AtomicBoolean actionRan = new AtomicBoolean(false);

        HookChainAction hooked = HookChainAction.forTesting(
                (e) -> actionRan.set(true), STUB_DESCRIPTOR,
                null,
                List.of((e, d) -> { throw new RuntimeException("after failed"); }));

        assertThrows(RuntimeException.class, () -> hooked.perform((Executor) stubEngine));
        assertTrue(actionRan.get(), "Action must execute before the after hook throws");
    }

    @Test
    public void hookedAction_nullHookLists_treatedAsEmpty() {
        AtomicBoolean actionRan = new AtomicBoolean(false);

        HookChainAction hooked = HookChainAction.forTesting(
                (e) -> actionRan.set(true), STUB_DESCRIPTOR,
                null, null);

        hooked.perform((Executor) stubEngine);  // should not throw

        assertTrue(actionRan.get(), "Action must execute when hook lists are null");
    }

    @Test
    public void hookedAction_nullHookInList_skipped() {
        AtomicBoolean actionRan = new AtomicBoolean(false);
        List<ActionHandler> withNull = new ArrayList<>();
        withNull.add(null);
        withNull.add((e, d) -> executionLog.add("valid"));

        HookChainAction hooked = HookChainAction.forTesting(
                (e) -> actionRan.set(true), STUB_DESCRIPTOR,
                withNull, null);

        hooked.perform((Executor) stubEngine);  // null hook should be skipped gracefully

        assertTrue(actionRan.get());
        assertEquals(executionLog, List.of("valid"));
    }

    @Test
    public void hookedAction_nullDescriptor_passedToHooks() {
        AtomicReference<LocatorDescriptor> received = new AtomicReference<>(STUB_DESCRIPTOR);

        HookChainAction hooked = HookChainAction.forTesting(
                (e) -> {}, null,  // null descriptor (legacy path)
                List.of((e, d) -> received.set(d)),
                null);

        hooked.perform((Executor) stubEngine);

        assertNull(received.get(), "Legacy path: null descriptor must be passed through to hooks");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void hookedAction_nullDelegate_throwsNPE() {
        HookChainAction.forTesting(null, STUB_DESCRIPTOR, null, null);
    }

    // ═════════════════════════════════════════════════════════════════════
    // ElementActions — resolvable action factory
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void elementActions_perform_resolvesAndExecutes() {
        AtomicReference<LocatorDescriptor> receivedDescriptor = new AtomicReference<>();

        Action action = ElementActions.of(STUB_ELEMENT, ElementRole.TRIGGER,
                (engine, d) -> receivedDescriptor.set(d));

        action.perform((Executor) stubEngine);

        assertSame(receivedDescriptor.get(), STUB_DESCRIPTOR,
                "ElementActions.of() must resolve and pass descriptor to the op");
    }

    @Test
    public void elementActions_resolve_returnsDescriptor() {
        Action action = ElementActions.of(STUB_ELEMENT, ElementRole.TRIGGER,
                (engine, d) -> {});

        LocatorDescriptor resolved = action.resolve((Executor) stubEngine);

        assertSame(resolved, STUB_DESCRIPTOR,
                "resolve() must return the descriptor from engine.resolve()");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Action.resolve() — default throws
    // ═════════════════════════════════════════════════════════════════════

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void action_resolve_default_throwsForRawLambda() {
        Action rawAction = (engine) -> {};
        rawAction.resolve((Executor) stubEngine);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Action.withHooks() — fluent API
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void withHooks_executesFullPipeline() {
        Action action = ElementActions.of(STUB_ELEMENT, ElementRole.TRIGGER,
                (engine, d) -> executionLog.add("action"));

        Action hooked = action.withHooks(
                List.of((e, d) -> executionLog.add("before")),
                List.of((e, d) -> executionLog.add("after")));

        hooked.perform((Executor) stubEngine);

        assertEquals(executionLog, List.of("before", "action", "after"));
    }

    @Test
    public void withHooks_descriptorSharedAcrossHooksAndAction() {
        AtomicReference<LocatorDescriptor> beforeDesc = new AtomicReference<>();
        AtomicReference<LocatorDescriptor> afterDesc  = new AtomicReference<>();
        AtomicReference<LocatorDescriptor> actionDesc = new AtomicReference<>();

        Action action = ElementActions.of(STUB_ELEMENT, ElementRole.TRIGGER,
                (engine, d) -> actionDesc.set(d));

        action.withHooks(
                List.of((e, d) -> beforeDesc.set(d)),
                List.of((e, d) -> afterDesc.set(d))
        ).perform((Executor) stubEngine);

        assertSame(beforeDesc.get(), STUB_DESCRIPTOR);
        assertSame(actionDesc.get(), STUB_DESCRIPTOR);
        assertSame(afterDesc.get(), STUB_DESCRIPTOR);
    }

    @Test
    public void withHooks_nullLists_treatedAsNoHooks() {
        AtomicBoolean ran = new AtomicBoolean(false);

        Action action = ElementActions.of(STUB_ELEMENT, ElementRole.TRIGGER,
                (engine, d) -> ran.set(true));

        action.withHooks(null, null).perform((Executor) stubEngine);

        assertTrue(ran.get(), "Action must execute even with null hook lists");
    }

    @Test
    public void withHooks_beforeThrows_actionNotExecuted() {
        AtomicBoolean actionRan = new AtomicBoolean(false);

        Action action = ElementActions.of(STUB_ELEMENT, ElementRole.TRIGGER,
                (engine, d) -> actionRan.set(true));

        Action hooked = action.withHooks(
                List.of((e, d) -> { throw new RuntimeException("boom"); }),
                null);

        assertThrows(RuntimeException.class, () -> hooked.perform((Executor) stubEngine));
        assertFalse(actionRan.get());
    }

    @Test(expectedExceptions = UnsupportedOperationException.class,
          expectedExceptionsMessageRegExp = ".*does not support descriptor resolution.*")
    public void withHooks_onRawLambda_throwsUnsupported() {
        Action rawAction = (engine) -> {};
        Action hooked = rawAction.withHooks(
                List.of((e, d) -> {}),
                null);
        hooked.perform((Executor) stubEngine);  // resolve() called inside → throws
    }

    // ═════════════════════════════════════════════════════════════════════
    // ActionHandler — functional interface + legacy adapter
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void actionHandler_lambda_receivesBothArgs() {
        AtomicReference<UIEngine> capturedEngine = new AtomicReference<>();
        AtomicReference<LocatorDescriptor> capturedDesc = new AtomicReference<>();

        ActionHandler handler = (executor, descriptor) -> {
            capturedEngine.set((UIEngine) executor);
            capturedDesc.set(descriptor);
        };

        handler.execute((Executor) stubEngine, STUB_DESCRIPTOR);

        assertSame(capturedEngine.get(), stubEngine);
        assertSame(capturedDesc.get(), STUB_DESCRIPTOR);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void actionHandler_legacy_ignoresDescriptor() {
        AtomicReference<UIEngine> capturedEngine = new AtomicReference<>();

        ActionHandler handler = ActionHandler.legacy(engine -> capturedEngine.set(engine));

        handler.execute((Executor) stubEngine, STUB_DESCRIPTOR);

        assertSame(capturedEngine.get(), stubEngine,
                "Legacy adapter must pass through the engine");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void actionHandler_legacy_worksWithNullDescriptor() {
        AtomicBoolean ran = new AtomicBoolean(false);

        ActionHandler handler = ActionHandler.legacy(engine -> ran.set(true));

        handler.execute((Executor) stubEngine, null);  // legacy path: null descriptor

        assertTrue(ran.get());
    }

    // ═════════════════════════════════════════════════════════════════════
    // FlowExecutor — run(Action) and run(Flow)
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void flowExecutor_runAction_executesAgainstEngine() {
        AtomicReference<UIEngine> capturedEngine = new AtomicReference<>();
        FlowExecutor executor = new FlowExecutor(stubEngine);

        executor.run(e -> capturedEngine.set((UIEngine) e));

        assertSame(capturedEngine.get(), stubEngine);
    }

    @Test
    public void flowExecutor_runFlow_executesAllActionsInOrder() {
        FlowExecutor executor = new FlowExecutor(stubEngine);

        executor.run(Flow.of(
                (engine) -> executionLog.add("a1"),
                (engine) -> executionLog.add("a2"),
                (engine) -> executionLog.add("a3")
        ));

        assertEquals(executionLog, List.of("a1", "a2", "a3"));
    }

    @Test
    public void flowExecutor_runFlow_withHookedActions() {
        FlowExecutor executor = new FlowExecutor(stubEngine);

        Action hookedClick = ElementActions
                .of(STUB_ELEMENT, ElementRole.TRIGGER,
                        (engine, d) -> executionLog.add("click"))
                .withHooks(
                        List.of((e, d) -> executionLog.add("before-click")),
                        List.of((e, d) -> executionLog.add("after-click")));

        Action hookedType = ElementActions
                .of(STUB_ELEMENT, ElementRole.INPUT,
                        (engine, d) -> executionLog.add("type"))
                .withHooks(
                        List.of((e, d) -> executionLog.add("before-type")),
                        List.of((e, d) -> executionLog.add("after-type")));

        executor.run(Flow.of(hookedClick, hookedType));

        assertEquals(executionLog, List.of(
                "before-click", "click", "after-click",
                "before-type", "type", "after-type"));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Integration: full pipeline end-to-end
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void fullPipeline_elementAction_withHooks_viaFlowExecutor() {
        AtomicInteger hookCallCount = new AtomicInteger();
        AtomicReference<LocatorDescriptor> actionDescriptor = new AtomicReference<>();

        // Simulate: element.click().withHooks(before, after) → executor.run(flow)
        Action click = ElementActions.of(STUB_ELEMENT, ElementRole.TRIGGER,
                (engine, d) -> {
                    executionLog.add("click");
                    actionDescriptor.set(d);
                });

        Action hookedClick = click.withHooks(
                List.of(
                        (e, d) -> { executionLog.add("wait-visible"); hookCallCount.incrementAndGet(); },
                        (e, d) -> { executionLog.add("highlight"); hookCallCount.incrementAndGet(); }
                ),
                List.of(
                        (e, d) -> { executionLog.add("log-success"); hookCallCount.incrementAndGet(); }
                ));

        FlowExecutor executor = new FlowExecutor(stubEngine);
        executor.run(Flow.of(hookedClick));

        // Verify ordering
        assertEquals(executionLog,
                List.of("wait-visible", "highlight", "click", "log-success"));

        // Verify descriptor was resolved and shared
        assertSame(actionDescriptor.get(), STUB_DESCRIPTOR);

        // Verify all hooks ran
        assertEquals(hookCallCount.get(), 3);
    }

    @Test
    public void fullPipeline_multipleActionsWithDifferentHooks() {
        FlowExecutor executor = new FlowExecutor(stubEngine);

        // Action 1: type with clear-before
        Action type = ElementActions
                .of(STUB_ELEMENT, ElementRole.INPUT,
                        (engine, d) -> executionLog.add("type-text"))
                .withHooks(
                        List.of((e, d) -> executionLog.add("clear")),
                        null);

        // Action 2: click with no before, highlight-after
        Action click = ElementActions
                .of(STUB_ELEMENT, ElementRole.TRIGGER,
                        (engine, d) -> executionLog.add("click-submit"))
                .withHooks(
                        null,
                        List.of((e, d) -> executionLog.add("highlight")));

        // Action 3: plain action, no hooks
        Action plain = (engine) -> executionLog.add("navigate");

        executor.run(Flow.of(type, click, plain));

        assertEquals(executionLog, List.of(
                "clear", "type-text",
                "click-submit", "highlight",
                "navigate"));
    }
}

