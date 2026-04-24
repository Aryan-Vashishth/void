package elements.api;

import elements.meta.ElementRole;

/**
 * Represents a table/grid (and optionally sub-locators for row/column/cell/header).
 * <p>Roles: {@link ElementRole#TABLE}, plus optional {@link ElementRole#ROW}, {@link ElementRole#COLUMN},
 * {@link ElementRole#CELL}, {@link ElementRole#HEADER}</p>
 */
public interface TableElement extends Element {
    /**
     * Key for the main table XPath pattern.
     */
    String getTableLocator();

    /**
     * Key for row locator XPath (optional).
     */
    default String getRowLocator() { return null; }

    /**
     * Key for column locator XPath (optional).
     */
    default String getColumnLocator() { return null; }

    /**
     * Key for specific cell XPath (optional).
     */
    default String getCellLocator() { return null; }

    /**
     * Key for table header XPath (optional).
     */
    default String getHeaderLocator() { return null; }

    /**
     * Returns a display text for this table (for logs).
     */
    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTableLocator();
    }

    /** Build role map with table and optional structural locators. */
    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        String table = getTableLocator();
        if(table!=null && !table.isBlank()) roles.put(ElementRole.TABLE, table);
        String row = getRowLocator();
        if(row!=null && !row.isBlank() && !row.equals(table)) roles.put(ElementRole.ROW, row);
        String col = getColumnLocator();
        if(col!=null && !col.isBlank() && !col.equals(table) && !col.equals(row)) roles.put(ElementRole.COLUMN, col);
        String cell = getCellLocator();
        if(cell!=null && !cell.isBlank() && !roles.containsValue(cell)) roles.put(ElementRole.CELL, cell);
        String hdr = getHeaderLocator();
        if(hdr!=null && !hdr.isBlank() && !roles.containsValue(hdr)) roles.put(ElementRole.HEADER, hdr);
        return roles;
    }

}
