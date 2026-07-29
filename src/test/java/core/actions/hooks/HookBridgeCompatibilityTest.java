package core.actions.hooks;

import core.actions.Action;
import core.engine.Executor;
import core.engine.LocatorDescriptor;
import core.engine.LocatorStrategy;
import core.engine.UIEngine;
import elements.api.UIElement;
import elements.api.actions.ElementActions;
import elements.meta.ElementRole;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Verifies the runtime-redesign I2.1 bridge: implementations of the deprecated
 * {@code core.interactions.hooks} hook interfaces remain assignable to the kernel-owned
 * {@code core.actions.hooks} contract, so existing implementors (not just lambdas) keep
 * compiling and working against new-typed call sites until the bridges are deleted in I9.3.
 */
public class HookBridgeCompatibilityTest {

    private static final LocatorDescriptor STUB_DESCRIPTOR =
            new LocatorDescriptor("//button[@id='ok']", LocatorStrategy.XPATH);

    private static final UIElement STUB_ELEMENT = new UIElement() {
        @Override public String getExternalFileName() { return "stub.json"; }
        @Override public String getPrimaryLocator()   { return "STUB_KEY"; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    };

    /** A named implementor of the deprecated interface, not a lambda -- proves the bridge
     *  works for concrete classes, not just functional-interface call sites. */
    private static final class LegacyBeforeHook implements core.interactions.hooks.BeforeActionHandler {
        private boolean called;

        @Override
        public void execute(Executor executor, LocatorDescriptor descriptor) {
            called = true;
        }
    }

    private static final class LegacyAfterHook implements core.interactions.hooks.AfterActionHandler {
        private boolean called;

        @Override
        public void execute(Executor executor, LocatorDescriptor descriptor) {
            called = true;
        }
    }

    @Test(description = "old BeforeActionHandler implementor is assignable to the new kernel type")
    public void oldBeforeActionHandlerIsAssignableToNewType() {
        core.interactions.hooks.BeforeActionHandler oldTyped = new LegacyBeforeHook();
        BeforeActionHandler newTyped = oldTyped; // compiles only because old extends new
        assertTrue(newTyped instanceof ActionHandler);
    }

    @Test(description = "old AfterActionHandler implementor is assignable to the new kernel type")
    public void oldAfterActionHandlerIsAssignableToNewType() {
        core.interactions.hooks.AfterActionHandler oldTyped = new LegacyAfterHook();
        AfterActionHandler newTyped = oldTyped;
        assertTrue(newTyped instanceof ActionHandler);
    }

    @Test(description = "old-typed hook implementor passes to a new-typed call site (Action.before)")
    public void oldHookImplementorWorksWithActionBefore() {
        LegacyBeforeHook hook = new LegacyBeforeHook();
        Action action = ElementActions.of(STUB_ELEMENT, ElementRole.TRIGGER, (engine, descriptor) -> {});
        Action decorated = action.before(hook);

        decorated.perform((Executor) stubEngine());

        assertTrue(hook.called, "legacy-typed before hook should still execute via the new-typed pipeline");
    }

    @Test(description = "old-typed hook implementor passes to a new-typed call site (Action.after)")
    public void oldHookImplementorWorksWithActionAfter() {
        LegacyAfterHook hook = new LegacyAfterHook();
        Action action = ElementActions.of(STUB_ELEMENT, ElementRole.TRIGGER, (engine, descriptor) -> {});
        Action decorated = action.after(hook);

        decorated.perform((Executor) stubEngine());

        assertTrue(hook.called, "legacy-typed after hook should still execute via the new-typed pipeline");
    }

    @Test(description = "old-package ActionHandler.legacy() adapter still bridges to the new contract")
    @SuppressWarnings("deprecation")
    public void legacyAdapterStillBridges() {
        java.util.concurrent.atomic.AtomicBoolean ran = new java.util.concurrent.atomic.AtomicBoolean(false);
        ActionHandler wrapped = ActionHandler.legacy(engine -> ran.set(true));
        wrapped.execute(stubEngine(), null);
        assertEquals(ran.get(), true);
    }

    /** Minimal Proxy-based UIEngine stub -- only resolve() is meaningful. */
    private static UIEngine stubEngine() {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("resolve".equals(method.getName())) return STUB_DESCRIPTOR;
            Class<?> rt = method.getReturnType();
            if (!rt.isPrimitive()) return null;
            if (rt == boolean.class) return Boolean.FALSE;
            if (rt == void.class) return null;
            return 0;
        };
        return (UIEngine) Proxy.newProxyInstance(
                HookBridgeCompatibilityTest.class.getClassLoader(),
                new Class<?>[]{ UIEngine.class },
                handler);
    }
}
