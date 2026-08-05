package core.runtime;

import core.logging.CustomLogger;
import domain.automation.web.engine.UIEngine;

/**
 * Session service for advanced / diagnostic engine access.
 *
 * <p>Obtained via {@link VOID#debug()}. Direct engine access bypasses session
 * abstractions and reduces engine portability. Use only for custom wait strategies,
 * engine-specific native commands, or diagnostic tooling.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   // Prefer app.elements() and app.reader() for normal queries.
 *   // Use debug() only when those don't cover the case.
 *   app.debug().engine().waitUntil(...);
 * </pre>
 */
public final class Debug {

    private final UIEngine engine;

    Debug(UIEngine engine) {
        this.engine = engine;
    }

    /**
     * Returns the underlying {@link UIEngine} for this session.
     *
     * <p><b>Warning:</b> direct engine access bypasses session abstractions and reduces
     * engine portability. Prefer {@code app.browser()}, {@code app.elements()}, and
     * {@code app.reader()} for all standard interactions.</p>
     */
    public UIEngine engine() {
        CustomLogger.warn.log(
            "[Debug] Direct engine access via app.debug().engine() -- " +
            "bypasses session abstractions. Use app.browser() / app.elements() / app.reader() if possible."
        );
        return engine;
    }
}
