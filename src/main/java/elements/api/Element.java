package elements.api;

import core.actions.ActionCapability;
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
    /**
     * Returns the conventional classpath path of the external locator resource for this element,
     * or {@code null} if the locator is hardcoded.
     *
     * <p>Default: probes {@code PageClass/locators.json} then {@code PageClass/locators.properties}
     * under the FQCN-derived directory. Returns the first that exists on the classpath, or the
     * {@code .json} path as the preferred target when neither exists yet.
     * Override to point to a different file.</p>
     */
    @Nullable
    default String getExternalFileName() {
        Class<?> enumClass = ElementSupport.declaringClassOf(this);
        Class<?> pageClass = enumClass.getEnclosingClass();
        Class<?> target = pageClass != null ? pageClass : enumClass;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String dir = target.getName().replace('.', '/') + "/";
        for (String file : new String[]{"locators.json", "locators.properties"}) {
            if (cl.getResource(dir + file) != null) return dir + file;
        }
        return dir + "locators.json";
    }

    /**
     * Returns the namespaced locator key for this element.
     * <p>For capability elements, delegates to the first role returned by {@link #getAllLocatorRoles()},
     * which each capability interface populates independently. Falls back to the enum-name–derived key
     * {@code PageName.GroupName.CONSTANT_NAME} for plain elements with no capability roles.</p>
     * <p>{@link elements.api.LocatorFamily} and its subtypes override this directly to return
     * the shared family key (no constant suffix), so they are unaffected by this delegation.</p>
     */
    default String getPrimaryLocator() {
        java.util.Map<ElementRole, String> roles = getAllLocatorRoles();
        if (!roles.isEmpty()) return roles.values().iterator().next();
        Class<?> enumClass = ElementSupport.declaringClassOf(this);
        Class<?> pageClass = enumClass.getEnclosingClass();
        String name = ElementSupport.nameOf(this);
        return pageClass != null
            ? pageClass.getSimpleName() + "." + enumClass.getSimpleName() + "." + name
            : enumClass.getSimpleName() + "." + name;
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
        String[] tokens = ElementSupport.nameOf(this).split("_");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) sb.append(token.substring(1).toLowerCase());
        }
        return sb.isEmpty() ? "element" : sb.toString();
    }

    /**
     * Returns the locator key for a given role on this element.
     *
     * <p>Default: delegates to {@link #qualifiedLocatorKey} — per-constant format
     * {@code PageName.EnumName.CONSTANT.ROLE}.
     * {@link LocatorFamily} overrides this to return the shared family key instead,
     * so all capability defaults automatically route through the family template
     * without any per-enum boilerplate.</p>
     */
    default String locatorKeyForRole(ElementRole role) {
        return qualifiedLocatorKey(this, role);
    }

    /**
     * Returns the single shared template key for this element if it uses the
     * family-locator pattern, or {@code null} if per-constant keys should be generated.
     *
     * <p>The sync tool calls this to decide whether to emit one family key
     * ({@link LocatorFamily} returns non-null) or one key per constant (default null).
     * New strategies opt in by overriding this method — the generator never changes.</p>
     */
    default String templateFamilyKey() { return null; }

    static String qualifiedLocatorKey(Element element, ElementRole role) {
        Class<?> enumClass = ElementSupport.declaringClassOf(element);
        Class<?> pageClass = enumClass.getEnclosingClass();
        String prefix = (pageClass != null)
            ? pageClass.getSimpleName() + "." + enumClass.getSimpleName()
            : enumClass.getSimpleName();
        return prefix + "." + ElementSupport.nameOf(element) + "." + role.name();
    }

    /**
     * Returns the capability category of this element.
     *
     * <p>Each element represents exactly one interaction kind. Capability interfaces
     * override this to return their specific constant. Elements that do not participate
     * in the Action/Flow pipeline return {@link ActionCapability#UNKNOWN}.</p>
     *
     * <p><b>Invariant:</b> one capability per element. An element that extends both
     * {@code Clickable} and {@code Typeable} is a modelling error -- the method cannot
     * represent two capabilities simultaneously.</p>
     */
    default ActionCapability capability() {
        return ActionCapability.UNKNOWN;
    }

    /**
     * Builds an ordered map of {@link ElementRole} to locator key strings.
     * Only non-blank locators are included; order reflects fallback priority.
     * <p>The base implementation returns an empty map. Capability interfaces override this
     * independently (without calling {@link #getPrimaryLocator()}) to populate their specific
     * roles (INPUT, TRIGGER, TEXT, etc.), avoiding the circular dependency that would result
     * from delegating here to {@code getPrimaryLocator()} while that method delegates back.</p>
     */
    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        return java.util.Collections.emptyMap();
    }

}
