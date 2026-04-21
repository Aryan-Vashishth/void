/**
 * Framework core — pure UI interaction engine with NO BDD/Cucumber dependencies.
 *
 * <p>{@link interactions.Interactions} provides every low-level Selenium action
 * (click, type, search, dropdown, etc.) driven by typed element enums and
 * role-based locator resolution.</p>
 *
 * <p>The {@code hooks} sub-package defines the {@link interactions.hooks.ActionHandler}
 * interface and built-in {@link interactions.hooks.Before}/{@link interactions.hooks.After}
 * enum constants that compose optional pre/post-action behaviour (waits, highlights, etc.).</p>
 *
 * <p>For BDD-specific wrappers, see {@code automation.interactions.StepDefInteractions}.</p>
 */
package interactions;

