package core.engine;

import domain.automation.web.WebDomainRegistrar;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link DomainRegistry}.
 *
 * <p>Tests cover: name resolution priority, programmatic registration,
 * and the error path for unknown domains. No browser is opened.</p>
 */
public class DomainRegistryTest {

    @BeforeClass
    public void setUp() {
        core.logging.CustomLogger.initialize(DomainRegistryTest.class);
    }

    @AfterMethod
    public void clearSystemProperty() {
        System.clearProperty(DomainRegistry.PROP_DOMAIN);
    }

    // -------------------------------------------------------------------------
    // Name resolution priority
    // -------------------------------------------------------------------------

    @Test
    public void resolveDomainName_defaultsToWeb_whenNothingSpecified() {
        Properties config = new Properties();
        assertEquals(DomainRegistry.resolveDomainName(config), DomainRegistry.DEFAULT_DOMAIN);
    }

    @Test
    public void resolveDomainName_readsFromConfig() {
        Properties config = new Properties();
        config.setProperty(DomainRegistry.PROP_DOMAIN, "store");
        assertEquals(DomainRegistry.resolveDomainName(config), "store");
    }

    @Test
    public void resolveDomainName_systemPropertyOverridesConfig() {
        Properties config = new Properties();
        config.setProperty(DomainRegistry.PROP_DOMAIN, "store");
        System.setProperty(DomainRegistry.PROP_DOMAIN, "web");
        assertEquals(DomainRegistry.resolveDomainName(config), "web");
    }

    @Test
    public void resolveDomainName_lowercasesValue() {
        Properties config = new Properties();
        config.setProperty(DomainRegistry.PROP_DOMAIN, "WEB");
        assertEquals(DomainRegistry.resolveDomainName(config), "web");
    }

    @Test
    public void resolveDomainName_toleratesNullConfig() {
        assertEquals(DomainRegistry.resolveDomainName(null), DomainRegistry.DEFAULT_DOMAIN);
    }

    // -------------------------------------------------------------------------
    // Programmatic registration
    // -------------------------------------------------------------------------

    @Test
    public void register_programmaticRegistrar_isFoundByCreate() {
        StubDomainRegistrar stub = new StubDomainRegistrar("testdomain");
        DomainRegistry.register(stub);

        Properties config = new Properties();
        EngineBootstrap bootstrap = EngineBootstrap.withSettings(new Properties());
        Executor executor = DomainRegistry.create("testdomain", config, bootstrap);

        assertNotNull(executor, "DomainRegistry.create must return the stub's executor");
        assertSame(executor, stub.lastCreated, "must return the executor from the registered domain");
    }

    // -------------------------------------------------------------------------
    // Error path
    // -------------------------------------------------------------------------

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = ".*Unknown domain.*unregistered.*")
    public void create_throwsForUnknownDomain() {
        DomainRegistry.create("unregistered", new Properties(),
                EngineBootstrap.withSettings(new Properties()));
    }

    // -------------------------------------------------------------------------
    // Built-in web domain is discoverable
    // -------------------------------------------------------------------------

    @Test
    public void webDomainRegistrar_hasPredictableName() {
        assertEquals(new WebDomainRegistrar().name(), "web");
        assertEquals(WebDomainRegistrar.ID, "web");
    }

    // -------------------------------------------------------------------------
    // Stub
    // -------------------------------------------------------------------------

    private static final class StubDomainRegistrar implements DomainRegistrar {
        private final String name;
        Executor lastCreated;

        StubDomainRegistrar(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Executor createExecutor(Properties config, EngineBootstrap bootstrap) {
            lastCreated = new StubExecutor();
            return lastCreated;
        }
    }

    private static final class StubExecutor implements Executor {
        @Override
        public void initialize(EngineConfig config) {}

        @Override
        public void shutdown() {}

        @Override
        public String getEngineName() {
            return "stub";
        }
    }
}
