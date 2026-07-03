package elements.api.capability;

import core.actions.Action;
import core.actions.ActionCapability;
import core.actions.ActionCapabilityProvider;
import core.actions.ActionProfile;
import core.actions.ElementActions;
import core.interactions.hooks.After;
import core.interactions.hooks.Before;
import elements.api.Element;
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
 *   Element → Clickable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Produces deferred {@link Action} objects via {@link #click()}.
 * Resolution happens <b>inside</b> the lambda — never eagerly.</p>
 */
public interface Clickable extends Element, ActionCapabilityProvider {

    /** @return property key for the clickable element's locator template. */
    String getTriggerLocator();

    @Override
    default String getPrimaryLocator() { return getTriggerLocator(); }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTriggerLocator();
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

    ActionProfile CLICKABLE_SAFE_PROFILE = ActionProfile.builder()
            .before(Before.WAIT_FOR_ELEMENT_CLICKABLE)
            .after(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT)
            .build();

    @Override
    default ActionProfile safeProfile() { return CLICKABLE_SAFE_PROFILE; }

    // ── Action emission ─────────────────────────────────────────────────

    /** Deferred click action. Locator resolved at execution time by the engine. */
    default Action click() {
        return ElementActions.of(this, ElementRole.TRIGGER,
                (engine, d) -> engine.click(d));
    }
}
