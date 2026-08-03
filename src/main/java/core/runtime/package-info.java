/**
 * {@code core.runtime} — Framework entry point and session lifecycle.
 *
 * <p>Contains the {@link core.runtime.VOID} façade, which is the primary entry point
 * for starting and managing a VOID automation session. This class orchestrates the
 * full startup pipeline: bootstrap → driver creation → engine initialisation.</p>
 *
 * <h3>Key type</h3>
 * <ul>
 *   <li>{@link core.runtime.VOID} — the main façade that wires together all framework
 *       components. Provides access to the {@link core.engine.UIEngine} and the legacy
 *       {@link core.interactions.Interactions} helper.</li>
 * </ul>
 *
 * <h3>Startup pipeline</h3>
 * <pre>
 *   VOID.start()
 *     → FrameworkBootstrap.init()          (one-time: validate configs, seed utils)
 *     → SeleniumDriverManager.createDriver()       (create + register WebDriver)
 *     → UIEngineFactory.create()           (instantiate engine from config)
 *     → ExecutionContext                   (holds config + driver for this session)
 *     → return VOID façade                 (thin wrapper, delegates to context)
 * </pre>
 *
 * <h3>Usage</h3>
 * <pre>
 *   // Start a session
 *   VOID app = VOID.start();
 *
 *   // Access the engine for modern Action/Flow usage
 *   UIEngine engine = app.getEngine();
 *   FlowExecutor executor = new FlowExecutor(engine);
 *   executor.run(Flow.of(LoginPage.USERNAME.type("admin")));
 *
 *   // Or use the legacy Interactions API
 *   app.interaction().clickOn(MyPage.SUBMIT_BUTTON);
 *
 *   // Shut down
 *   app.shutdown();
 * </pre>
 *
 * <h3>Layer model</h3>
 * <pre>
 *   ┌─────────────────────────────────────────────────────┐
 *   │  DSL layer         (dsl.VoidDSL)                    │
 *   ├─────────────────────────────────────────────────────┤
 *   │  Framework layer   (core.runtime.VOID)              │
 *   │    → Interactions  (legacy, frozen)                 │
 *   │    → UIEngine      (modern, preferred)              │
 *   └─────────────────────────────────────────────────────┘
 * </pre>
 *
 * @see core.runtime.VOID
 * @see core.bootstrap.FrameworkBootstrap
 * @see core.engine.UIEngine
 */
package core.runtime;

