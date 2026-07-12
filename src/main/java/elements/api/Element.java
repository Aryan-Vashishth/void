package elements.api;
import elements.meta.ElementRole;
import javax.annotation.Nullable;

/**
 * Core abstraction for every UI element descriptor in the framework.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Expose an external properties filename (if locators are stored in property bundles).</li>
 *   <li>Provide a <b>primary</b> locator key and optional <b>secondary</b> locator key for fallback.</li>
 *   <li>Supply optional dynamic arguments (e.g., row index, label text) for templated locator strings.</li>
 *   <li>Offer a human friendly display text for logging/reporting.</li>
 *   <li>Publish an ordered map of locator roles via {@link #getAllLocatorRoles()} used by resolution pipelines.</li>
 * </ul>
 * </p>
 * <p>
 * Role-based locator exposure: instead of hard‑coding String identifiers throughout the codebase,
 * {@link ElementRole} provides an enum with stable names (e.g. PRIMARY, SECONDARY, TRIGGER, INPUT, LIST, etc.).
 * This enables compile‑time discoverability and consistent fallback sequencing.
 * </p>
 * <p>
 * Locator roles are exposed via {@link #getAllLocatorRoles()}, returning a map keyed by
 * {@link ElementRole} for type-safe role access.
 * </p>
 */
public interface Element {
    /** @return properties file name containing locator key/value pairs, or null if none. */
    @Nullable
    String getExternalFileName();

    /**
     * Returns the namespaced locator key for this element.
     * <p>The default derives the key as {@code PageName.GroupName.CONSTANT_NAME} from
     * the Java type hierarchy, matching the key format used in generated properties files.
     * Capability interfaces and elements with custom keys override this as needed.</p>
     */
    default String getPrimaryLocator() {
        Enum<?> e = (Enum<?>) this;
        Class<?> enumClass = e.getDeclaringClass();
        Class<?> pageClass = enumClass.getEnclosingClass();
        if (pageClass != null) {
            return pageClass.getSimpleName() + "." + enumClass.getSimpleName() + "." + e.name();
        }
        return enumClass.getSimpleName() + "." + e.name();
    }

    /** @return secondary fallback locator key, or null if not applicable. */
    default String getSecondaryLocator(){ return null; }

    /** Shared empty-args constant — signals that this element requires no locator arguments. */
    Object[] NO_ARGS = new Object[0];

    /** @return dynamic arguments used to format locator templates containing %s tokens. */
    default Object[] getArgs() { return NO_ARGS; }

    /**
     * Returns {@code overrides} when it is non-null and non-empty; otherwise returns {@link #getArgs()}.
     * <p>Centralises the "override args take precedence over the element's own args" rule that used to
     * be repeated as a ternary in every resolver call site.</p>
     */
    default Object[] effectiveArgs(Object... overrides) {
        return (overrides != null && overrides.length > 0) ? overrides : getArgs();
    }

    /**
     * Returns a human-readable label derived from the enum constant name.
     * <p>Transformation: {@code SAVE_AS_DRAFT} → {@code Save As Draft}.
     * Tokens are split on underscores; each token is capitalised with the rest lowercased.
     * Capability interfaces override this to incorporate dynamic args when present.</p>
     */
    default String getDisplayText() {
        String[] tokens = ((Enum<?>) this).name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) sb.append(token.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /**
     * Builds an ordered map of {@link ElementRole} to locator key strings.
     * Only non-blank locators are included; order reflects fallback priority.
     */
    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        String primary = getPrimaryLocator();
        if(primary!=null && !primary.isBlank()) roles.put(ElementRole.PRIMARY, primary);
        String secondary = getSecondaryLocator();
        if(secondary!=null && !secondary.isBlank()) roles.put(ElementRole.SECONDARY, secondary);
        return roles;
    }

}
