package elements.api.capability;

import core.actions.ActionCapability;
import core.actions.ActionCapabilityProvider;
import elements.api.Element;
import elements.meta.ElementRole;

/**
 * Capability interface for table/grid elements with optional sub-locators.
 *
 * <p>Roles: {@link ElementRole#TABLE}, optional ROW, COLUMN, CELL, HEADER</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → Table
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface Table extends Element, ActionCapabilityProvider {

    String getTableLocator();

    default String getRowLocator() { return null; }

    default String getColumnLocator() { return null; }

    default String getCellLocator() { return null; }

    default String getHeaderLocator() { return null; }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTableLocator();
    }

    @Override
    default ActionCapability capability() { return ActionCapability.TABLE; }

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String table = getTableLocator();
        if (table != null && !table.isBlank()) roles.put(ElementRole.TABLE, table);
        String row = getRowLocator();
        if (row != null && !row.isBlank() && !row.equals(table)) roles.put(ElementRole.ROW, row);
        String col = getColumnLocator();
        if (col != null && !col.isBlank() && !col.equals(table) && !col.equals(row)) roles.put(ElementRole.COLUMN, col);
        String cell = getCellLocator();
        if (cell != null && !cell.isBlank() && !roles.containsValue(cell)) roles.put(ElementRole.CELL, cell);
        String hdr = getHeaderLocator();
        if (hdr != null && !hdr.isBlank() && !roles.containsValue(hdr)) roles.put(ElementRole.HEADER, hdr);
        return roles;
    }

}

