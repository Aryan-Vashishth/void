package core.runtime;

import core.actions.Action;
import core.bootstrap.FrameworkBootstrap;
import core.context.SessionContext;
import domain.automation.web.engine.UIEngine;
import core.executor.FlowExecutor;
import core.flow.Flow;
import core.logging.CustomLogger;
import core.interactions.Interactions;
import org.openqa.selenium.WebDriver;

/**
 * Primary session object for the VOID framework.
 *
 * <p>A {@code VOID} instance represents a single browser session. VOID is a
 * <b>composition root</b>: it creates and holds the session's service objects and
 * exposes them through focused accessors. Tests interact with the service objects
 * rather than with VOID directly.</p>
 *
 * <h3>Service objects</h3>
 * <table>
 *   <tr><td>{@link #browser()}</td><td>{@link Browser}</td><td>Navigation and page state</td></tr>
 *   <tr><td>{@link #elements()}</td><td>{@link Elements}</td><td>Element-level queries (visibility, attributes, count)</td></tr>
 *   <tr><td>{@link #reader()}</td><td>{@link Reader}</td><td>Text reads via the action DSL</td></tr>
 *   <tr><td>{@link #debug()}</td> <td>{@link Debug}</td><td>Direct engine access (escape hatch)</td></tr>
 * </table>
 *
 * <h3>Typical usage</h3>
 * <pre>
 *   VOID app = VOID.builder().start();
 *
 *   app.browser().navigateTo("https://example.com/login");
 *
 *   app.run(Flow.of(
 *       LoginPage.USERNAME.type("admin"),
 *       LoginPage.PASSWORD.type("secret"),
 *       LoginPage.SUBMIT.click()
 *   ));
 *
 *   String error = app.reader().query(LoginPage.ErrorMessage.BANNER.getText());
 *   assertTrue(app.browser().url().contains("/dashboard"));
 *
 *   app.shutdown();
 * </pre>
 *
 * <h3>Multi-session usage</h3>
 * <pre>
 *   VOID admin    = VOID.builder().start();
 *   VOID customer = VOID.builder().start();
 *
 *   admin.browser().navigateTo(adminUrl);
 *   admin.run(loginFlow);
 *
 *   customer.browser().navigateTo(customerUrl);
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
 *  │  VOID (composition root)                                     │
 *  │    run(Flow/Action) / browser() / elements() / reader()      │
 *  │    debug() / shutdown()                                      │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │  Browser | Elements | Reader | Debug                         │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │  FlowExecutor  (internal — do not construct directly)        │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │  UIEngine  (execution contract — access via debug().engine())│
 *  └──────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @see Browser
 * @see Elements
 * @see Reader
 * @see Debug
 */
public class VOID {

    /** Lifecycle states for a VOID session. */
    public enum SessionState { ACTIVE, SHUTDOWN }

    /** Per-session context (config, executor, identity). */
    private final SessionContext context;

    /** Current lifecycle state of this session. */
    private volatile SessionState state = SessionState.ACTIVE;

    /** Lazily-initialised, cached legacy interaction helper. */
    @Deprecated
    private Interactions interactions;

    /** Executes Actions and Flows against this session's engine. */
    private final FlowExecutor executor;

    // ── Service objects ────────────────────────────────────────────────────

    private final Browser  browser;
    private final Elements elements;
    private final Reader   reader;
    private final Debug    debug;

    // ===========================
    //        Construction
    // ===========================

    /**
     * Protected constructor -- use {@link #builder()} or {@link #start()}.
     *
     * @param context the session context for this session
     */
    protected VOID(SessionContext context) {
        this.context  = context;
        this.executor = new FlowExecutor(context.engine());
        this.reader   = new Reader(executor);
        if (context.engine() instanceof UIEngine uiEngine) {
            this.browser  = new Browser(uiEngine);
            this.elements = new Elements(uiEngine);
            this.debug    = new Debug(uiEngine);
        } else {
            this.browser  = null;
            this.elements = null;
            this.debug    = null;
        }
    }

    // ===========================
    //      Static Factories
    // ===========================

    /**
     * Returns a new {@link VOIDBuilder} for fluent session configuration.
     *
     * <pre>
     *   VOID session = VOID.builder()
     *           .profile(SeleniumDriverFactory.Profile.DEFAULT)
     *           .start();
     * </pre>
     *
     * @return a new builder instance
     */
    public static VOIDBuilder builder() {
        return new VOIDBuilder();
    }

    /**
     * Starts a new VOID session with the default profile.
     *
     * @return a ready-to-use VOID session
     * @deprecated since 0.3 -- use {@link #builder()} instead.
     *             Will be removed in 1.0.
     */
    @Deprecated(since = "0.3", forRemoval = true)
    public static VOID start() {
        return builder().start();
    }

    // ===========================
    //     Service Accessors
    // ===========================

    /** Returns the {@link Browser} service for navigation and page-state queries. */
    public Browser browser() {
        if (browser == null) throw new UnsupportedOperationException("browser() requires a UIEngine session");
        return browser;
    }

    /** Returns the {@link Elements} service for capability-typed element queries. */
    public Elements elements() {
        if (elements == null) throw new UnsupportedOperationException("elements() requires a UIEngine session");
        return elements;
    }

    /** Returns the {@link Reader} service for reading element text via the action DSL. */
    public Reader reader() {
        return reader;
    }

    /**
     * Returns the {@link Debug} service for direct engine access.
     *
     * <p><b>Advanced API.</b> Most tests should not need direct engine access.
     * Use this only for advanced scenarios such as custom wait strategies,
     * engine-specific native commands, or diagnostic tooling.</p>
     */
    public Debug debug() {
        if (debug == null) throw new UnsupportedOperationException("debug() requires a UIEngine session");
        return debug;
    }

    // ===========================
    //        Lifecycle
    // ===========================

    /**
     * Shuts down this VOID session.
     *
     * <p>Calls {@link UIEngine#shutdown()} on this session's engine (which releases
     * the browser/driver), then removes the driver reference from the thread-local
     * registry. Only this session's driver is affected -- other concurrent sessions
     * on the same thread are unaffected.</p>
     */
    public void shutdown() {
        CustomLogger.info.log("VOID session shutting down -- sessionId=" + context.sessionId()
                + ", engine=" + context.getEngineName());
        context.engine().shutdown();
        state = SessionState.SHUTDOWN;
        // SeleniumDriverContext cleanup is owned by SeleniumEngine.shutdown()
    }

    /** Returns the current lifecycle state of this session. */
    public SessionState getSessionState() {
        return state;
    }

    // ===========================
    //   Deprecated -- Navigation
    // ===========================

    /**
     * @deprecated since 0.9 -- use {@link #browser()}{@code .navigateTo(url)} instead.
     *             Will be removed in 1.0.
     */
    @Deprecated(since = "0.9", forRemoval = true)
    public void navigateTo(String url) {
        browser.navigateTo(url);
    }

    /**
     * @deprecated since 0.9 -- use {@link #browser()}{@code .url()} instead.
     *             Will be removed in 1.0.
     */
    @Deprecated(since = "0.9", forRemoval = true)
    public String getCurrentUrl() {
        return browser.url();
    }

    /**
     * @deprecated since 0.9 -- use {@link #browser()}{@code .title()} instead.
     *             Will be removed in 1.0.
     */
    @Deprecated(since = "0.9", forRemoval = true)
    public String getTitle() {
        return browser.title();
    }

    /**
     * @deprecated since 0.9 -- use {@link #browser()}{@code .refresh()} instead.
     *             Will be removed in 1.0.
     */
    @Deprecated(since = "0.9", forRemoval = true)
    public void refresh() {
        browser.refresh();
    }

    // ===========================
    //         Execution
    // ===========================

    /**
     * Executes the given {@link Flow} using this session's engine.
     *
     * @param flow the flow to execute
     */
    public void run(Flow flow) {
        executor.run(flow);
    }

    /**
     * Executes a single {@link Action} using this session's engine.
     *
     * @param action the action to execute
     */
    public void run(Action action) {
        executor.run(action);
    }

    // ===========================
    //   Deprecated -- Escape Hatch
    // ===========================

    /**
     * Returns the underlying {@link UIEngine} for this session.
     *
     * @deprecated since 0.9 -- use {@link #debug()}{@code .engine()} instead.
     *             Will be removed in 1.0.
     */
    @Deprecated(since = "0.9", forRemoval = true)
    public UIEngine getEngine() {
        return debug.engine();
    }

    // ===========================
    //   Deprecated -- Legacy
    // ===========================

    /**
     * Returns the (cached) general-purpose legacy interaction helper.
     *
     * @deprecated Since 2.1 -- use {@link #flow()}{@code .run(Flow)} instead.
     *             Prefer composing {@code UIElement -> Action -> Flow} and executing via
     *             {@code app.flow().run(flow)}. Will be removed in 3.0.
     */
    @Deprecated(since = "0.1", forRemoval = true)
    public Interactions interaction() {
        if (interactions == null) {
            interactions = new Interactions((UIEngine) context.engine());
        }
        return interactions;
    }

    /**
     * Returns the {@link SessionContext} for this session.
     *
     * @deprecated since 0.1 -- subclasses should access session state through
     *             engine-level abstractions rather than the raw context.
     *             Will be removed in 1.0.
     */
    @Deprecated(since = "0.1", forRemoval = true)
    protected SessionContext getContext() {
        return context;
    }

    /**
     * Returns the underlying {@link WebDriver} for this session.
     *
     * @deprecated since 0.1 -- exposes Selenium types directly, breaking engine
     *             portability. Use {@link #debug()}{@code .engine().getNativeDriver()}
     *             for engine-specific escape-hatch access. Will be removed in 1.0.
     */
    @Deprecated(since = "0.1", forRemoval = true)
    protected WebDriver getDriver() {
        return (WebDriver) ((UIEngine) context.engine()).getNativeDriver();
    }
}
