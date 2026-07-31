package core.runtime;

import core.engine.DomainRegistry;
import domain.automation.web.engine.UIEngineFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link VOIDBuilder}.
 *
 * <p>Tests cover the fluent configuration API, private field assignment, resolved-config
 * logic, and the single-use guard. No browser is opened -- {@link VOIDBuilder#start()}
 * is only invoked indirectly via the started-flag reflection path to test the guard.</p>
 */
public class VOIDBuilderTest {

    @BeforeClass
    public void setUp() {
        core.logging.CustomLogger.initialize(VOIDBuilderTest.class);
    }

    // -------------------------------------------------------------------------
    // Factory method
    // -------------------------------------------------------------------------

    @Test
    public void builder_returnsNewInstanceEachCall() {
        VOIDBuilder a = VOID.builder();
        VOIDBuilder b = VOID.builder();
        assertNotNull(a);
        assertNotNull(b);
        assertNotSame(a, b, "VOID.builder() must return a new instance each time");
    }

    // -------------------------------------------------------------------------
    // Fluent API -- return self
    // -------------------------------------------------------------------------

    @Test
    public void domain_returnsSelf_forFluentChaining() {
        VOIDBuilder builder = VOID.builder();
        assertSame(builder.domain("web"), builder,
                "domain() must return the same builder instance");
    }

    @Test
    public void engine_returnsSelf_forFluentChaining() {
        VOIDBuilder builder = VOID.builder();
        assertSame(builder.engine("selenium"), builder,
                "engine() must return the same builder instance");
    }

    @Test
    public void profile_returnsSelf_forFluentChaining() {
        VOIDBuilder builder = VOID.builder();
        assertSame(builder.profile(SessionProfile.DEFAULT), builder,
                "profile() must return the same builder instance");
    }

    // -------------------------------------------------------------------------
    // Field assignment
    // -------------------------------------------------------------------------

    @Test
    public void domain_setsDomainNameField() throws Exception {
        VOIDBuilder builder = VOID.builder();
        builder.domain("store");
        assertEquals(domainName(builder), "store");
    }

    @Test
    public void domain_overridesExistingDomainName() throws Exception {
        VOIDBuilder builder = VOID.builder();
        builder.domain("web");
        builder.domain("store");
        assertEquals(domainName(builder), "store",
                "second domain() call must overwrite the first");
    }

    @Test
    public void engine_setsEngineNameField() throws Exception {
        VOIDBuilder builder = VOID.builder();
        builder.engine("playwright");
        assertEquals(engineName(builder), "playwright");
    }

    @Test
    public void engine_overridesExistingEngineName() throws Exception {
        VOIDBuilder builder = VOID.builder();
        builder.engine("selenium");
        builder.engine("playwright");
        assertEquals(engineName(builder), "playwright",
                "second engine() call must overwrite the first");
    }

    @Test
    public void profile_setsProfileField() throws Exception {
        VOIDBuilder builder = VOID.builder();
        builder.profile(SessionProfile.DEFAULT);
        assertEquals(profile(builder), SessionProfile.DEFAULT);
    }

    // -------------------------------------------------------------------------
    // resolvedConfig() -- private method tested via reflection
    // -------------------------------------------------------------------------

    @Test
    public void resolvedConfig_injectsDomainNameWhenSet() throws Exception {
        VOIDBuilder builder = VOID.builder().domain("store");
        Properties config = resolvedConfig(builder);
        assertEquals(config.getProperty(DomainRegistry.PROP_DOMAIN), "store",
                "resolvedConfig() must inject domainName into the Properties copy");
    }

    @Test
    public void resolvedConfig_doesNotSetDomainNameWhenDomainNotCalled() throws Exception {
        VOIDBuilder builder = VOID.builder();
        Properties config = resolvedConfig(builder);
        assertNull(config.getProperty(DomainRegistry.PROP_DOMAIN),
                "resolvedConfig() must not inject domain name when domain() was not called");
    }

    @Test
    public void resolvedConfig_injectsEngineNameWhenSet() throws Exception {
        VOIDBuilder builder = VOID.builder().engine("playwright");
        Properties config = resolvedConfig(builder);
        assertEquals(config.getProperty(UIEngineFactory.PROP_ENGINE), "playwright",
                "resolvedConfig() must inject engineName into the Properties copy");
    }

    @Test
    public void resolvedConfig_doesNotSetEngineNameWhenEngineNotCalled() throws Exception {
        VOIDBuilder builder = VOID.builder();
        Properties config = resolvedConfig(builder);
        assertNull(config.getProperty(UIEngineFactory.PROP_ENGINE),
                "resolvedConfig() must not inject engine name when engine() was not called");
    }

    @Test
    public void resolvedConfig_returnsNewPropertiesInstanceEachCall() throws Exception {
        VOIDBuilder builder = VOID.builder().engine("selenium");
        Properties first  = resolvedConfig(builder);
        Properties second = resolvedConfig(builder);
        assertNotSame(first, second,
                "resolvedConfig() must return a defensive copy each time");
    }

    // -------------------------------------------------------------------------
    // Single-use guard
    // -------------------------------------------------------------------------

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = ".*single-use.*")
    public void start_throwsIllegalStateExceptionWhenAlreadyStarted() throws Exception {
        VOIDBuilder builder = VOID.builder();
        // Force the started flag without opening a browser
        Field started = VOIDBuilder.class.getDeclaredField("started");
        started.setAccessible(true);
        started.set(builder, true);
        builder.start(); // must throw immediately, before FrameworkBootstrap
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String domainName(VOIDBuilder builder) throws Exception {
        Field f = VOIDBuilder.class.getDeclaredField("domainName");
        f.setAccessible(true);
        return (String) f.get(builder);
    }

    private static String engineName(VOIDBuilder builder) throws Exception {
        Field f = VOIDBuilder.class.getDeclaredField("engineName");
        f.setAccessible(true);
        return (String) f.get(builder);
    }

    private static SessionProfile profile(VOIDBuilder builder) throws Exception {
        Field f = VOIDBuilder.class.getDeclaredField("profile");
        f.setAccessible(true);
        return (SessionProfile) f.get(builder);
    }

    private static Properties resolvedConfig(VOIDBuilder builder) throws Exception {
        Method m = VOIDBuilder.class.getDeclaredMethod("resolvedConfig");
        m.setAccessible(true);
        return (Properties) m.invoke(builder);
    }
}
