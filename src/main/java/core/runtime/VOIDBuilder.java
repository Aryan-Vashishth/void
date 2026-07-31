package core.runtime;

import core.bootstrap.FrameworkBootstrap;
import core.context.SessionContext;
import core.engine.DomainRegistry;
import core.engine.EngineBootstrap;
import core.engine.Executor;
import domain.automation.web.engine.UIEngineFactory;
import core.logging.CustomLogger;

import java.util.Properties;

/**
 * Fluent builder for a {@link VOID} session.
 *
 * <p>Obtain an instance via {@link VOID#builder()}. Each builder is single-use:
 * calling {@link #start()} twice on the same instance throws
 * {@link IllegalStateException}. Call {@code VOID.builder()} for each new session.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   // Domain and engine resolved from config / ENV / System property
 *   VOID session = VOID.builder()
 *           .profile(SessionProfile.DEFAULT)
 *           .start();
 *
 *   // Explicit domain and engine override
 *   VOID session = VOID.builder()
 *           .domain("web")
 *           .engine(SeleniumEngine.ID)
 *           .profile(SessionProfile.CI)
 *           .start();
 *
 *   // Two independent sessions
 *   VOID admin    = VOID.builder().profile(SessionProfile.DEFAULT).start();
 *   VOID customer = VOID.builder().profile(SessionProfile.DEFAULT).start();
 * </pre>
 */
public final class VOIDBuilder {

    private String domainName;
    private String engineName;
    private SessionProfile profile;
    private boolean started = false;

    /** Package-private -- callers use {@link VOID#builder()}. */
    VOIDBuilder() {}

    /**
     * Selects the domain for this session.
     *
     * <p>If not called, the domain is resolved from System property {@code domain},
     * ENV {@code DOMAIN}, config, or the registry default ({@code "web"}).</p>
     *
     * @param domainId domain identifier string (e.g. {@code "web"})
     * @return this builder
     */
    public VOIDBuilder domain(String domainId) {
        this.domainName = domainId;
        return this;
    }

    /**
     * Overrides engine selection for this session.
     *
     * <p>If not called, the engine is resolved from System property, ENV, config, or the
     * factory default ("selenium"). Prefer the typed constant (e.g.,
     * {@code SeleniumEngine.ID}) over a raw string literal.</p>
     *
     * @param engineId engine identifier string
     * @return this builder
     */
    public VOIDBuilder engine(String engineId) {
        this.engineName = engineId;
        return this;
    }

    /**
     * Sets the session configuration profile.
     *
     * <p>The profile name is passed to the domain executor's adapter, which maps it
     * to its own configuration source (e.g. the Selenium adapter maps {@code "DEFAULT"}
     * to {@code driver.properties}, {@code "CI"} to {@code driver-ci.properties}).</p>
     *
     * @param sessionProfile session profile
     * @return this builder
     */
    public VOIDBuilder profile(SessionProfile sessionProfile) {
        this.profile = sessionProfile;
        return this;
    }

    /**
     * Initializes the domain executor and returns a ready-to-use {@link VOID} session.
     *
     * <p>This is the terminal operation. The call sequence is:</p>
     * <ol>
     *   <li>{@link FrameworkBootstrap#init()} -- one-time config load</li>
     *   <li>{@link DomainRegistry#create} -- domain resolved, executor created and initialized</li>
     *   <li>Build a {@link SessionContext} binding config and executor</li>
     * </ol>
     *
     * @return a ready-to-use VOID session
     * @throws IllegalStateException if called more than once on the same builder
     */
    public VOID start() {
        if (started) throw new IllegalStateException(
                "VOIDBuilder is single-use. Call VOID.builder() for each new session.");
        started = true;

        FrameworkBootstrap.init();

        Properties config = resolvedConfig();
        String resolvedDomain = DomainRegistry.resolveDomainName(config);

        Properties bootstrapSettings = new Properties();
        bootstrapSettings.setProperty("profile",
                (profile != null ? profile : SessionProfile.DEFAULT).name());
        Executor executor = DomainRegistry.create(resolvedDomain, config,
                EngineBootstrap.withSettings(bootstrapSettings));

        SessionContext ctx = new SessionContext(config, executor);

        CustomLogger.info.log("VOID session started -- sessionId=" + ctx.sessionId()
                + ", domain=" + resolvedDomain
                + ", engine=" + ctx.getEngineName() + ", profile=" + profile);
        return new VOID(ctx);
    }

    private Properties resolvedConfig() {
        Properties config = new Properties(FrameworkBootstrap.getUtilsConfig());
        if (domainName != null) {
            config.setProperty(DomainRegistry.PROP_DOMAIN, domainName);
        }
        if (engineName != null) {
            config.setProperty(UIEngineFactory.PROP_ENGINE, engineName);
        }
        return config;
    }
}
