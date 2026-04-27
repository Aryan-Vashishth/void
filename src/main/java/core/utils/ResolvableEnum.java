package core.utils;

import java.util.Map;

/**
 * Mixin for enums that need standardized name→label resolution.
 * <p>
 * Used by {@link EnumResolver} to match user-facing text
 * (e.g., step definition parameters) to enum constants.
 * </p>
 * <p><b>This is NOT a locator interface.</b> Enums that also need locator
 * behaviour should additionally implement the appropriate {@link elements.api.Element}
 * sub-interface ({@code Clickable}, {@code TextInputField}, etc.).</p>
 * <p>Default label formatting: transforms ENUM_CONSTANT → "Enum Constant".</p>
 */
public interface ResolvableEnum {

    /** @return raw enum constant name. */
    default String getName() {
        return ((Enum<?>) this).name();
    }

    /** @return human-friendly label (Title Case, underscores removed). */
    default String getLabel() {
        String raw = getName();
        String[] words = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) sb.append(w.substring(1).toLowerCase());
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    /** @return lowerCamel variant (ENUM_CONSTANT → enumConstant). */
    default String toLowerCamel() {
        String raw = getName().toLowerCase();
        String[] parts = raw.split("_");
        if (parts.length == 0) return raw;
        StringBuilder b = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return b.toString();
    }

    /** @return immutable Map.Entry of name→label. */
    default Map.Entry<String, String> toEntry() {
        return Map.entry(getName(), getLabel());
    }
}

