package elements.api.capability;

import core.actions.Action;
import core.annotations.Beta;
import elements.meta.ElementRole;

import java.time.Duration;

/**
 * Capability interface for dropdown elements with a trigger and a list/options panel.
 *
 * <p>Roles: {@link ElementRole#TRIGGER} (button/icon) and {@link ElementRole#LIST} (options)</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → Clickable ─┐
 *   Element → Listable  ─┤→ Selectable
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
    default String getPrimaryLocator() { return getTriggerLocator(); }

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

    // ── Action emission ─────────────────────────────────────────────────

    /** Opens the dropdown trigger. */
    @Beta
    default Action open() {
        return engine -> {
            var d = engine.resolve(this, ElementRole.TRIGGER);
            engine.click(d);
        };
    }

    /** Composite: opens trigger → waits for overlay → clicks option. */
    @Beta
    default Action select() {
        return engine -> {
            engine.click(engine.resolve(this, ElementRole.TRIGGER));
            engine.waitForOverlay(Duration.ofSeconds(5));
            engine.click(engine.resolve(this, ElementRole.LIST, getArgs()));
        };
    }

    @Beta
    default Action selectByText(String text) {
        return engine -> {
            var d = engine.resolve(this, ElementRole.LIST);
            engine.selectByVisibleText(d, text);
        };
    }

    @Beta
    default Action selectByValue(String value) {
        return engine -> {
            var d = engine.resolve(this, ElementRole.LIST);
            engine.selectByValue(d, value);
        };
    }
}

