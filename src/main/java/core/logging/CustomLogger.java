package core.logging;

import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class CustomLogger {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final int MAX_COL_WIDTH = 40;

    private static String truncateCell(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\R", " ");
        return s.length() > MAX_COL_WIDTH ? s.substring(0, MAX_COL_WIDTH - 3) + "..." : s;
    }

    public static boolean isDebugEnabled() {
        return getSafeLogger().isDebugEnabled();
    }

    // === ANSI COLOR CONSTANTS ===
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BOLD = "\u001B[1m";

    // Foreground colors
    public static final String FG_BLACK = "\u001B[30m";
    public static final String FG_RED = "\u001B[31m";
    public static final String FG_GREEN = "\u001B[32m";
    public static final String FG_YELLOW = "\u001B[33m";
    public static final String FG_BLUE = "\u001B[34m";
    public static final String FG_MAGENTA = "\u001B[35m";
    public static final String FG_CYAN = "\u001B[36m";
    public static final String FG_WHITE = "\u001B[37m";
    public static final String FG_BRIGHT_WHITE = "\u001B[97m";
    public static final String FG_BRIGHT_CYAN = "\u001B[96m";
    public static final String FG_BRIGHT_YELLOW = "\u001B[93m";
    public static final String FG_BRIGHT_RED = "\u001B[91m";
    public static final String FG_BRIGHT_MAGENTA = "\u001B[95m";
    public static final String FG_BRIGHT_BLUE = "\u001B[94m";
    public static final String FG_BRIGHT_GREEN = "\u001B[92m";
    public static final String FG_ORANGE_208 = "\u001B[38;5;208m";
    public static final String FG_BOLD_ORANGE_208 = "\u001B[38;5;208;1m";
    public static final String FG_DIM_WHITE = "\u001B[37;2m";

    // Background colors
    public static final String BG_BLACK = "\u001B[40m";
    public static final String BG_WHITE = "\u001B[47m";
    public static final String BG_ORANGE_208 = "\u001B[48;5;208m";
    public static final String BG_RED = "\u001B[41m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_MAGENTA = "\u001B[45m";
    public static final String BG_CYAN = "\u001B[46m";
    public static final String BG_BRIGHT_CYAN = "\u001B[106m";
    public static final String BG_BRIGHT_WHITE = "\u001B[107m";
    public static final String BG_BRIGHT_YELLOW = "\u001B[103m";
    public static final String BG_BRIGHT_RED = "\u001B[101m";
    public static final String BG_BRIGHT_GREY = "\u001B[47m";
    public static final String BG_BRIGHT_MAGENTA = "\u001B[105m";
    public static final String BG_GREY_100 = "\u001B[100m";
    public static final String BG_DARKER_GREY = "\u001B[48;5;236m";
    public static final String BG_DARKER_BLUE = "\u001B[48;5;17m";
    public static final String BG_DARKER_GREEN = "\u001B[48;5;22m";
    public static final String BG_DARKER_MAGENTA = "\u001B[48;5;53m";
    public static final String BG_DARKER_YELLOW = "\u001B[48;5;94m";
    public static final String BG_DARKER_RED = "\u001B[48;5;52m";
    public static final String BG_MAROON_RED = "\u001B[48;5;88m";

    // === END OF COLOR CONSTANTS ===

    public enum LogTheme {
        INDUSTRIAL_STEEL, NIGHT_CLUB, CARBON_ORANGE, MODERN_CLEAN
    }

    public record ThemeColors(
            String classNameStyle,
            String methodNameStyle,
            String infoStyle,
            String warnStyle,
            String errorStyle,
            String clickStyle,
            String waitStyle,
            String actionStyle,
            String tableStyle,
            String successStyle,
            String reset
    ) {
    }

    private static final ThemeColors INDUSTRIAL_STEEL = new ThemeColors(
            FG_BRIGHT_WHITE + BG_GREY_100,
            FG_BRIGHT_BLUE + BG_GREY_100,
            FG_BLACK + BG_WHITE,
            FG_BLACK + BG_YELLOW,
            FG_BLACK + BG_RED,
            FG_BLUE + BG_BLACK,
            FG_CYAN + BG_BLACK,
            FG_MAGENTA + BG_BLACK,
            FG_WHITE + BG_BLACK,
            FG_GREEN + BG_BLACK,
            ANSI_RESET
    );
    private static final ThemeColors DISCO = new ThemeColors(
            FG_BRIGHT_WHITE + BG_MAGENTA,
            FG_BRIGHT_BLUE + BG_MAGENTA,
            FG_BLACK + BG_CYAN,
            FG_BLACK + BG_BRIGHT_YELLOW,
            FG_BLACK + BG_BRIGHT_RED,
            FG_BRIGHT_MAGENTA + BG_BLACK,
            FG_BRIGHT_BLUE + BG_BLACK,
            FG_GREEN + BG_BLACK,
            FG_BRIGHT_MAGENTA + BG_BLACK,
            FG_BRIGHT_GREEN + BG_BLACK,
            ANSI_RESET
    );
    private static final ThemeColors CARBON_ORANGE = new ThemeColors(
            FG_BRIGHT_WHITE + BG_BLACK,
            FG_BRIGHT_BLUE + BG_BLACK,
            FG_BLACK + BG_BRIGHT_WHITE,
            FG_BLACK + BG_ORANGE_208,
            FG_BLACK + BG_RED,
            FG_BOLD_ORANGE_208 + BG_BLACK,
            FG_CYAN + BG_BLACK,
            FG_ORANGE_208 + BG_BLACK,
            FG_WHITE + BG_BLACK,
            FG_BRIGHT_GREEN + BG_BLACK,
            ANSI_RESET
    );
    private static final ThemeColors MODERN_CLEAN = new ThemeColors(
            FG_BRIGHT_WHITE + BG_GREY_100,                    // class chip
            FG_BRIGHT_BLUE + ANSI_BOLD + BG_GREY_100,         // method chip
            FG_BRIGHT_WHITE + ANSI_BOLD + BG_WHITE,           // info
            FG_BRIGHT_WHITE + ANSI_BOLD + BG_YELLOW,          // warn
            FG_BRIGHT_WHITE + ANSI_BOLD + BG_DARKER_RED,      // error
            FG_BRIGHT_WHITE + ANSI_BOLD + BG_MAGENTA,         // click
            FG_BRIGHT_WHITE + ANSI_BOLD + BG_YELLOW,          // wait
            FG_BRIGHT_WHITE + ANSI_BOLD + BG_MAGENTA,         // action
            FG_BRIGHT_WHITE + ANSI_BOLD + BG_WHITE,           // table
            FG_BRIGHT_WHITE + ANSI_BOLD + BG_GREEN,           // success
            ANSI_RESET
    );

    private static LogTheme currentTheme = LogTheme.MODERN_CLEAN;

    public static void setTheme(LogTheme theme) {
        currentTheme = theme;
    }

    private static ThemeColors getColors() {
        return switch (currentTheme) {
            case MODERN_CLEAN -> MODERN_CLEAN;
            case INDUSTRIAL_STEEL -> INDUSTRIAL_STEEL;
            case NIGHT_CLUB -> DISCO;
            case CARBON_ORANGE -> CARBON_ORANGE;
        };
    }

    public static final Debug debug = new Debug();
    public static final Info info = new Info();
    public static final Warn warn = new Warn();
    public static final Error error = new Error();

    public static LinkedHashMap<String, Object> fields(Object... pairs) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        if (pairs.length % 2 != 0)
            throw new IllegalArgumentException("fields() expects even number of arguments as key/value pairs");
        int i = 0;
        while (i < pairs.length) {
            String key = String.valueOf(pairs[i]);
            Object value = pairs[i + 1];
            map.put(key, value);
            i += 2;
        }
        return map;
    }

    protected static Logger log = Logger.getLogger(CustomLogger.class);
    private static final AtomicBoolean isAnsiEnabled = new AtomicBoolean(true);

    public static void enableAnsi() {
        isAnsiEnabled.set(true);
    }

    public static void disableAnsi() {
        isAnsiEnabled.set(false);
    }

    public static void initialize(Class<?> clazz) {
        log = (clazz != null) ? Logger.getLogger(clazz) : Logger.getLogger(CustomLogger.class);
    }

    // ---------------- Configurable call-chain filtering ----------------
    private static final Set<String> SUPPRESS_CONTAINS = java.util.Collections.synchronizedSet(new LinkedHashSet<>(List.of(
            "core.logging.CustomLogger",
            "org.apache.log4j",
            "java.",
            "sun.",
            "jdk.",
            "com.sun.proxy",
            "jdk.proxy",
            "net.bytebuddy",
            "reflect."
    )));
    private static final Set<String> SUPPRESS_METHOD_PREFIXES = java.util.Collections.synchronizedSet(new LinkedHashSet<>(List.of(
            "log", "debug", "info", "warn", "error", "lambda$", "invoke"
    )));
    private static final Set<String> INCLUDE_ONLY_PREFIXES = java.util.Collections.synchronizedSet(new LinkedHashSet<>());

    /**
     * Only consider frames whose class starts with one of these prefixes.
     */
    public static void includeOnlyPackages(String... prefixes) {
        INCLUDE_ONLY_PREFIXES.clear();
        if (prefixes != null) for (String p : prefixes) if (p != null && !p.isBlank()) INCLUDE_ONLY_PREFIXES.add(p);
    }

    /**
     * Add class-name substrings to suppress from call chain.
     */
    public static void suppressClassContains(String... substrings) {
        if (substrings != null) for (String s : substrings) if (s != null && !s.isBlank()) SUPPRESS_CONTAINS.add(s);
    }

    /**
     * Remove include-only filter (go back to default skipping).
     */
    public static void clearIncludes() {
        INCLUDE_ONLY_PREFIXES.clear();
    }
    // -------------------------------------------------------------------

    // --- Debug Level ---
    public static class Debug extends LogActions {
        public Debug() {
            super("DEBUG");
        }

        public void log(String message) {
            // Black bar look (invert text color)
            logMessage(getColors().infoStyle(), "DEBUG", message, true);
        }

        public void log(String heading, Map<String, ?> fields) {
            tree(heading, fields, getColors().infoStyle(), "DEBUG", true);
        }

        public void log(String heading, Object... pairs) {
            log(heading, CustomLogger.fields(pairs));
        }

        public void click(String message) {
            logMessage(getColors().clickStyle(), "CLICK", message, true);
        }

        public void wait(String message) {
            logMessage(getColors().waitStyle(), "WAIT", message, true);
        }

        public void text(String message) {
            logMessage(getColors().actionStyle(), "TEXT", message, true);
        }

        public void success(String message) {
            logMessage(getColors().successStyle(), "SUCCESS✅", message, true);
        }

        public void complete(String message) {
            logMessage(getColors().successStyle(), "COMPLETE✅", message, true);
        }

        public void error(String message) {
            logMessage(getColors().errorStyle(), "ERROR", message, true);
        }

        public void table(String message) {
            logMessage(getColors().tableStyle(), "TABLE", message, true);
        }

        public void grid(String message) {
            logMessage(getColors().tableStyle(), "GRID", message, true);
        }

        public void row(Map<?, ?> data) {
            super.row(data, true);
        }

        public void table(List<? extends Map<?, ?>> rows) {
            super.table(rows, true, null);
        }

        public void table(Map<?, ?> row) {
            super.table(row, true);
        }

        // --- Added for table with title support ---
        public void table(List<? extends Map<?, ?>> rows, String title) {
            super.table(rows, title);
        }

        public void table(Map<?, ?> row, String title) {
            super.table(row, title);
        }
    }

    public static class Info extends LogActions {
        public Info() {
            super("INFO");
        }

        public void log(String message) {
            logMessage(getColors().infoStyle(), "INFO", message, false);
        }

        public void log(String heading, Map<String, ?> fields) {
            tree(heading, fields, getColors().infoStyle(), "INFO", false);
        }

        public void log(String heading, Object... pairs) {
            log(heading, CustomLogger.fields(pairs));
        }

        // --- Added for table with title support ---
        public void table(List<? extends Map<?, ?>> rows, String title) {
            super.table(rows, title);
        }

        public void table(Map<?, ?> row, String title) {
            super.table(row, title);
        }
    }

    public static class Warn extends LogActions {
        public Warn() {
            super("WARN");
        }

        public void log(String message) {
            logMessage(getColors().warnStyle(), "WARN", message, false);
        }

        public void log(String heading, Map<String, ?> fields) {
            tree(heading, fields, getColors().warnStyle(), "WARN", false);
        }

        public void log(String heading, Object... pairs) {
            log(heading, CustomLogger.fields(pairs));
        }
    }

    public static class Error extends LogActions {
        public Error() {
            super("ERROR");
        }

        public void log(String message) {
            logMessage(getColors().errorStyle(), "ERROR", message, false);
        }

        public void log(String heading, Map<String, ?> fields) {
            tree(heading, fields, getColors().errorStyle(), "ERROR", false);
        }

        public void log(String heading, Object... pairs) {
            log(heading, CustomLogger.fields(pairs));
        }
    }

    public static class LogActions {
        protected final String logLevel;

        public LogActions(String logLevel) {
            this.logLevel = logLevel;
        }

        public void click(String message) {
            logMessage(getColors().clickStyle(), "CLICK", message, false);
        }

        public void checkbox(String message) {
            logMessage(getColors().clickStyle(), "CHECKBOX", message, false);
        }

        public void text(String message) {
            logMessage(getColors().actionStyle(), "TEXT", message, false);
        }

        public void validation(String message) {
            logMessage(getColors().warnStyle(), "VALIDATION ⁉", message, false);
        }

        public void fallback(String message) {
            logMessage(getColors().warnStyle(), "FALLBACK ↩", message, false);
        }

        public void wait(String message) {
            logMessage(getColors().waitStyle(), "WAIT", message, false);
        }

        public void input(String message) {
            logMessage(getColors().actionStyle(), "INPUT", message, false);
        }

        public void table(String message) {
            logMessage(getColors().tableStyle(), "TABLE", message, false);
        }

        public void grid(String message) {
            logMessage(getColors().tableStyle(), "GRID", message, false);
        }

        public void complete(String message) {
            logMessage(getColors().successStyle(), "COMPLETE✅", message, false);
        }

        public void success(String message) {
            logMessage(getColors().successStyle(), "SUCCESS✅", message, false);
        }

        public void upload(String message) {
            logMessage(getColors().actionStyle(), "UPLOAD⬆", message, false);
        }

        public void dropdown(String message) {
            logMessage(getColors().actionStyle(), "DROPDOWN", message, false);
        }

        public void frame(String message) {
            logMessage(getColors().actionStyle(), "FRAME", message, false);
        }

        public void tab(String message) {
            logMessage(getColors().actionStyle(), "TAB", message, false);
        }

        public void breadcrumb(String message) {
            logMessage(getColors().actionStyle(), "BREADCRUMB", message, false);
        }

        public void search(String message) {
            logMessage(getColors().actionStyle(), "SEARCHED", message, false);
        }

        public void error(String message) {
            logMessage(getColors().errorStyle(), "ERROR❌", message, false);
        }

        public void result(String message) {
            logMessage(getColors().actionStyle(), "RESULT", message, false);
        }

        public void timeout(String message) {
            logMessage(getColors().errorStyle(), "TIMEOUT❌", message, false);
        }

        public void failed(String message) {
            logMessage(getColors().errorStyle(), "FAILED❌", message, false);
        }

        public void toggle(String message) {
            logMessage(getColors().actionStyle(), "TOGGLE", message, false);
        }

        public void skip(String message) {
            logMessage(getColors().warnStyle(), "SKIP❕", message, false);
        }

        public void resolved(String message) {
            logMessage(getColors().actionStyle(), "RESOLVED", message, false);
        }

        // ---- Enhanced Object Logging ----
        public void log(Object obj) {
            if (obj == null) {
                logMessage(getLevelColor(), "LOG", "null", false);
            } else if (obj instanceof Map) {
                table((Map<?, ?>) obj, false);
            } else if (obj instanceof List) {
                logList((List<?>) obj, false);
            } else if (obj.getClass().isArray()) {
                logList(java.util.Arrays.asList((Object[]) obj), false);
            } else {
                logMessage(getLevelColor(), "LOG", obj.toString(), false);
            }
        }

        public void log(String heading, Object obj) {
            if (obj == null) {
                logMessage(getLevelColor(), "LOG", heading + ": null", false);
            } else if (obj instanceof Map) {
                logMessage(getLevelColor(), "LOG", heading + ":", false);
                table((Map<?, ?>) obj, false);
            } else if (obj instanceof List) {
                logMessage(getLevelColor(), "LOG", heading + ":", false);
                logList((List<?>) obj, false);
            } else if (obj.getClass().isArray()) {
                logMessage(getLevelColor(), "LOG", heading + ":", false);
                logList(java.util.Arrays.asList((Object[]) obj), false);
            } else {
                logMessage(getLevelColor(), "LOG", heading + ": " + obj.toString(), false);
            }
        }

        public void log(List<?> list) {
            logList(list, false);
        }

        public void log(String heading, Object... pairs) {
            tree(heading, CustomLogger.fields(pairs), getLevelColor(), "LOG", false);
        }

        private void logList(List<?> list, boolean invert) {
            if (list == null || list.isEmpty()) {
                logMessage(getLevelColor(), "LOG", "(empty list)", invert);
                return;
            }
            int i = 0;
            for (Object item : list) {
                String prefix = "  [" + (i++) + "] ";
                if (item instanceof Map) {
                    logMessage(getLevelColor(), "LOG", prefix, invert);
                    table((Map<?, ?>) item, invert);
                } else {
                    logMessage(getLevelColor(), "LOG", prefix + String.valueOf(item), invert);
                }
            }
        }

        private static String center(String s, int width) {
            String plain = s.replaceAll("\\u001B\\[[;\\d]*m", "");
            int len = plain.length();
            if (len >= width) return s.substring(0, width);
            int left = (width - len) / 2;
            int right = width - len - left;
            return " ".repeat(left) + s + " ".repeat(right);
        }

        public void table(List<? extends Map<?, ?>> rows, boolean invert, String title) {
            if (rows == null || rows.isEmpty()) {
                logMessage(getColors().tableStyle(), "TABLE", "No rows to display.", invert);
                return;
            }
            LinkedHashMap<String, Integer> colWidths = new LinkedHashMap<>();
            for (Map<?, ?> row : rows) {
                for (Object key : row.keySet()) {
                    String col = String.valueOf(key);
                    String val = truncateCell(String.valueOf(row.get(key)));
                    colWidths.put(col, Math.max(colWidths.getOrDefault(col, col.length()), Math.max(col.length(), val.length())));
                }
            }
            List<String> headers = new java.util.ArrayList<>(colWidths.keySet());
            StringBuilder sb = new StringBuilder();
            String color = getColors().tableStyle();
            String reset = getColors().reset();
            if (invert) color = BG_BLACK + fgFromBg(getColors().tableStyle());

            String hBorder = "+" + headers.stream()
                    .map(h -> "-".repeat(colWidths.get(h) + 2))
                    .reduce((a, b) -> a + "+" + b).orElse("") + "+";

            String headerRow = "|" + headers.stream()
                    .map(h -> " " + truncateCell(h) + " ".repeat(colWidths.get(h) - h.length()) + " ")
                    .reduce((a, b) -> a + "|" + b).orElse("") + "|";

            sb.append(hBorder).append("\n");

            if (title != null && !title.isEmpty()) {
                String bold = ANSI_BOLD;
                String resetBold = ANSI_RESET;
                int insideWidth = headerRow.length() - 2;
                String centeredTitle = center(" " + title + " ", insideWidth);
                sb.append("|").append(bold).append(centeredTitle).append(resetBold).append("|\n");
                sb.append(hBorder).append("\n");
            }

            sb.append(headerRow).append("\n");
            sb.append(hBorder).append("\n");

            for (Map<?, ?> row : rows) {
                String rowStr = "|" + headers.stream().map(h -> {
                    Object v = null;
                    for (Object key : row.keySet()) {
                        if (String.valueOf(key).equals(h)) {
                            v = row.get(key);
                            break;
                        }
                    }
                    if (v == null) v = "";
                    String s = truncateCell(String.valueOf(v));
                    return " " + s + " ".repeat(colWidths.get(h) - s.length()) + " ";
                }).reduce((a, b) -> a + "|" + b).orElse("") + "|";
                sb.append(rowStr).append("\n");
            }
            sb.append(hBorder);
            logMessage(color, "TABLE", "\n" + sb + reset, invert);
        }

        public void table(Map<?, ?> row, boolean invert) {
            if (row == null || row.isEmpty()) {
                logMessage(getColors().tableStyle(), "TABLE", "No data to display.", invert);
                return;
            }
            java.util.List<Map<?, ?>> rows = java.util.Collections.singletonList(row);
            table(rows, invert, null);
        }

        public void table(Map<?, ?> row, boolean invert, String title) {
            if (row == null || row.isEmpty()) {
                logMessage(getColors().tableStyle(), "TABLE", "No data to display.", invert);
                return;
            }
            java.util.List<Map<?, ?>> rows = java.util.Collections.singletonList(row);
            table(rows, invert, title);
        }

        public void table(List<? extends Map<?, ?>> rows) {
            table(rows, false, null);
        }

        public void table(Map<?, ?> row) {
            table(row, false);
        }

        public void table(List<? extends Map<?, ?>> rows, String title) {
            table(rows, false, title);
        }

        public void table(Map<?, ?> row, String title) {
            table(row, false, title);
        }

        public void row(Map<?, ?> data, boolean invert) {
            if (data == null || data.isEmpty()) {
                logMessage(getColors().tableStyle(), "ROW", "(empty)", invert);
                return;
            }
            int maxKeyLen = data.keySet().stream().map(k -> String.valueOf(k).length()).max(Integer::compareTo).orElse(0);
            String color = getColors().tableStyle();
            String reset = getColors().reset();
            if (invert) color = BG_BLACK + fgFromBg(getColors().tableStyle());
            for (Map.Entry<?, ?> entry : data.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String val = truncateCell(String.valueOf(entry.getValue()));
                String line = String.format("%-" + maxKeyLen + "s : %s", key, val);
                logMessage(color, "ROW", line + reset, invert);
            }
        }

        public void row(Map<?, ?> data) {
            row(data, false);
        }

        public void tree(String heading, Map<String, ?> fields) {
            tree(heading, fields, getLevelColor(), "LOG", false);
        }

        public void tree(String heading, Map<String, ?> fields, String actionColor, String actionLabel, boolean invert) {
            if (heading == null || fields == null) return;
            String label = (actionLabel == null || actionLabel.isEmpty()) ? "LOG" : actionLabel;
            logMessage(actionColor, label, heading, invert);
            int size = fields.size();
            int i = 0;
            for (Map.Entry<String, ?> entry : fields.entrySet()) {
                i++;
                String prefix = (i < size) ? "          ├─ " : "          └─ ";
                Object valueObj = entry.getValue();
                String value;
                if (valueObj != null && valueObj.getClass().isArray()) {
                    if (valueObj instanceof Object[])
                        value = java.util.Arrays.deepToString((Object[]) valueObj);
                    else if (valueObj instanceof int[])
                        value = java.util.Arrays.toString((int[]) valueObj);
                    else if (valueObj instanceof long[])
                        value = java.util.Arrays.toString((long[]) valueObj);
                    else if (valueObj instanceof double[])
                        value = java.util.Arrays.toString((double[]) valueObj);
                    else if (valueObj instanceof boolean[])
                        value = java.util.Arrays.toString((boolean[]) valueObj);
                    else if (valueObj instanceof char[])
                        value = java.util.Arrays.toString((char[]) valueObj);
                    else if (valueObj instanceof float[])
                        value = java.util.Arrays.toString((float[]) valueObj);
                    else if (valueObj instanceof short[])
                        value = java.util.Arrays.toString((short[]) valueObj);
                    else if (valueObj instanceof byte[])
                        value = java.util.Arrays.toString((byte[]) valueObj);
                    else
                        value = String.valueOf(valueObj);
                } else {
                    value = String.valueOf(valueObj);
                }
                logMessage(actionColor, label, prefix + String.format("%-12s: %s", entry.getKey(), value), invert);
            }
        }

        public void tree(String heading, Object... pairs) {
            tree(heading, CustomLogger.fields(pairs), getLevelColor(), "LOG", false);
        }

        public void resolved(String heading, Map<String, ?> fields) {
            tree(heading, fields, getColors().actionStyle(), "RESOLVED", false);
        }

        public void resolved(String heading, Object... pairs) {
            tree(heading, CustomLogger.fields(pairs), getColors().actionStyle(), "RESOLVED", false);
        }

        protected void logMultiline(String actionColor, String actionLabel, String message, boolean invert, boolean showCaller) {
            if (message == null) message = "null";

            // build white timestamp chip (at the beginning)
            String ts = LocalDateTime.now().format(TS_FMT);
            String tsText = "[" + ts + "]";
            String tsChip = isAnsiEnabled.get()
                    ? BG_BRIGHT_GREY + FG_BLACK + " " + tsText + " " + ANSI_RESET + " "
                    : tsText + " ";

            // callee ← caller chips go at the END (first line only)
            String classAndMethodSuffix = showCaller ? getCallingClassAndMethodColored() : "";

            String stampColor = actionColor;
            String msgColor = (!invert) ? actionColor : BG_BLACK + fgFromBg(actionColor);

            String[] lines = message.split("\\R", -1);
            for (int i = 0; i < lines.length; i++) {
                String start = isAnsiEnabled.get()
                        ? tsChip + stampColor + "[" + actionLabel + "] " + getColors().reset()
                        : tsChip + "[" + actionLabel + "] ";

                String core = isAnsiEnabled.get()
                        ? msgColor + lines[i] + getColors().reset()
                        : lines[i];

                // only put class/method chips on the first line, at the end
                String end = (i == 0 && !classAndMethodSuffix.isEmpty())
                        ? " " + classAndMethodSuffix
                        : "";

                String out = start + core + end;

                switch (logLevel) {
                    case "ERROR" -> getSafeLogger().error(out);
                    case "WARN" -> getSafeLogger().warn(out);
                    case "INFO" -> getSafeLogger().info(out);
                    default -> getSafeLogger().debug(out);
                }
            }
        }


        protected void logMessage(String actionColor, String actionLabel, String message, boolean invert) {
            boolean showCaller = CustomLogger.isDebugEnabled();
            logMultiline(actionColor, actionLabel, message, invert, showCaller);
        }

        protected void logMessage(String actionColor, String actionLabel, String message) {
            boolean showCaller = CustomLogger.isDebugEnabled();
            logMultiline(actionColor, actionLabel, message, false, showCaller);
        }

        // ---------- Stricter caller/callee resolution with configurable filtering ----------
        private static String simpleClass(String fqcn) {
            int i = (fqcn == null) ? -1 : fqcn.lastIndexOf('.');
            return (i >= 0) ? fqcn.substring(i + 1) : (fqcn == null ? "" : fqcn);
        }

        private static String prettyMethod(String methodName) {
            if ("<init>".equals(methodName)) return "(constructor)";
            if ("<clinit>".equals(methodName)) return "(static init)";
            return methodName;
        }

        private static boolean filteredOut(String className, String methodName) {
            if (methodName != null) {
                for (String p : SUPPRESS_METHOD_PREFIXES) if (methodName.startsWith(p)) return true;
            }
            if (className != null) {
                // include-only takes precedence: if set and not matched -> filtered out
                if (!INCLUDE_ONLY_PREFIXES.isEmpty()) {
                    boolean ok = false;
                    for (String inc : INCLUDE_ONLY_PREFIXES) {
                        if (className.startsWith(inc)) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) return true;
                }
                for (String s : SUPPRESS_CONTAINS) {
                    if (className.contains(s) || className.startsWith(s)) return true;
                }
            }
            return false;
        }


        protected String getCallingClassAndMethodColored() {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();

            int calleeIdx = -1, callerIdx = -1;
            for (int i = 3; i < st.length; i++) {
                String cn = st[i].getClassName();
                String mn = st[i].getMethodName();
                if (filteredOut(cn, mn)) continue;

                if (calleeIdx == -1) {             // where log(...) was invoked
                    calleeIdx = i;
                    continue;
                }
                // avoid X.x ← X.x
                if (st[i].getClassName().equals(st[calleeIdx].getClassName())
                        && st[i].getMethodName().equals(st[calleeIdx].getMethodName())) {
                    continue;
                }
                callerIdx = i;
                break;
            }

            if (calleeIdx == -1) return "";

            ThemeColors c = getColors();

            // pretty names for ctor / clinit
            String calleeClass = st[calleeIdx].getClassName();
            String calleeMethod = st[calleeIdx].getMethodName();
            boolean calleeCtor = "<init>".equals(calleeMethod);
            boolean calleeClinit = "<clinit>".equals(calleeMethod);
            String calleePretty = calleeCtor ? "(constructor)"
                    : calleeClinit ? "(static init)"
                    : calleeMethod;

            String leftChip =
                    c.classNameStyle() + simpleClass(calleeClass) +
                            c.methodNameStyle() + "." + calleePretty + " " + ANSI_RESET;

            if (callerIdx == -1) return leftChip;

            String callerClass = st[callerIdx].getClassName();
            String callerMethod = st[callerIdx].getMethodName();
            boolean callerCtor = "<init>".equals(callerMethod);
            boolean callerClinit = "<clinit>".equals(callerMethod);
            String callerPretty = callerCtor ? "(constructor)"
                    : callerClinit ? "(static init)"
                    : callerMethod;

            String rightChip =
                    c.classNameStyle() + simpleClass(callerClass) +
                            c.methodNameStyle() + "." + callerPretty + " " + ANSI_RESET;

            return leftChip + BG_GREY_100 + "\u2190 " + rightChip;  // callee ← caller //temp BG fix
        }


        // -----------------------------------------------------------------------------------

        private static String stripAnsi(String str) {
            return str == null ? "" : str.replaceAll("\\u001B\\[[;\\d]*m", "");
        }

        protected String getLevelColor() {
            return switch (logLevel) {
                case "ERROR" -> getColors().errorStyle();
                case "WARN" -> getColors().warnStyle();
                case "INFO" -> getColors().infoStyle();
                default -> BG_BLACK + fgFromBg(getColors().infoStyle());
            };
        }
    }

    private static String fgFromBg(String style) {
        if (style == null) return FG_BRIGHT_WHITE;
        java.util.regex.Matcher fgMatcher = java.util.regex.Pattern
                .compile("(\u001B\\[3\\d{1,2}(;\\d{1,2})?m)")
                .matcher(style);
        if (fgMatcher.find()) {
            return fgMatcher.group();
        }
        java.util.regex.Matcher m8 = java.util.regex.Pattern
                .compile("(\u001B\\[4)(\\d)(m)")
                .matcher(style);
        if (m8.find()) {
            String fgCandidate = "\u001B[3" + m8.group(2) + "m";
            return fgCandidate.equals(FG_BLACK) ? FG_BRIGHT_WHITE : fgCandidate;
        }
        java.util.regex.Matcher m256 = java.util.regex.Pattern
                .compile("(\u001B\\[48;5;)(\\d+)(m)")
                .matcher(style);
        if (m256.find()) {
            return "\u001B[38;5;" + m256.group(2) + "m";
        }
        return FG_BRIGHT_WHITE;
    }

    protected static Logger getSafeLogger() {
        return (log != null) ? log : Logger.getLogger(CustomLogger.class);
    }
}
