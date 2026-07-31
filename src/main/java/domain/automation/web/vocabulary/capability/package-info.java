/**
 * {@code domain.automation.web.vocabulary.capability} -- Web element capability interfaces.
 *
 * <p>Each interface models a distinct interaction capability a web UI element can have.
 * Capabilities compose via multiple interface inheritance; a page-object enum declares
 * exactly the capabilities it supports.</p>
 *
 * <h3>Capability hierarchy</h3>
 * <ul>
 *   <li>{@link domain.automation.web.vocabulary.capability.Clickable} -- TRIGGER role; {@code click()}</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.Typeable} -- INPUT role; {@code type()}, {@code clear()}</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.Listable} -- LIST role; structural only</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.Selectable} -- extends Clickable + Listable; {@code open()}, {@code select()}</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.Checkable} -- extends Clickable; {@code toggle()}, {@code set(boolean)}</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.ReadOnly} -- TEXT role; {@code getText()}</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.Hoverable} -- extends ReadOnly; {@code hover()}</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.SearchField} -- SEARCH_INPUT + SEARCH_BUTTON; {@code typeSearch()}, {@code submitSearch()}</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.Searchable} -- extends SearchField; adds SEARCH_RESULT</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.SearchableDropdown} -- extends Selectable + Searchable; {@code searchAndSelect()}</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.Uploadable} -- INPUT role; {@code upload(path)}</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.Table} -- TABLE/ROW/COLUMN/CELL/HEADER roles</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.EditableTable} -- extends Table; ADD_ROW/REMOVE_ROW/FOOTER roles</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.MultiSelectable} -- MULTI_TRIGGER + MULTI_LIST; {@code selectAtIndex()}</li>
 *   <li>{@link domain.automation.web.vocabulary.capability.LocatorRoles} -- shared role key constants</li>
 * </ul>
 */
package domain.automation.web.vocabulary.capability;
