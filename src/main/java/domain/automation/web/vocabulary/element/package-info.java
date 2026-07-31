/**
 * {@code domain.automation.web.vocabulary.element} -- UIElement root and locator-family contracts.
 *
 * <p>Defines the root abstraction for all Web-domain element descriptors and the
 * locator-family contracts that govern how elements expose their locator keys.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link domain.automation.web.vocabulary.element.UIElement} -- web-domain refinement of
 *       {@code core.target.Target}; adds locator keys, external file reference, and role map.
 *       Implemented by page-object enum constants.</li>
 *   <li>{@link domain.automation.web.vocabulary.element.LocatorFamily} -- base locator-family
 *       contract; {@code getPrimaryLocator()} and optional {@code getSecondaryLocator()}.</li>
 *   <li>{@link domain.automation.web.vocabulary.element.AdvancedLocatorFamily} -- extends
 *       {@code LocatorFamily} with multiple named locator keys.</li>
 *   <li>{@link domain.automation.web.vocabulary.element.SwitchLocatorFamily} -- extends
 *       {@code AdvancedLocatorFamily} with runtime locator switching.</li>
 *   <li>{@link domain.automation.web.vocabulary.element.ElementSupport} -- package-private utility
 *       providing enum-specific helpers ({@code nameOf}, {@code declaringClassOf},
 *       {@code ordinalOf}). Scope frozen by ADR-017.</li>
 *   <li>{@link domain.automation.web.vocabulary.element.KeyValuePair} -- utility interface
 *       for enums that map an internal key to a user-facing label.</li>
 * </ul>
 */
package domain.automation.web.vocabulary.element;
