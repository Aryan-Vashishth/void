/**
 * Domain-specific hook payload libraries -- before/after handlers for UI interactions.
 *
 * <p>The hook contract itself ({@link core.actions.hooks.ActionHandler},
 * {@link core.actions.hooks.BeforeActionHandler}, {@link core.actions.hooks.AfterActionHandler})
 * moved to the kernel-owned {@code core.actions.hooks} package in runtime-redesign I2.1
 * (ADR-021, audit D4) -- this package is the frozen legacy orchestrator zone and the
 * kernel must not import through it. This package retains:</p>
 * <ul>
 *   <li>Deprecated bridge types ({@code ActionHandler}, {@code BeforeActionHandler},
 *       {@code AfterActionHandler}: old extends new) that keep existing imports and
 *       implementations compiling until I9.3.</li>
 *   <li>{@link core.interactions.hooks.Before} and {@link core.interactions.hooks.After} --
 *       the pre-built, domain-specific constant libraries. These are UI-domain content
 *       (locator waits, scroll, highlight) awaiting relocation to a UI-domain package in
 *       a later initiative; they are not part of the kernel move.</li>
 * </ul>
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

