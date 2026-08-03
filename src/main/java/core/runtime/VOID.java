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
 * <p>A {@code VOID} instance represents a single browser session. Tests should
 * think in terms of a session, not an engine. The majority of test code should
 * interact only with {@code VOID}, {@link Flow}, {@link Action}, and
 * {@code UIElement} types.</p>
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

    // ===========================
    //        Construction
    // ===========================

    /**
     * Protected constructor -- use {@link #builder()} or {@link #start()}.
     *
     * @param context the session context for this session
     */
    protected VOID(SessionContext context) {
        this.context = context;
        this.executor = new FlowExecutor(context.engine());
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
    //   Session-Level Navigation
    // ===========================

    /**
     * Navigates this session's browser to the given URL.
     *
     * @param url the target URL
     */
    public void navigateTo(String url) {
        executor.run(e -> ((UIEngine) e).navigateTo(url));
    }

    /**
     * Returns the current URL of this session's browser.
     *
     * @return current URL string
     */
    public String getCurrentUrl() {
        String[] result = {null};
        executor.run(e -> result[0] = ((UIEngine) e).getCurrentUrl());
        return result[0];
    }

    /**
     * Returns the title of the current page in this session.
     *
     * @return page title string
     */
    public String getTitle() {
        String[] result = {null};
        executor.run(e -> result[0] = ((UIEngine) e).getTitle());
        return result[0];
    }

    /**
     * Reloads the current page in this session's browser.
     */
    public void refresh() {
        executor.run(e -> ((UIEngine) e).refresh());
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
        return (UIEngine) context.engine();
    }

    // ===========================
    //   Deprecated — Legacy
    // ===========================

    /**
     * Returns the (cached) general-purpose legacy interaction helper.
     *
     * @deprecated Since 2.1 — use {@link #run(Flow)} / {@link #run(Action)} instead.
     *             Prefer composing {@code UIElement → Action → Flow} and executing via
     *             {@code app.run(flow)}. Will be removed in 3.0.
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
     *             portability. Use {@link #getEngine()}{@code .getNativeDriver()}
     *             for engine-specific escape-hatch access. Will be removed in 1.0.
     */
    @Deprecated(since = "0.1", forRemoval = true)
    protected WebDriver getDriver() {
        return (WebDriver) ((UIEngine) context.engine()).getNativeDriver();
    }
}
