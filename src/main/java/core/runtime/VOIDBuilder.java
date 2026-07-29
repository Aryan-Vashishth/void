package core.runtime;

import core.bootstrap.FrameworkBootstrap;
import core.context.SessionContext;
import core.driver.DriverFactory;
import core.engine.EngineBootstrap;
import core.engine.Executor;
import core.engine.UIEngine;
import core.engine.UIEngineFactory;
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
 *   // Engine resolved from config / ENV / System property
 *   VOID session = VOID.builder()
 *           .profile(DriverFactory.Profile.DEFAULT)
 *           .start();
 *
 *   // Explicit engine override
 *   VOID session = VOID.builder()
 *           .engine(SeleniumEngine.ID)
 *           .profile(DriverFactory.Profile.CHROME)
 *           .start();
 *
 *   // Two independent sessions
 *   VOID admin    = VOID.builder().profile(DriverFactory.Profile.DEFAULT).start();
 *   VOID customer = VOID.builder().profile(DriverFactory.Profile.DEFAULT).start();
 * </pre>
 */
public final class VOIDBuilder {

    private String engineName;
    private DriverFactory.Profile profile;
    private boolean started = false;

    /** Package-private -- callers use {@link VOID#builder()}. */
    VOIDBuilder() {}

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
     * Sets the driver configuration profile for this session.
     *
     * @param profile driver profile
     * @return this builder
     */
    public VOIDBuilder profile(DriverFactory.Profile profile) {
        this.profile = profile;
        return this;
    }

    /**
     * Initializes the engine and returns a ready-to-use {@link VOID} session.
     *
     * <p>This is the terminal operation. The call sequence is:</p>
     * <ol>
     *   <li>{@link FrameworkBootstrap#init()} -- one-time config validation</li>
     *   <li>{@link UIEngineFactory#create} -- engine selected first, driver deferred</li>
     *   <li>Build a {@link SessionContext} binding config and engine</li>
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
        Properties bootstrapSettings = new Properties();
        bootstrapSettings.setProperty("profile",
                (profile != null ? profile : DriverFactory.Profile.DEFAULT).name());
        Executor executor = UIEngineFactory.create(config,
                EngineBootstrap.withSettings(bootstrapSettings));

        SessionContext ctx = new SessionContext(config, executor);

        // Bridge cast: all current registrars produce UIEngine subtypes; closes in 5.3 when
        // navigation routes through the pipeline and VOID no longer holds a UIEngine reference.
        UIEngine engine = (UIEngine) executor;

        CustomLogger.info.log("VOID session started -- sessionId=" + ctx.sessionId()
                + ", engine=" + engine.getEngineName() + ", profile=" + profile);
        return new VOID(ctx, engine);
    }

    private Properties resolvedConfig() {
        Properties config = new Properties(FrameworkBootstrap.getUtilsConfig());
        if (engineName != null) {
            config.setProperty(UIEngineFactory.PROP_ENGINE, engineName);
        }
        return config;
    }
}
