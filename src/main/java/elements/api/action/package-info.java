/**
 * Action interfaces — behavior layer.
 *
 * <p>Each {@code *Action} interface extends its corresponding {@code *Target} and adds
 * default methods that produce deferred {@link core.actions.Action} objects.
 * Locator resolution happens <b>inside</b> the Action lambda at execution time.</p>
 *
 * <p>Hierarchy: {@code Element → *Target → *Action}</p>
 */
package elements.api.action;

