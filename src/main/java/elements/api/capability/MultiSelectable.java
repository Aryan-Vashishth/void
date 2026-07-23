package elements.api.capability;

import core.actions.Action;
import core.actions.ActionCapability;

import elements.api.Element;
import elements.meta.ElementRole;

/**
 * Capability interface for multi-instance dropdowns (appearing in repeated contexts).
 *
 * <p>Roles: {@link ElementRole#MULTI_TRIGGER}, {@link ElementRole#MULTI_LIST}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → MultiSelectable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface MultiSelectable extends Element {

    String getTriggerLocator();

    String getListLocator();

    @Override
    Object[] getArgs();

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getListLocator();
    }

    default Object[] getArgsWithIndex(int index) {
        Object[] original = getArgs();
        Object[] result = new Object[original.length + 1];
        result[0] = index;
        System.arraycopy(original, 0, result, 1, original.length);
        return result;
    }

    default Object[] argsForIndex(Integer index) {
        return (index == null) ? getArgs() : getArgsWithIndex(index);
    }

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String trigger = getTriggerLocator();
        if (trigger != null && !trigger.isBlank()) roles.put(ElementRole.MULTI_TRIGGER, trigger);
        String list = getListLocator();
        if (list != null && !list.isBlank() && !list.equals(trigger)) roles.put(ElementRole.MULTI_LIST, list);
        return roles;
    }

    @Override
    default ActionCapability capability() { return ActionCapability.MULTI_SELECTABLE; }

    // ── Action emission ─────────────────────────────────────────────────

    /** Opens the multi-dropdown trigger (default index). */
    default Action open() {
        return engine -> {
            var d = engine.resolve(this, ElementRole.MULTI_TRIGGER);
            engine.click(d);
        };
    }

    /** Composite: opens Nth trigger → waits overlay → clicks option by label. */
    default Action selectAtIndex(Integer index) {
        return engine -> {
            engine.click(engine.resolve(this, ElementRole.MULTI_TRIGGER, argsForIndex(index)));
            engine.waitForOverlay(java.time.Duration.ofSeconds(5));
            engine.click(engine.resolve(getExternalFileName(), getListLocator(), getArgs()));
        };
    }
}

