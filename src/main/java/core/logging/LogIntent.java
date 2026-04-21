package core.logging;

/**
 * Classifies <em>what</em> a log line is communicating — independently of the
 * log level. The theme maps each intent to a <b>foreground color</b>; the
 * log level supplies the <b>background color</b>. The two are composed at
 * render time via {@link ThemeColors#resolve(String, LogIntent)}.
 *
 * <table border="1">
 * <tr><th>Intent</th><th>Actions</th></tr>
 * <tr><td>BASE</td><td>log(), info/warn/error/debug label lines</td></tr>
 * <tr><td>INTERACTION</td><td>click, checkbox, text, input, dropdown, toggle, upload</td></tr>
 * <tr><td>NAVIGATION</td><td>tab, frame, breadcrumb</td></tr>
 * <tr><td>OBSERVE</td><td>wait, search, result</td></tr>
 * <tr><td>DATA</td><td>table, grid, row</td></tr>
 * <tr><td>SUCCESS</td><td>success, complete, resolved</td></tr>
 * <tr><td>ALERT</td><td>error[x], failed, timeout, validation, fallback, skip</td></tr>
 * </table>
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
    /** Structured data: table, grid, row. */
    DATA,
    /** Positive outcome: success, complete, resolved. */
    SUCCESS,
    /** Negative / warning signals: error[x], failed, timeout, validation, fallback, skip. */
    ALERT
}

