package core.resolvers.locator.template;

import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Value object representing a raw locator template string with a configurable
 * formatting policy.
 *
 * <p>Consolidates the two divergent template formatters that previously lived in
 * {@code ElementLocatorResolverV1.resolveLocatorTemplate} (which silently padded the
 * last argument) and {@code LocatorResolverV1.resolveLocatorTemplate} (which throws on
 * too few arguments and supports indexed {@code %1$s} placeholders).</p>
 *
 * <p>Choose behaviour via the {@link Policy} enum.</p>
 *
 * @param raw    the template string (may be {@code null})
 * @param policy the formatting policy
 */
public record LocatorTemplate(String raw, Policy policy) {

    /** Formatting policy. */
    public enum Policy {
        /**
         * Strict, indexed-aware mode (the {@code LocatorResolverV1} contract):
         * <ul>
         *   <li>Recognises {@code %s} and {@code %1$s} (case-sensitive lower-{@code s}).</li>
         *   <li>If no {@code %s} is found, the template is returned unchanged.</li>
         *   <li>Too few arguments → {@link IllegalStateException} wrapping the format error.</li>
         * </ul>
         */
        STRICT,
        /**
         * Padding mode (the legacy {@code ElementLocatorResolverV1} contract):
         * <ul>
         *   <li>Counts {@code %s} and {@code %S} case-insensitively.</li>
         *   <li>If fewer args than placeholders, the last arg is repeated to fill the gap.</li>
         *   <li>Empty / no placeholders → returned unchanged.</li>
         * </ul>
         */
        PAD_LAST
    }

    /** Strict, indexed {@code %s}/{@code %n$s} pattern (matches {@code LocatorResolverV1}). */
    private static final Pattern STRICT_PLACEHOLDER = Pattern.compile("%(\\d+\\$)?s");

    public static LocatorTemplate strict(String raw)  { return new LocatorTemplate(raw, Policy.STRICT); }
    public static LocatorTemplate padded(String raw)  { return new LocatorTemplate(raw, Policy.PAD_LAST); }

    /**
     * Count placeholders according to the active policy.
     * For {@link Policy#PAD_LAST} this is case-insensitive ({@code %s} and {@code %S}).
     * For {@link Policy#STRICT} only lowercase {@code %s} (and indexed variants) is counted.
     */
    public int placeholderCount() {
        if (raw == null || raw.isEmpty()) return 0;
        if (policy == Policy.PAD_LAST) return countCaseInsensitive(raw);
        Matcher m = STRICT_PLACEHOLDER.matcher(raw);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    /** Whether the template has at least one placeholder under the active policy. */
    public boolean hasPlaceholders() { return placeholderCount() > 0; }

    /** Format the template using the active policy. Returns {@code null} when {@link #raw} is {@code null}. */
    public String format(Object... args) {
        if (raw == null) return null;
        if (args == null) args = new Object[0];

        return switch (policy) {
            case STRICT   -> formatStrict(args);
            case PAD_LAST -> formatPadded(args);
        };
    }

    // ---- internal -----------------------------------------------------------

    private String formatStrict(Object[] args) {
        if (!STRICT_PLACEHOLDER.matcher(raw).find()) return raw;
        try {
            return String.format(Locale.ROOT, raw, args);
        } catch (IllegalFormatException ex) {
            throw new IllegalStateException(
                    "Locator template format error. template=\"" + raw + "\", args(len=" + args.length + ")", ex);
        }
    }

    private String formatPadded(Object[] args) {
        int n = countCaseInsensitive(raw);
        if (n == 0) return raw;
        if (n > args.length) {
            Object[] padded = new Object[n];
            for (int i = 0; i < n; i++) {
                padded[i] = (i < args.length) ? args[i] : (args.length == 0 ? null : args[args.length - 1]);
            }
            args = padded;
        }
        return String.format(raw, args);
    }

    private static int countCaseInsensitive(String template) {
        int count = 0, idx = 0;
        String lower = template.toLowerCase(Locale.ROOT);
        while ((idx = lower.indexOf("%s", idx)) != -1) {
            count++;
            idx += 2;
        }
        return count;
    }
}

