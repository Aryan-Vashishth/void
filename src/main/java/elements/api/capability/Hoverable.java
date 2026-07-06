package elements.api.capability;

import core.actions.ActionCapability;
import core.actions.ActionCapabilityProvider;
import core.actions.HoverAction;
import elements.meta.ElementRole;

/**
 * Capability interface for tooltip elements whose full text appears on hover.
 *
 * <p>Roles: {@link ElementRole#TEXT} and {@link ElementRole#TOOLTIP_CONTENT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ReadOnly → Hoverable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface Hoverable extends ReadOnly, ActionCapabilityProvider {

    String getToolTipContentLocator();

    @Override
    default String getSecondaryLocator() { return getToolTipContentLocator(); }

    String getEndsWith();

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>(ReadOnly.super.getAllLocatorRoles());
        String tip = getToolTipContentLocator();
        if (tip != null && !tip.isBlank() && !roles.containsValue(tip)) roles.put(ElementRole.TOOLTIP_CONTENT, tip);
        return roles;
    }

    @Override
    default ActionCapability capability() { return ActionCapability.HOVERABLE; }

    // ── Action emission ─────────────────────────────────────────────────

    /** Emits a {@link HoverAction} targeting this element's TEXT locator. */
    default HoverAction hover() {
        return new HoverAction(this);
    }
}
