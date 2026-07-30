/**
 * Web-domain locator vocabulary.
 *
 * <p>Contains the two types that describe how a UI element is addressed:
 * {@link elements.locator.LocatorDescriptor} (the resolved locator + strategy + parent scope)
 * and {@link elements.locator.LocatorStrategy} (the open strategy token).</p>
 *
 * <p>Both types are owned by the web domain. They previously lived in
 * {@code core.engine} (the neutral contract package) and were moved here
 * in runtime-redesign I7.2 to align ownership with usage: these are DOM-scoped,
 * UI-domain nouns, not neutral engine contract types.</p>
 *
 * <p>The final physical relocation to {@code domain.automation.web.*} is
 * deferred to I6.4 (Class Migration Matrix gate).</p>
 */
package elements.locator;
