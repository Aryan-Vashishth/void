/**
 * {@code domain.automation.web.vocabulary.role} -- Element role vocabulary.
 *
 * <p>Defines the named locator roles ({@link domain.automation.web.vocabulary.role.ElementRole})
 * used by capabilities to distinguish multiple locator keys on a single element, and the
 * {@link domain.automation.web.vocabulary.role.EnumClassRegistry} used to associate
 * UIElement implementation classes with their declaring page class.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link domain.automation.web.vocabulary.role.ElementRole} -- open enum of locator
 *       roles (TRIGGER, INPUT, LIST, TEXT, SEARCH_INPUT, SEARCH_BUTTON, etc.).</li>
 *   <li>{@link domain.automation.web.vocabulary.role.EnumClassRegistry} -- registry mapping
 *       UIElement enum classes to their enclosing page class; supports IDE navigation via
 *       the {@code PageName.Group.CONSTANT} convention.</li>
 * </ul>
 */
package domain.automation.web.vocabulary.role;
