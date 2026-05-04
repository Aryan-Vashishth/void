package elements.api.capability;

import core.actions.Action;
import elements.meta.ElementRole;

/**
 * Capability interface for tooltip elements whose full text appears on hover.
 *
 * <p>Roles: {@link ElementRole#TEXT} and {@link ElementRole#TOOLTIP_CONTENT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → Readable → Hoverable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface Hoverable extends Readable {

    String getToolTipContentLocator();

    @Override
    default String getSecondaryLocator() { return getToolTipContentLocator(); }

    String getEndsWith();

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTextLocator();
    }

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>(Readable.super.getAllLocatorRoles());
        String tip = getToolTipContentLocator();
        if (tip != null && !tip.isBlank() && !roles.containsValue(tip)) roles.put(ElementRole.TOOLTIP_CONTENT, tip);
        return roles;
    }

    // ── Action emission ─────────────────────────────────────────────────

    /** Hovers over the element to trigger tooltip display. */
    default Action hover() {
        return engine -> {
            var d = engine.resolve(this, ElementRole.TEXT);
            engine.hover(d);
        };
    }
}

