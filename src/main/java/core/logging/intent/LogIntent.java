package core.logging.intent;

/**
 * Classifies <em>what</em> a log line is communicating — independently of the
 * log level. The theme maps each intent to a <b>foreground color</b>; the
 * log level supplies the <b>background color</b>. The two are composed at
 * render time via {@code ThemeColors#resolve(String, LogIntent)}.
 */
public enum LogIntent {
    /** Plain log/label lines — text color matches the log level. */
    BASE,
    /** User interaction: click, checkbox, text, input, dropdown, toggle, upload. */
    INTERACTION,
    /** Page navigation: tab, frame, breadcrumb. */
    NAVIGATION,
    /** Observing / reading: wait, search, result. */
    OBSERVE,
    /** Active verification step: verifying, asserting. */
    VERIFY,
    /** Structured data: table, grid, row. */
    DATA,
    /** Positive outcome: success, complete, resolved. */
    SUCCESS,
    /** Negative / warning signals: error[x], failed, timeout, validation, fallback, skip. */
    ALERT
}

