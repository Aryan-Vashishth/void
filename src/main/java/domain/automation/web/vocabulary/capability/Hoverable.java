package domain.automation.web.vocabulary.capability;

import core.actions.ActionCapability;

import domain.automation.web.vocabulary.actions.HoverAction;
import domain.automation.web.vocabulary.role.ElementRole;

/**
 * Capability interface for tooltip elements whose full text appears on hover.
 *
 * <p>Roles: {@link ElementRole#TEXT} and {@link ElementRole#TOOLTIP_CONTENT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   UIElement → ReadOnly → Hoverable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 *
 * <p><b>Domain ownership:</b> Web ({@code elements.api.capability}, ADR-021, I3.3).
 * Not a kernel type. The kernel references capabilities solely through
 * {@link core.actions.ActionCapability}.</p>
 */
public interface Hoverable extends ReadOnly {

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
