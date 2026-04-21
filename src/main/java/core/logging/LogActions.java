package core.logging;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static core.logging.AnsiColors.*;

/**
 * Base class for all log-level instances (Info, Warn, Error, Debug).
 *
 * <p>Every action method resolves its ANSI style via
 * {@link ThemeColors#resolve(String, LogIntent)}, compositing the action's
 * {@link LogIntent} foreground with the current log level's background.</p>
 *
 * <p>This means the same action (e.g. {@code click()}) automatically renders with a
 * different background when called through {@code debug.*} vs {@code info.*} —
 * no per-level overrides required.</p>
 */
public class LogActions {

    protected final String logLevel;

    public LogActions(String logLevel) { this.logLevel = logLevel; }

    // ── INTERACTION group ─────────────────────────────────────────────────────
    public void click(String message)      { logMessage(LogIntent.INTERACTION, "CLICK [>]",       message); }
    public void checkbox(String message)   { logMessage(LogIntent.INTERACTION, "CHECKBOX [x]",    message); }
    public void text(String message)       { logMessage(LogIntent.INTERACTION, "TEXT [T]",        message); }
    public void input(String message)      { logMessage(LogIntent.INTERACTION, "INPUT [>>]",      message); }
    public void dropdown(String message)   { logMessage(LogIntent.INTERACTION, "DROPDOWN [v]",    message); }
    public void toggle(String message)     { logMessage(LogIntent.INTERACTION, "TOGGLE [o]",      message); }
    public void upload(String message)     { logMessage(LogIntent.INTERACTION, "UPLOAD [^]",      message); }

    // ── NAVIGATION group ──────────────────────────────────────────────────────
    public void tab(String message)        { logMessage(LogIntent.NAVIGATION,  "TAB [->]",        message); }
    public void frame(String message)      { logMessage(LogIntent.NAVIGATION,  "FRAME [{}]",      message); }
    public void breadcrumb(String message) { logMessage(LogIntent.NAVIGATION,  "BREADCRUMB [/]",  message); }

    // ── OBSERVE group ─────────────────────────────────────────────────────────
    public void wait(String message)       { logMessage(LogIntent.OBSERVE,     "WAIT [~]",        message); }
    public void search(String message)     { logMessage(LogIntent.OBSERVE,     "SEARCHED [*]",    message); }
    public void result(String message)     { logMessage(LogIntent.OBSERVE,     "RESULT [:]",      message); }

    // ── DATA group ────────────────────────────────────────────────────────────
    public void table(String message)      { logMessage(LogIntent.DATA,        "TABLE [=]",       message); }
    public void grid(String message)       { logMessage(LogIntent.DATA,        "GRID [#]",        message); }

    // ── SUCCESS group ─────────────────────────────────────────────────────────
    public void success(String message)    { logMessage(LogIntent.SUCCESS,     "SUCCESS [+]",     message); }
    public void complete(String message)   { logMessage(LogIntent.SUCCESS,     "COMPLETE [+]",    message); }
    public void resolved(String message)   { logMessage(LogIntent.SUCCESS,     "RESOLVED [ok]",   message); }

    // ── ALERT group ───────────────────────────────────────────────────────────
    public void error(String message)      { logMessage(LogIntent.ALERT,       "ERROR [x]",       message); }
    public void failed(String message)     { logMessage(LogIntent.ALERT,       "FAILED [x]",      message); }
    public void timeout(String message)    { logMessage(LogIntent.ALERT,       "TIMEOUT [!!]",    message); }
    public void validation(String message) { logMessage(LogIntent.ALERT,       "VALIDATION [?!]", message); }
    public void fallback(String message)   { logMessage(LogIntent.ALERT,       "FALLBACK [<-]",   message); }
    public void skip(String message)       { logMessage(LogIntent.ALERT,       "SKIP [>>]",       message); }

    // ── Object overloads ──────────────────────────────────────────────────────

    public void log(Object obj) {
        if (obj == null)                    { logMessage(LogIntent.BASE, "LOG", "null"); }
        else if (obj instanceof Map<?, ?> m){ table(m); }
        else if (obj instanceof List<?> l)  { logList(l); }
        else if (obj.getClass().isArray())  { logList(java.util.Arrays.asList((Object[]) obj)); }
        else                                { logMessage(LogIntent.BASE, "LOG", obj.toString()); }
    }

    public void log(String heading, Object obj) {
        if (obj == null)                    { logMessage(LogIntent.BASE, "LOG", heading + ": null"); }
        else if (obj instanceof Map<?, ?> m){ logMessage(LogIntent.BASE, "LOG", heading + ":"); table(m); }
        else if (obj instanceof List<?> l)  { logMessage(LogIntent.BASE, "LOG", heading + ":"); logList(l); }
        else if (obj.getClass().isArray())  { logMessage(LogIntent.BASE, "LOG", heading + ":"); logList(java.util.Arrays.asList((Object[]) obj)); }
        else                                { logMessage(LogIntent.BASE, "LOG", heading + ": " + obj); }
    }

    public void log(List<?> list)  { logList(list); }

    public void log(String heading, Object... pairs) {
        treeInternal(heading, fields(pairs), LogIntent.BASE, "LOG");
    }

    /** Overridable stub — subclasses (Info/Warn/Error/Debug) override with level-specific labels. */
    public void log(String message)                         { logMessage(LogIntent.BASE, "LOG", message); }
    /** Overridable stub — subclasses (Info/Warn/Error/Debug) override with level-specific labels. */
    public void log(String heading, Map<String, ?> fields)  { treeInternal(heading, fields, LogIntent.BASE, "LOG"); }

    // ── Helper: key/value field builder ──────────────────────────────────────

    public static LinkedHashMap<String, Object> fields(Object... pairs) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        if (pairs.length % 2 != 0)
            throw new IllegalArgumentException("fields() requires an even number of key/value arguments");
        for (int i = 0; i < pairs.length; i += 2)
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        return map;
    }

    private void logList(List<?> list) {
        if (list == null || list.isEmpty()) { logMessage(LogIntent.BASE, "LOG", "(empty list)"); return; }
        int i = 0;
        for (Object item : list) {
            String prefix = "  [" + (i++) + "] ";
            if (item instanceof Map<?, ?> m) { logMessage(LogIntent.BASE, "LOG", prefix); table(m); }
            else                             { logMessage(LogIntent.BASE, "LOG", prefix + item); }
        }
    }

    // ── Table rendering ───────────────────────────────────────────────────────

    private static String center(String s, int width) {
        String plain = s.replaceAll("\\u001B\\[[;\\d]*m", "");
        int len = plain.length();
        if (len >= width) return s.substring(0, width);
        int left = (width - len) / 2;
        return " ".repeat(left) + s + " ".repeat(width - len - left);
    }

    public void table(List<? extends Map<?, ?>> rows, String title) {
        if (rows == null || rows.isEmpty()) {
            logMessage(LogIntent.DATA, "TABLE", "No rows to display.");
            return;
        }
        LinkedHashMap<String, Integer> colWidths = new LinkedHashMap<>();
        for (Map<?, ?> row : rows) {
            for (Object key : row.keySet()) {
                String col = String.valueOf(key);
                String val = LoggerContext.truncateCell(String.valueOf(row.get(key)));
                colWidths.put(col, Math.max(
                        colWidths.getOrDefault(col, col.length()),
                        Math.max(col.length(), val.length())));
            }
        }
        List<String> headers = new java.util.ArrayList<>(colWidths.keySet());
        boolean ansi = LogConfig.current().isAnsiEnabled();
        String color = ansi ? BuiltInThemes.getColors().resolve(logLevel, LogIntent.DATA) : "";
        String rst   = ansi ? BuiltInThemes.getColors().reset() : "";

        String hBorder  = "+" + headers.stream()
                .map(h -> "-".repeat(colWidths.get(h) + 2))
                .reduce((a, b) -> a + "+" + b).orElse("") + "+";
        String headerRow = "|" + headers.stream()
                .map(h -> " " + LoggerContext.truncateCell(h) + " ".repeat(colWidths.get(h) - h.length()) + " ")
                .reduce((a, b) -> a + "|" + b).orElse("") + "|";

        StringBuilder sb = new StringBuilder();
        sb.append(hBorder).append("\n");

        if (title != null && !title.isEmpty()) {
            String bold      = ansi ? BOLD  : "";
            String resetBold = ansi ? RESET : "";
            sb.append("|").append(bold)
              .append(center(" " + title + " ", headerRow.length() - 2))
              .append(resetBold).append("|\n");
            sb.append(hBorder).append("\n");
        }

        sb.append(headerRow).append("\n").append(hBorder).append("\n");
        for (Map<?, ?> row : rows) {
            String rowStr = "|" + headers.stream().map(h -> {
                Object v = null;
                for (Object key : row.keySet())
                    if (String.valueOf(key).equals(h)) { v = row.get(key); break; }
                if (v == null) v = "";
                String s = LoggerContext.truncateCell(String.valueOf(v));
                return " " + s + " ".repeat(colWidths.get(h) - s.length()) + " ";
            }).reduce((a, b) -> a + "|" + b).orElse("") + "|";
            sb.append(rowStr).append("\n");
        }
        sb.append(hBorder);
        logMessage(color, "TABLE", "\n" + sb + rst);
    }

    public void table(List<? extends Map<?, ?>> rows) { table(rows, null); }

    public void table(Map<?, ?> row, String title) {
        if (row == null || row.isEmpty()) { logMessage(LogIntent.DATA, "TABLE", "No data to display."); return; }
        table(java.util.Collections.singletonList(row), title);
    }

    public void table(Map<?, ?> row) { table(row, null); }

    public void row(Map<?, ?> data) {
        if (data == null || data.isEmpty()) { logMessage(LogIntent.DATA, "ROW", "(empty)"); return; }
        int maxKeyLen = data.keySet().stream()
                .map(k -> String.valueOf(k).length()).max(Integer::compareTo).orElse(0);
        for (Map.Entry<?, ?> entry : data.entrySet()) {
            logMessage(LogIntent.DATA, "ROW",
                    String.format("%-" + maxKeyLen + "s : %s",
                            entry.getKey(),
                            LoggerContext.truncateCell(String.valueOf(entry.getValue()))));
        }
    }

    // ── Tree / resolved ───────────────────────────────────────────────────────

    public void tree(String heading, Map<String, ?> fields) {
        treeInternal(heading, fields, LogIntent.BASE, "LOG");
    }

    public void tree(String heading, Object... pairs) {
        treeInternal(heading, fields(pairs), LogIntent.BASE, "LOG");
    }

    public void resolved(String heading, Map<String, ?> fields) {
        treeInternal(heading, fields, LogIntent.SUCCESS, "RESOLVED");
    }

    public void resolved(String heading, Object... pairs) {
        treeInternal(heading, fields(pairs), LogIntent.SUCCESS, "RESOLVED");
    }

    /** Internal tree renderer — composes color from intent + level, then logs each branch. */
    protected void treeInternal(String heading, Map<String, ?> fields,
                                LogIntent intent, String label) {
        if (heading == null || fields == null) return;
        logMessage(intent, label, heading);
        int size = fields.size(), i = 0;
        for (Map.Entry<String, ?> entry : fields.entrySet()) {
            i++;
            String prefix = (i < size) ? "          \u251C\u2500 " : "          \u2514\u2500 ";
            Object v = entry.getValue();
            String value;
            if (v != null && v.getClass().isArray()) {
                if      (v instanceof Object[]  a) value = java.util.Arrays.deepToString(a);
                else if (v instanceof int[]     a) value = java.util.Arrays.toString(a);
                else if (v instanceof long[]    a) value = java.util.Arrays.toString(a);
                else if (v instanceof double[]  a) value = java.util.Arrays.toString(a);
                else if (v instanceof boolean[] a) value = java.util.Arrays.toString(a);
                else if (v instanceof char[]    a) value = java.util.Arrays.toString(a);
                else if (v instanceof float[]   a) value = java.util.Arrays.toString(a);
                else if (v instanceof short[]   a) value = java.util.Arrays.toString(a);
                else if (v instanceof byte[]    a) value = java.util.Arrays.toString(a);
                else value = String.valueOf(v);
            } else {
                value = String.valueOf(v);
            }
            logMessage(intent, label,
                    prefix + String.format("%-12s: %s", entry.getKey(), value));
        }
    }

    // ── Core rendering ────────────────────────────────────────────────────────

    /**
     * Primary render path — resolves ANSI style from {@code intent} + {@code logLevel},
     * then delegates to {@link #logMultiline}.
     */
    protected void logMessage(LogIntent intent, String actionLabel, String message) {
        logMultiline(BuiltInThemes.getColors().resolve(logLevel, intent),
                     actionLabel, message, LoggerContext.isDebugEnabled());
    }

    /**
     * Low-level render path with an explicit pre-composed ANSI style string.
     * Used by the table renderer which builds its own multi-line block.
     */
    protected void logMessage(String actionColor, String actionLabel, String message) {
        logMultiline(actionColor, actionLabel, message, LoggerContext.isDebugEnabled());
    }

    /**
     * Splits {@code message} on newlines and emits one log entry per line.
     */
    protected void logMultiline(String actionColor, String actionLabel,
                                String message, boolean showCaller) {
        if (message == null) message = "null";
        LogConfig cfg = LogConfig.current();
        String ts         = java.time.LocalDateTime.now().format(cfg.getTsFormat());
        String callerText = showCaller ? getCallerString() : "";
        String[] lines    = message.split("\\R", -1);
        boolean ansi      = cfg.isAnsiEnabled();

        String div = cfg.getSegmentDivider();
        for (int i = 0; i < lines.length; i++) {
            // e.g.  [2026-04-21 13:16:39.123] │ CLICK [>] │ message text │ Caller.method ← Parent.method
            String body = ts + div + actionLabel + div + lines[i];
            String out;
            if (ansi) {
                if (i == 0 && !callerText.isEmpty() && cfg.isCallerColorEnabled()) {
                    out = actionColor + body + RESET
                        + div + BuiltInThemes.getColors().callerFg() + callerText + RESET;
                } else {
                    String cp = (i == 0 && !callerText.isEmpty()) ? div + callerText : "";
                    out = actionColor + body + cp + RESET;
                }
            } else {
                String cp = (i == 0 && !callerText.isEmpty()) ? div + callerText : "";
                out = body + cp;
            }
            switch (logLevel) {
                case "ERROR" -> LoggerContext.getLogger().error(out);
                case "WARN"  -> LoggerContext.getLogger().warn(out);
                case "INFO"  -> LoggerContext.getLogger().info(out);
                default      -> LoggerContext.getLogger().debug(out);
            }
        }
    }

    // ── Caller resolution ─────────────────────────────────────────────────────

    private static String simpleClass(String fqcn) {
        int i = (fqcn == null) ? -1 : fqcn.lastIndexOf('.');
        return (i >= 0) ? fqcn.substring(i + 1) : (fqcn == null ? "" : fqcn);
    }

    private static boolean filteredOut(String className, String methodName) {
        LogConfig cfg = LogConfig.current();
        if (methodName != null)
            for (String p : cfg.getSuppressMethodPrefixes())
                if (methodName.startsWith(p)) return true;
        if (className != null) {
            if (!cfg.getIncludeOnlyPrefixes().isEmpty()) {
                if (cfg.getIncludeOnlyPrefixes().stream().noneMatch(className::startsWith)) return true;
            }
            for (String s : cfg.getSuppressContains())
                if (className.contains(s) || className.startsWith(s)) return true;
        }
        return false;
    }

    /** Returns {@code "Callee.method ← Caller.method"} as plain text. */
    protected String getCallerString() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        int calleeIdx = -1, callerIdx = -1;
        for (int i = 3; i < st.length; i++) {
            if (filteredOut(st[i].getClassName(), st[i].getMethodName())) continue;
            if (calleeIdx == -1) { calleeIdx = i; continue; }
            if (st[i].getClassName().equals(st[calleeIdx].getClassName())
                    && st[i].getMethodName().equals(st[calleeIdx].getMethodName())) continue;
            callerIdx = i; break;
        }
        if (calleeIdx == -1) return "";
        String cm = st[calleeIdx].getMethodName();
        String left = simpleClass(st[calleeIdx].getClassName()) + "."
                + ("<init>".equals(cm) ? "(constructor)" : "<clinit>".equals(cm) ? "(static init)" : cm);
        if (callerIdx == -1) return left;
        String pm = st[callerIdx].getMethodName();
        return left + " \u2190 " + simpleClass(st[callerIdx].getClassName()) + "."
                + ("<init>".equals(pm) ? "(constructor)" : "<clinit>".equals(pm) ? "(static init)" : pm);
    }

    /** Convenience for callers that need a pre-composed style (e.g. table renderer). */
    protected String getLevelColor() {
        return BuiltInThemes.getColors().resolve(logLevel, LogIntent.BASE);
    }
}

