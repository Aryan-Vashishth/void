package elements.api.capability;

import core.actions.Action;
import core.actions.ActionCapability;

import elements.api.actions.ClickAction;
import elements.api.UIElement;
import elements.meta.ElementRole;

/**
 * Capability interface for elements that can be clicked (button, link, icon, etc.).
 *
 * <p>Defines the structural contract for clickable elements — exposes locator key
 * and role mapping. Contains NO execution logic. Emits Action (intent) only.</p>
 *
 * <p>Primary locator role: {@link ElementRole#TRIGGER}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   UIElement → Clickable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Produces deferred {@link Action} objects via {@link #click()}.
 * Resolution happens <b>inside</b> the lambda — never eagerly.</p>
 */
public interface Clickable extends UIElement {

    /** @return fully-qualified role-suffixed locator key, e.g. {@code PageName.EnumName.CONSTANT.TRIGGER}. */
    default String getTriggerLocator() { return locatorKeyForRole(ElementRole.TRIGGER); }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : UIElement.super.getDisplayText();
    }

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String trigger = getTriggerLocator();
        if (trigger != null && !trigger.isBlank()) roles.put(ElementRole.TRIGGER, trigger);
        return roles;
    }

    @Override
    default ActionCapability capability() { return ActionCapability.CLICKABLE; }

    // ── Action emission ─────────────────────────────────────────────────

    /** Emits a {@link ClickAction} targeting this element's TRIGGER locator. */
    default ClickAction click() {
        return new ClickAction(this);
    }
}
