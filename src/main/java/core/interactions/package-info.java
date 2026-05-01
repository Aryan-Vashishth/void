/**
 * Framework core — pure UI interaction engine with NO BDD/Cucumber dependencies.
 *
 * <p>{@link core.interactions.Interactions} provides every low-level Selenium action
 * (click, type, search, dropdown, etc.) driven by typed element enums and
 * role-based locator resolution.</p>
 *
 * <p>The {@code hooks} sub-package defines the {@link core.interactions.hooks.ActionHandler}
 * interface and built-in {@link core.interactions.hooks.Before}/{@link core.interactions.hooks.After}
 * constants that compose optional pre/post-action behaviour (waits, highlights, etc.).</p>
 *
 * <p>For BDD-specific wrappers, see {@code dsl.VoidDSL}.</p>
 */
package core.interactions;

