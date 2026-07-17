package elements.api;

import elements.meta.ElementRole;

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
 * exactly one {@code %s} token. Multi-token templates require an explicit {@link #getArgs()}
 * override on the constant.</p>
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

    /**
     * Routes every capability role through the family key so no per-enum override
     * of {@code getInputLocator()} / {@code getTriggerLocator()} is ever needed.
     */
    @Override
    default String locatorKeyForRole(ElementRole role) {
        return getPrimaryLocator();
    }

    /**
     * Signals the sync tool to emit one shared template key for this enum class
     * rather than one key per constant.
     */
    @Override
    default String templateFamilyKey() {
        return getPrimaryLocator();
    }

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
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) sb.append(token.substring(1).toLowerCase());
        }
        return new Object[]{sb.toString()};
    }

    /**
     * Returns the conventional {@code locators.properties} path for this element's enclosing
     * page class.
     *
     * <p>LocatorFamily keys are flat 2-segment strings ({@code PageName.EnumName}). The
     * resolution chain selects a source based on file extension; {@code .properties} selects
     * {@code PropertiesLocatorSource}, which handles flat key lookup natively. JSON sources
     * navigate nested object paths and cannot resolve a 2-segment family key.</p>
     */
    @Override
    default String getExternalFileName() {
        Enum<?> e = (Enum<?>) this;
        Class<?> ec = e.getDeclaringClass();
        Class<?> pc = ec.getEnclosingClass();
        Class<?> target = pc != null ? pc : ec;
        return target.getName().replace('.', '/') + "/locators.properties";
    }

}
