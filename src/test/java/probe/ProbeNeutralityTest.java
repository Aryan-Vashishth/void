package probe;

import core.actions.trace.ActionTrace;
import core.actions.trace.TraceStatus;
import core.context.SessionContext;
import core.engine.DomainRegistry;
import core.runtime.VOID;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import probe.store.StoreCapabilities;
import probe.store.StoreExecutor;
import probe.store.StoreDomainRegistrar;
import probe.store.actions.ClearStoreAction;
import probe.store.actions.ReadStoreAction;
import probe.store.actions.WriteStoreAction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.*;

/**
 * M4 domain-neutrality regression gate.
 *
 * <p>Proves that adding a new domain (store) requires zero edits to any
 * runtime-owned file in {@code src/main/java}. The store domain is test-scope
 * only: no services file in main resources, no main source changes.</p>
 */
public class ProbeNeutralityTest {

    private static final Method LAST_TRACE;
    private static final Method CLEAR_LAST_TRACE;

    static {
        try {
            Class<?> hca = Class.forName("core.actions.HookChainAction");
            LAST_TRACE = hca.getDeclaredMethod("lastTrace");
            LAST_TRACE.setAccessible(true);
            CLEAR_LAST_TRACE = hca.getDeclaredMethod("clearLastTrace");
            CLEAR_LAST_TRACE.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private VOID session;

    @BeforeMethod
    public void setUp() {
        DomainRegistry.register(new StoreDomainRegistrar());
    }

    @AfterMethod
    public void tearDown() {
        if (session != null) {
            session.shutdown();
            session = null;
        }
    }

    @Test
    public void storeDomain_registersAndStartsSession_withoutOpeningBrowser() {
        session = VOID.builder().domain(StoreDomainRegistrar.ID).start();
        assertNotNull(session, "session must not be null");
    }

    @Test
    public void storeDomain_sessionExecutor_isStoreExecutorInstance() throws Exception {
        session = VOID.builder().domain(StoreDomainRegistrar.ID).start();

        Field contextField = VOID.class.getDeclaredField("context");
        contextField.setAccessible(true);
        SessionContext ctx = (SessionContext) contextField.get(session);

        assertSame(ctx.engine().getClass(), StoreExecutor.class,
                "executor must be StoreExecutor, was: " + ctx.engine().getClass());
    }

    @Test
    public void writeReadAction_capability_isStoreCapability() {
        assertEquals(new WriteStoreAction("k", "v").capability(), StoreCapabilities.WRITE);
        assertEquals(new ReadStoreAction("k").capability(), StoreCapabilities.READ);
    }

    @Test
    public void storeDomain_dispatchWrite_persists_andRead_retrieves() {
        session = VOID.builder().domain(StoreDomainRegistrar.ID).start();

        session.run(new WriteStoreAction("greeting", "hello"));

        ReadStoreAction read = new ReadStoreAction("greeting");
        session.run(read);

        assertEquals(read.getResult(), "hello");
    }

    @Test
    public void storeDomain_dispatchClear_emptiesStore() {
        session = VOID.builder().domain(StoreDomainRegistrar.ID).start();

        session.run(new WriteStoreAction("x", "1"));
        session.run(new ClearStoreAction());

        ReadStoreAction read = new ReadStoreAction("x");
        session.run(read);

        assertNull(read.getResult(), "store must be empty after clear");
    }

    @Test
    public void storeDomain_hooksFireAroundAction() {
        session = VOID.builder().domain(StoreDomainRegistrar.ID).start();

        AtomicBoolean beforeFired = new AtomicBoolean(false);
        AtomicBoolean afterFired  = new AtomicBoolean(false);

        var action = new WriteStoreAction("flagKey", "flagValue")
                .before((exec, desc) -> beforeFired.set(true))
                .after((exec, desc) -> afterFired.set(true));

        session.run(action);

        assertTrue(beforeFired.get(), "before hook must fire");
        assertTrue(afterFired.get(),  "after hook must fire");
    }

    @Test
    public void storeDomain_hookChainAction_emitsTrace_withPassedStatus() throws Exception {
        session = VOID.builder().domain(StoreDomainRegistrar.ID).start();

        CLEAR_LAST_TRACE.invoke(null);

        var action = new WriteStoreAction("traceKey", "traceValue")
                .before((exec, desc) -> {});

        session.run(action);

        ActionTrace trace = (ActionTrace) LAST_TRACE.invoke(null);
        assertNotNull(trace, "trace must be captured after hook-wrapped action");
        assertEquals(trace.status(), TraceStatus.SUCCESS);
        assertEquals(trace.elementName(), "store[traceKey]");
        assertEquals(trace.operation(), "write");
    }
}
