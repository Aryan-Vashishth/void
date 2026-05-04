package elements.api.capability;

import core.actions.Action;
import elements.api.Element;
import elements.meta.ElementRole;

/**
 * Capability interface for non-interactive text/display elements (label, span, static cell).
 *
 * <p>Primary locator role: {@link ElementRole#TEXT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → Readable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface Readable extends Element {

    String getTextLocator();

    @Override
    default String getPrimaryLocator() { return getTextLocator(); }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTextLocator();
    }

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String text = getTextLocator();
        if (text != null && !text.isBlank()) roles.put(ElementRole.TEXT, text);
        return roles;
    }

    // ── Action emission ─────────────────────────────────────────────────

    /** Scrolls the element into view. */
    default Action scrollIntoView() {
        return engine -> {
            var d = engine.resolve(this, ElementRole.TEXT);
            engine.scrollTo(d);
        };
    }
}

