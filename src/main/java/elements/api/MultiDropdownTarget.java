package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for multi-instance dropdowns (appearing in repeated contexts).
 *
 * <p>Roles: {@link ElementRole#MULTI_TRIGGER}, {@link ElementRole#MULTI_LIST}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → MultiDropdownTarget → MultiDropdownAction
 * </pre>
 */
public interface MultiDropdownTarget extends Element {

    String getTriggerLocator();

    String getListLocator();

    @Override
    default String getPrimaryLocator() { return getTriggerLocator(); }

    @Override
    Object[] getArgs();

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getListLocator();
    }

    /**
     * Returns a new argument array with the index prepended.
     */
    default Object[] getArgsWithIndex(int index) {
        Object[] original = getArgs();
        Object[] result = new Object[original.length + 1];
        result[0] = index;
        System.arraycopy(original, 0, result, 1, original.length);
        return result;
    }

    /**
     * Returns the appropriate argument array for the given (possibly null) index.
     */
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
}

