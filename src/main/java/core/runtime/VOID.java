package core.runtime;

import core.actions.Action;
import core.bootstrap.FrameworkBootstrap;
import core.context.ExecutionContext;
import core.driver.DriverContext;
import core.driver.DriverFactory;
import core.driver.DriverManager;
import core.engine.EngineConfig;
import core.engine.UIEngine;
import core.engine.UIEngineFactory;
import core.executor.FlowExecutor;
import core.flow.Flow;
import core.logging.CustomLogger;
import core.interactions.Interactions;
import org.openqa.selenium.WebDriver;

/**
 * Primary session object for the VOID framework.
 *
 * <p>A {@code VOID} instance represents a single browser session. Tests should
 * think in terms of a session, not an engine. The majority of test code should
 * interact only with {@code VOID}, {@link Flow}, {@link Action}, and
 * {@code Element} types.</p>
 *
 * <h3>Typical usage</h3>
 * <pre>
 *   VOID app = VOID.start();
 *
 *   app.navigateTo("https://example.com/login");
 *
 *   app.run(Flow.of(
 *       LoginPage.USERNAME.type("admin"),
 *       LoginPage.PASSWORD.type("secret"),
 *       LoginPage.SUBMIT.click()
 *   ));
 *
 *   assertTrue(app.getCurrentUrl().contains("/dashboard"));
 *
 *   app.shutdown();
 * </pre>
 *
 * <h3>Multi-session usage</h3>
 * <pre>
 *   VOID admin    = VOID.start();
 *   VOID customer = VOID.start();
 *
 *   admin.navigateTo(adminUrl);
 *   admin.run(loginFlow);
 *
 *   customer.navigateTo(customerUrl);
 *   customer.run(customerFlow);
 *
 *   admin.shutdown();    // does NOT affect the customer session
 *   customer.shutdown();
 * </pre>
 *
 * <h3>Layer model</h3>
 * <pre>
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │  Tests                                                       │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │  VOID  (session façade)                                      │
 *  │    navigateTo / getCurrentUrl / getTitle / refresh           │
 *  │    run(Flow) / run(Action)                                   │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │  FlowExecutor  (internal — do not construct directly)        │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │  UIEngine  (execution contract — advanced via getEngine())   │
 *  └──────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @see Flow
 * @see Action
 * @see UIEngine
 */
public class VOID {

    /** Per-session execution context (config + driver). */
    private final ExecutionContext context;

    /** The active execution engine for this session. */
    private final UIEngine engine;

    /** Lazily-initialised, cached legacy interaction helper. */
    @Deprecated
    private Interactions interactions;

    /** Executes Actions and Flows against this session's engine. */
    private final FlowExecutor executor;

    // ===========================
    //        Construction
    // ===========================

    /**
     * Protected constructor — use {@link #start()} or {@link #start(DriverFactory.Profile)}.
     *
     * @param context the execution context for this session
     * @param engine  the active UI engine
     */
    protected VOID(ExecutionContext context, UIEngine engine) {
        this.context = context;
        this.engine = engine;
        this.executor = new FlowExecutor(engine);
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
     *   <li>Creates a {@link UIEngine} via factory</li>
     * </ol>
     *
     * @return a ready-to-use VOID session
     */
    public static VOID start() {
        return start(DriverFactory.Profile.DEFAULT);
    }

    /**
     * Starts a new VOID session with the specified driver profile.
     *
     * @param profile the driver configuration profile
     * @return a ready-to-use VOID session
     */
    public static VOID start(DriverFactory.Profile profile) {
        FrameworkBootstrap.init();

        WebDriver driver = DriverManager.createDriver(profile);
        ExecutionContext ctx = new ExecutionContext(
                FrameworkBootstrap.getUtilsConfig(),
                driver
        );

        UIEngine engine = UIEngineFactory.create(FrameworkBootstrap.getUtilsConfig(), driver);

        CustomLogger.info.log("VOID session started — engine=" + engine.getEngineName() + ", driver ready.");
        return new VOID(ctx, engine);
    }

    // ===========================
    //        Lifecycle
    // ===========================

    /**
     * Shuts down this VOID session.
     *
     * <p>Calls {@link UIEngine#shutdown()} on this session's engine (which releases
     * the browser/driver), then removes the driver reference from the thread-local
     * registry. Only this session's driver is affected — other concurrent sessions
     * on the same thread are unaffected.</p>
     */
    public void shutdown() {
        CustomLogger.info.log("VOID session shutting down — engine=" + engine.getEngineName());
        engine.shutdown();
        DriverContext.removePrimary();
    }

    // ===========================
    //   Session-Level Navigation
    // ===========================

    /**
     * Navigates this session's browser to the given URL.
     *
     * @param url the target URL
     */
    public void navigateTo(String url) {
        engine.navigateTo(url);
    }

    /**
     * Returns the current URL of this session's browser.
     *
     * @return current URL string
     */
    public String getCurrentUrl() {
        return engine.getCurrentUrl();
    }

    /**
     * Returns the title of the current page in this session.
     *
     * @return page title string
     */
    public String getTitle() {
        return engine.getTitle();
    }

    /**
     * Reloads the current page in this session's browser.
     */
    public void refresh() {
        engine.refresh();
    }

    // ===========================
    //          Execution
    // ===========================

    /**
     * Executes the given {@link Flow} using this session's engine.
     *
     * <p>Prefer this over constructing a {@link FlowExecutor} manually.</p>
     *
     * @param flow the flow to execute
     */
    public void run(Flow flow) {
        executor.run(flow);
    }

    /**
     * Executes a single {@link Action} using this session's engine.
     *
     * <p>Prefer this over constructing a {@link FlowExecutor} manually.</p>
     *
     * @param action the action to execute
     */
    public void run(Action action) {
        executor.run(action);
    }

    // ===========================
    //   Escape Hatch — Advanced
    // ===========================

    /**
     * Returns the underlying {@link UIEngine} for this session.
     *
     * <p><b>Advanced API.</b> Most tests should not need direct engine access.
     * Use this only for advanced scenarios such as custom wait strategies,
     * engine-specific native commands, or diagnostic tooling. Direct engine
     * usage bypasses the session abstraction and may reduce engine portability.</p>
     *
     * @return the active UIEngine for this session
     */
    public UIEngine getEngine() {
        return engine;
    }

    // ===========================
    //   Deprecated — Legacy
    // ===========================

    /**
     * Returns the (cached) general-purpose legacy interaction helper.
     *
     * @deprecated Since 2.1 — use {@link #run(Flow)} / {@link #run(Action)} instead.
     *             Prefer composing {@code Element → Action → Flow} and executing via
     *             {@code app.run(flow)}. Will be removed in 3.0.
     */
    @Deprecated(since = "0.1", forRemoval = true)
    public Interactions interaction() {
        if (interactions == null) {
            interactions = new Interactions(engine);
        }
        return interactions;
    }

    /**
     * Returns the {@link ExecutionContext} so subclasses
     * can access configuration without re-fetching from globals.
     *
     * @deprecated Since 2.1 — subclasses should access session state through
     *             engine-level abstractions rather than the raw execution context.
     *             Will be removed in 3.0.
     */
    @Deprecated(since = "0.1", forRemoval = true)
    protected ExecutionContext getContext() {
        return context;
    }

    /**
     * Returns the underlying {@link WebDriver} for this session.
     *
     * @deprecated Since 2.1 — exposes Selenium types directly, breaking engine
     *             portability. Use {@link #getEngine()}{@code .getNativeDriver()}
     *             for engine-specific escape-hatch access. Will be removed in 3.0.
     */
    @Deprecated(since = "0.1", forRemoval = true)
    protected WebDriver getDriver() {
        return context.getDriver();
    }
}
