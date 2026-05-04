package elements.api;

import elements.meta.ElementRole;

/**
 * Capability interface for table/grid elements with optional sub-locators.
 *
 * <p>Defines the structural contract — exposes locator keys for table, row, column,
 * cell, header. Contains NO execution or Action logic.</p>
 *
 * <p>Roles: {@link ElementRole#TABLE}, optional {@link ElementRole#ROW},
 * {@link ElementRole#COLUMN}, {@link ElementRole#CELL}, {@link ElementRole#HEADER}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → TableTarget → TableAction
 * </pre>
 */
public interface TableTarget extends Element {

    String getTableLocator();

    default String getRowLocator() { return null; }

    default String getColumnLocator() { return null; }

    default String getCellLocator() { return null; }

    default String getHeaderLocator() { return null; }

    @Override
    default String getPrimaryLocator() { return getTableLocator(); }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTableLocator();
    }

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

