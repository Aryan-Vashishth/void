/**
 * Action hooks — before/after handlers for UI interactions.
 *
 * <p>The {@link core.interactions.hooks.ActionHandler} functional interface defines the
 * hook contract: {@code (UIEngine, LocatorDescriptor) → void}.  Pre-built constants
 * are provided by {@link core.interactions.hooks.Before} and
 * {@link core.interactions.hooks.After}.</p>
 *
 * <p>Hooks receive the element's {@code LocatorDescriptor} directly — they do not
 * depend on global state.  For legacy code paths that cannot supply a descriptor,
 * hooks guard against {@code null} and log a warning.</p>
 *
 * <h3>Hook ordering guarantee</h3>
 * <ol>
 *   <li>Before hooks execute in list order.</li>
 *   <li>The action executes.</li>
 *   <li>After hooks execute in list order.</li>
 * </ol>
 */
package core.interactions.hooks;

