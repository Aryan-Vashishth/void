package elements.api;
import elements.meta.ElementRole;
import io.reactivex.rxjava3.annotations.Nullable;

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
 * The legacy {@link #getAllLocators()} method is retained (and deprecated) to ease incremental migration.
 * New code should prefer {@link #getAllLocatorRoles()}.
 * </p>
 */
public interface Element {
    /** @return properties file name containing locator key/value pairs, or null if none. */
    @Nullable
    String getExternalFileName();

    /** @return primary locator key (e.g. for By.xpath lookup). Must not be blank for actionable elements. */
    String getPrimaryLocator();

    /** @return secondary fallback locator key, or null if not applicable. */
    default String getSecondaryLocator(){ return null; }

    /** @return dynamic arguments used to format locator templates containing %s tokens. */
    Object[] getArgs();

    /**
     * Returns {@code overrides} when it is non-null and non-empty; otherwise returns {@link #getArgs()}.
     * <p>Centralises the "override args take precedence over the element's own args" rule that used to
     * be repeated as a ternary in every resolver call site.</p>
     */
    default Object[] effectiveArgs(Object... overrides) {
        return (overrides != null && overrides.length > 0) ? overrides : getArgs();
    }

    /** @return human friendly label for logs; default uses first arg or empty string. */
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : "";
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

    /**
     * LEGACY adapter: returns a map keyed by {@link ElementRole#name()}. Prefer {@link #getAllLocatorRoles()}.
     * @deprecated Use {@link #getAllLocatorRoles()} for type-safe role access.
     */
    @Deprecated
    default java.util.Map<String,String> getAllLocators(){
        java.util.Map<String,String> legacy = new java.util.LinkedHashMap<>();
        getAllLocatorRoles().forEach((role,val)-> legacy.put(role.name(), val));
        return legacy;
    }
}
