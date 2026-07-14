package elements.api;

/**
 * Marker interface for enum groups whose constants share a single locator template
 * and differ only by a runtime argument derived automatically from the constant name.
 *
 * <h3>Key format</h3>
 * <p>{@code PageName.EnumName} — no constant suffix. One key covers the whole group.</p>
 *
 * <h3>Automatic argument</h3>
 * <p>The default {@link #getArgs()} returns the constant name word-transformed:
 * {@code MANAGE_USERS} → {@code ["Manage Users"]}. The locator template must contain
 * exactly one {@code %s} token; multi-token templates require an explicit {@code .with()}
 * override.</p>
 *
 *
 * <h3>Progression</h3>
 * <ul>
 *   <li>Use {@code LocatorFamily} when all constant names have predictable, word-style labels.</li>
 *   <li>Use {@code AdvancedLocatorFamily} when a few constants require exceptional values
 *       (acronyms, symbols, punctuation).</li>
 *   <li>Use {@code SwitchLocatorFamily} when all constants require custom values and a
 *       centralised, compiler-exhaustive mapping is preferred.</li>
 * </ul>
 */
public interface LocatorFamily extends Element {

    /** Returns the family locator key: {@code PageName.EnumName} (no constant suffix). */
    @Override
    default String getPrimaryLocator() {
        Enum<?> e = (Enum<?>) this;
        Class<?> ec = e.getDeclaringClass();
        Class<?> pc = ec.getEnclosingClass();
        return pc != null
                ? pc.getSimpleName() + "." + ec.getSimpleName()
                : ec.getSimpleName();
    }

    /**
     * Derives the runtime locator argument from the constant name using the same
     * word-transform as {@link Element#getDisplayText()}.
     * {@code MANAGE_USERS} → {@code ["Manage Users"]}.
     */
    @Override
    default Object[] getArgs() {
        String[] tokens = ((Enum<?>) this).name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) sb.append(token.substring(1).toLowerCase());
        }
        return new Object[]{sb.toString()};
    }

    /**
     * Family elements use the deterministic repository convention and require no
     * explicit filename. Returns {@code null} by default.
     */
    @Override
    default String getExternalFileName() { return null; }

}
