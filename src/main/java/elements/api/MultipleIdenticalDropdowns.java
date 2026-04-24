package elements.api;

import elements.meta.ElementRole;

/**
 * Specialized dropdown appearing multiple times (e.g., in table rows or repeating form sections).
 * <p>Roles: {@link ElementRole#MULTI_TRIGGER}, {@link ElementRole#MULTI_LIST}</p>
 */
public interface MultipleIdenticalDropdowns extends Element {

    /**
     * Returns the key for the trigger locator in the property file.
     */
    String getTriggerLocator();

    /**
     * Returns the key for the list (dropdown options) locator in the property file.
     */
    String getListLocator();

    /**
     * Optionally includes additional arguments (e.g., index, context) for dynamic XPaths.
     */
    @Override
    Object[] getArgs();

    /**
     * Returns a human-readable display text for this dropdown, useful for logs.
     */
    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getListLocator();
    }

    /**
     * Returns a new argument array with the index prepended.
     * Useful for accessing a specific instance of a repeated dropdown.
     */
    default Object[] getArgsWithIndex(int index) {
        Object[] original = getArgs();
        Object[] result = new Object[original.length + 1];
        result[0] = index;
        System.arraycopy(original, 0, result, 1, original.length);
        return result;
    }

    /**
     * Returns the appropriate argument array for the given (possibly {@code null}) dropdown index:
     * {@link #getArgs()} when {@code index == null}, else {@link #getArgsWithIndex(int)}.
     *
     * <p>Encapsulates the {@code index == null ? getArgs() : getArgsWithIndex(index)} ternary
     * that was repeated at every resolver call site.</p>
     */
    default Object[] argsForIndex(Integer index) {
        return (index == null) ? getArgs() : getArgsWithIndex(index);
    }

    /**
     * Returns a map of all locators for this element, including trigger and list keys.
     */
    /** Build role map with MULTI_TRIGGER and MULTI_LIST keys. */
    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        String trigger = getTriggerLocator();
        if(trigger!=null && !trigger.isBlank()) roles.put(ElementRole.MULTI_TRIGGER, trigger);
        String list = getListLocator();
        if(list!=null && !list.isBlank() && !list.equals(trigger)) roles.put(ElementRole.MULTI_LIST, list);
        return roles;
    }

}
