package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for writable tables supporting row insertion/removal.
 *
 * <p>Extends {@link TableTarget} with add-row, remove-row, and footer-input locators.
 * Contains NO execution or Action logic.</p>
 *
 * <p>Additional roles: {@link ElementRole#ADD_ROW_BUTTON}, {@link ElementRole#REMOVE_ROW_BUTTON},
 * {@link ElementRole#FOOTER_INPUT_ROW}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → TableTarget → WritableTableTarget → WritableTableAction
 * </pre>
 */
public interface WritableTableTarget extends TableTarget {

    default String getAddRowButtonLocator() { return null; }

    default String getRemoveRowButtonLocator() { return null; }

    default String getFooterInputRowLocator() { return null; }

    @Override
    default String getPrimaryLocator() { return getTableLocator(); }

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>(TableTarget.super.getAllLocatorRoles());
        String add = getAddRowButtonLocator();
        if (add != null && !add.isBlank() && !roles.containsValue(add)) roles.put(ElementRole.ADD_ROW_BUTTON, add);
        String remove = getRemoveRowButtonLocator();
        if (remove != null && !remove.isBlank() && !roles.containsValue(remove)) roles.put(ElementRole.REMOVE_ROW_BUTTON, remove);
        String footer = getFooterInputRowLocator();
        if (footer != null && !footer.isBlank() && !roles.containsValue(footer)) roles.put(ElementRole.FOOTER_INPUT_ROW, footer);
        return roles;
    }
}

