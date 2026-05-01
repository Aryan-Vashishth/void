package core.runtime;

import core.bootstrap.FrameworkBootstrap;
import core.context.ExecutionContext;
import core.driver.DriverFactory;
import core.driver.DriverManager;
import core.logging.CustomLogger;
import core.interactions.Interactions;
import org.openqa.selenium.WebDriver;

/**
 * Façade / entry point for the core VOID framework.
 *
 * <p>This class is intentionally <b>framework-only</b> — it carries no
 * BDD / Cucumber dependencies. For DSL context-driven helpers such as
 * {@code dsl()}, use a subclass that extends this class.</p>
 *
 * <h3>Framework layer usage</h3>
 * <pre>
 *   VOID app = VOID.start();
 *   app.interaction().clickOn(MyElements.SUBMIT_BUTTON);
 *   app.shutdown();
 * </pre>
 *
 * <h3>Architecture</h3>
 * <pre>
 *   VOID.start()
 *     → FrameworkBootstrap.init()     (one-time: validate configs, seed utils)
 *     → DriverManager.createDriver() (create + register WebDriver)
 *     → ExecutionContext              (holds config + driver for this session)
 *     → return VOID façade            (thin wrapper, delegates to context)
 * </pre>
 *
 * <h3>Layer model</h3>
 * <pre>
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │  dsl layer  (dsl.*)                                          │
 *  │    VoidDSL            →  context-driven DSL                  │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │  framework layer   (this class + core.interactions / core)   │
 *  │    VOID              →  Interactions  (raw UI actions)        │
 *  └──────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class VOID {

    /** Per-session execution context (config + driver). */
    private final ExecutionContext context;

    /** Lazily-initialised, cached interaction helper. */
    private Interactions interactions;

    // ===========================
    //        Construction
    // ===========================

    /**
     * Protected constructor — use {@link #start()} or {@link #start(DriverFactory.Profile)}.
     *
     * @param context the execution context for this session
     */
    protected VOID(ExecutionContext context) {
        this.context = context;
    }

    // ===========================
    //      Static Factories
    // ===========================

    /**
     * Starts a new VOID session with the {@link DriverFactory.Profile#DEFAULT DEFAULT} profile.
     *
     * <p>Orchestrates the full startup pipeline:</p>
     * <ol>
     *   <li>{@link FrameworkBootstrap#init()} — one-time config validation</li>
     *   <li>{@link DriverManager#createDriver(DriverFactory.Profile)} — WebDriver creation + registration</li>
     *   <li>Builds an {@link ExecutionContext} binding config and driver</li>
     * </ol>
     *
     * @return a ready-to-use VOID instance
     */
    public static VOID start() {
        return start(DriverFactory.Profile.DEFAULT);
    }

    /**
     * Starts a new VOID session with the specified driver profile.
     *
     * @param profile the driver configuration profile
     * @return a ready-to-use VOID instance
     */
    public static VOID start(DriverFactory.Profile profile) {
        FrameworkBootstrap.init();

        WebDriver driver = DriverManager.createDriver(profile);
        ExecutionContext ctx = new ExecutionContext(
                FrameworkBootstrap.getUtilsConfig(),
                driver
        );

        CustomLogger.info.log("VOID initialised — driver ready.");
        return new VOID(ctx);
    }

    // ===========================
    //        Lifecycle
    // ===========================

    /**
     * Shuts down this VOID session — quits all drivers for the current thread.
     */
    public void shutdown() {
        CustomLogger.info.log("VOID shutting down.");
        DriverManager.quitAll();
    }

    // ===========================
    //   Accessible to subclasses
    // ===========================

    /**
     * Returns the {@link ExecutionContext} so subclasses
     * can access configuration and the driver without re-fetching from globals.
     */
    protected ExecutionContext getContext() {
        return context;
    }

    /**
     * Returns the underlying {@link WebDriver} so subclasses
     * can pass it to their own interaction helpers without re-fetching it from the context.
     */
    protected WebDriver getDriver() {
        return context.getDriver();
    }

    // ===========================
    //         Interactions
    // ===========================

    /** Returns the (cached) general-purpose interaction helper. */
    public Interactions interaction() {
        if (interactions == null) {
            interactions = new Interactions(context.getDriver());
        }
        return interactions;
    }
}

