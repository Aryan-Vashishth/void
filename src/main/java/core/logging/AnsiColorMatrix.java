package core.logging;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Prints every possible Foreground × Background combination defined in
 * {@link AnsiColors} so you can visually preview which pairs are readable
 * in your terminal.
 *
 * <p>It uses reflection to discover every {@code public static final String}
 * constant whose name begins with {@code FG_} or {@code BG_} (including the
 * {@code RGB_FG_} / {@code RGB_BG_} variants).</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 *   mvn -q exec:java -Dexec.mainClass=core.logging.AnsiColorMatrix
 *   // or simply run the main method from your IDE
 * }</pre>
 *
 * <p>Optional CLI flags:</p>
 * <ul>
 *   <li>{@code --sample "TEXT"}  — text used for each cell (default: {@code  Sample 12345 }).</li>
 *   <li>{@code --bold}           — apply bold to every cell.</li>
 *   <li>{@code --grid}           — print as a compact grid (FG rows × BG columns)
 *                                   instead of one line per pair.</li>
 * </ul>
 */
public final class AnsiColorMatrix {

    private AnsiColorMatrix() {}

    public static void main(String[] args) throws Exception {
        String sample = " Sample 12345 ";
        boolean bold = false;
        boolean grid = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--sample": if (i + 1 < args.length) sample = args[++i]; break;
                case "--bold":   bold = true; break;
                case "--grid":   grid = true; break;
                case "-h":
                case "--help":
                    System.out.println("Usage: AnsiColorMatrix [--sample TEXT] [--bold] [--grid]");
                    return;
                default: /* ignore */ break;
            }
        }

        List<NamedCode> fgs = collect("FG_");
        List<NamedCode> bgs = collect("BG_");

        System.out.println(AnsiColors.BOLD + "Foreground constants : " + fgs.size()
                + "   Background constants : " + bgs.size()
                + "   Total combinations : " + (fgs.size() * bgs.size())
                + AnsiColors.RESET);
        System.out.println();

        String style = bold ? AnsiColors.BOLD : "";

        if (grid) {
            printGrid(fgs, bgs, sample, style);
        } else {
            printList(fgs, bgs, sample, style);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Output modes
    // ─────────────────────────────────────────────────────────────────────────

    private static void printList(List<NamedCode> fgs, List<NamedCode> bgs,
                                  String sample, String style) {
        int fgw = maxNameWidth(fgs);
        int bgw = maxNameWidth(bgs);
        for (NamedCode fg : fgs) {
            for (NamedCode bg : bgs) {
                String cell = bg.code + fg.code + style + sample + AnsiColors.RESET;
                System.out.printf("%-" + fgw + "s  on  %-" + bgw + "s  %s%n",
                        fg.name, bg.name, cell);
            }
            System.out.println();
        }
    }

    private static void printGrid(List<NamedCode> fgs, List<NamedCode> bgs,
                                  String sample, String style) {
        int fgw = maxNameWidth(fgs);
        // Header row with abbreviated BG indices
        StringBuilder header = new StringBuilder();
        header.append(pad("FG \\ BG", fgw)).append(" │ ");
        for (int i = 0; i < bgs.size(); i++) header.append(String.format("%-4d", i));
        System.out.println(header);
        System.out.println(repeat('─', fgw + 3 + bgs.size() * 4));

        for (NamedCode fg : fgs) {
            StringBuilder row = new StringBuilder();
            row.append(pad(fg.name, fgw)).append(" │ ");
            for (NamedCode bg : bgs) {
                row.append(bg.code).append(fg.code).append(style)
                   .append(" Aa ").append(AnsiColors.RESET);
            }
            System.out.println(row);
        }

        System.out.println();
        System.out.println(AnsiColors.BOLD + "Background legend:" + AnsiColors.RESET);
        for (int i = 0; i < bgs.size(); i++) {
            System.out.printf("  %2d = %s%n", i, bgs.get(i).name);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reflection helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static List<NamedCode> collect(String kind) throws IllegalAccessException {
        List<NamedCode> out = new ArrayList<>();
        for (Field f : AnsiColors.class.getDeclaredFields()) {
            int m = f.getModifiers();
            if (!Modifier.isPublic(m) || !Modifier.isStatic(m) || !Modifier.isFinal(m)) continue;
            if (f.getType() != String.class) continue;

            String n = f.getName();
            boolean isFg = n.startsWith("FG_") || n.startsWith("RGB_FG_");
            boolean isBg = n.startsWith("BG_") || n.startsWith("RGB_BG_");
            if ("FG_".equals(kind) && !isFg) continue;
            if ("BG_".equals(kind) && !isBg) continue;

            String value = (String) f.get(null);
            if (value == null || value.isEmpty()) continue;
            out.add(new NamedCode(n, value));
        }
        return out;
    }

    private static int maxNameWidth(List<NamedCode> list) {
        int w = 0;
        for (NamedCode c : list) if (c.name.length() > w) w = c.name.length();
        return w;
    }

    private static String pad(String s, int w) {
        if (s.length() >= w) return s;
        StringBuilder sb = new StringBuilder(w);
        sb.append(s);
        for (int i = s.length(); i < w; i++) sb.append(' ');
        return sb.toString();
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private static final class NamedCode {
        final String name;
        final String code;
        NamedCode(String name, String code) { this.name = name; this.code = code; }
    }
}

