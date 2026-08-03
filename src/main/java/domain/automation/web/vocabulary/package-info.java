/**
 * {@code domain.automation.web.vocabulary} -- Web domain vocabulary (logical ownership layer).
 *
 * <p>Parent package for all Web-domain vocabulary: the UIElement model, capabilities,
 * deferred actions, and roles. This is the "what" layer of the Web domain -- concepts
 * are defined here without any execution logic (ADR-021 guardrail rule 8).</p>
 *
 * <h3>Sub-packages</h3>
 * <ul>
 *   <li>{@code vocabulary.element} -- {@code UIElement} root and locator-family contracts</li>
 *   <li>{@code vocabulary.capability} -- capability interfaces (Clickable, Typeable, etc.)</li>
 *   <li>{@code vocabulary.actions} -- deferred {@code Action} implementations per capability</li>
 *   <li>{@code vocabulary.role} -- {@code ElementRole} and role-registry utilities</li>
 * </ul>
 *
 * <h3>Design invariant</h3>
 * <p>No type in this package tree may execute browser interactions. Capabilities emit
 * deferred {@code Action} lambdas; only {@link domain.automation.web.engine.UIEngine}
 * executes.</p>
 */
package domain.automation.web.vocabulary;
