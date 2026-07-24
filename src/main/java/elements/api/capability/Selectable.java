package elements.api.capability;

import core.actions.ActionCapability;
import core.actions.OpenAction;
import core.actions.SelectAction;
import core.actions.SelectByTextAction;
import core.actions.SelectByValueAction;
import elements.meta.ElementRole;

/**
 * Capability interface for dropdown elements with a trigger and a list/options panel.
 *
 * <p>Roles: {@link ElementRole#TRIGGER} (button/icon) and {@link ElementRole#LIST} (options)</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   UIElement → Clickable ─┐
 *   UIElement → Listable  ─┤→ Selectable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface Selectable extends Clickable, Listable {

    @Override
    String getTriggerLocator();

    @Override
    String getListLocator();

    @Override
    default String getSecondaryLocator() { return getListLocator(); }

    @Override
    default String getDisplayText() { return Clickable.super.getDisplayText(); }

    @Override
    default int getIndex() { return 0; }

    @Override
    String getExternalFileName();

    @Override
    Object[] getArgs();

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String trigger = getTriggerLocator();
        if (trigger != null && !trigger.isBlank()) roles.put(ElementRole.TRIGGER, trigger);
        String list = getListLocator();
        if (list != null && !list.isBlank() && !list.equals(trigger)) roles.put(ElementRole.LIST, list);
        return roles;
    }

    @Override
    default ActionCapability capability() { return ActionCapability.SELECTABLE; }

    // ── Action emission ─────────────────────────────────────────────────

    /** Emits an {@link OpenAction} — clicks the TRIGGER locator to reveal the options panel. */
    default OpenAction open() {
        return new OpenAction(this);
    }

    /** Emits a {@link SelectAction} — opens TRIGGER, waits for overlay, clicks LIST. */
    default SelectAction select() {
        return new SelectAction(this);
    }

    /** Emits a {@link SelectByTextAction} — selects an option by visible text. */
    default SelectByTextAction selectByText(String text) {
        return new SelectByTextAction(this, text);
    }

    /** Emits a {@link SelectByValueAction} — selects an option by value attribute. */
    default SelectByValueAction selectByValue(String value) {
        return new SelectByValueAction(this, value);
    }
}

