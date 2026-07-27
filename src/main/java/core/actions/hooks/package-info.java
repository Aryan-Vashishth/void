/**
 * Kernel-owned hook contract -- {@link core.actions.hooks.ActionHandler},
 * {@link core.actions.hooks.BeforeActionHandler}, {@link core.actions.hooks.AfterActionHandler}.
 *
 * <p>Moved out of {@code core.interactions.hooks} (runtime-redesign I2.1, audit D4):
 * that package is the frozen legacy orchestrator zone, and the kernel must not import
 * through it. {@code core.interactions.hooks} retains deprecated bridge types (old
 * extends new) until I9.3, and keeps the domain-specific hook payload libraries
 * ({@link core.interactions.hooks.Before}, {@link core.interactions.hooks.After}) --
 * those are pre-built constants tied to UI-domain concerns (locator waits, scroll,
 * highlight), not the neutral contract itself.</p>
 *
 * <p>The contract still types against {@link core.engine.UIEngine} and
 * {@link core.engine.LocatorDescriptor}, both domain-side today. Retyping against the
 * neutral {@code Executor} contract is Initiative I4's job, not this package's.</p>
 */
package core.actions.hooks;
