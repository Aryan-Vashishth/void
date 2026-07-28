/**
 * {@code core.actions} — Kernel-owned deferred execution model.
 *
 * <p>This package defines the domain-neutral <b>Action</b> contract that forms the
 * kernel side of VOID's primary execution path. Actions represent deferred operations
 * (intent) that are composed into flows and executed later by a
 * {@link core.executor.FlowExecutor}.</p>
 *
 * <p>As of runtime-redesign I2, phase 2.2 (kernel/UI action split, ADR-021), the
 * concrete UI action types -- {@code ElementAction} and its family (the three abstract
 * intermediaries, the 17 concrete leaf classes, and the {@code ElementActions} factory)
 * -- live in {@link elements.api.actions}, not here. This package retains only the
 * types on ADR-021's kernel membership list; it has zero dependency on
 * {@link elements.api.UIElement}, {@link elements.meta.ElementRole}, or
 * {@link elements.api.capability}.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link core.actions.Action} — functional interface representing a single deferred
 *       operation. Concrete UI action subclasses (produced by capability interfaces, e.g.
 *       {@code element.click()}, {@code element.type("text")}) live in
 *       {@link elements.api.actions}. Supports directional hook composition via
 *       {@link core.actions.Action#before(core.actions.hooks.BeforeActionHandler...)} and
 *       {@link core.actions.Action#after(core.actions.hooks.AfterActionHandler...)}.</li>
 *   <li>{@code HookChainAction} — internal wrapper that stores before/after
 *       {@link core.actions.hooks.ActionHandler} hooks and emits an
 *       {@link core.actions.trace.ActionTrace} on each execution.</li>
 *   <li>{@link core.actions.ActionProfiles} — domain-neutral default safe/reliable
 *       profiles and config-driven default-profile selection. Capability-specific
 *       profile constants live in {@link elements.api.actions.CapabilityProfiles}.</li>
 * </ul>
 *
 * <h3>Execution path</h3>
 * <pre>
 *   UIElement (capability interface)
 *     → Action (deferred intent; concrete UI actions in elements.api.actions)
 *       → Flow (composition)
 *         → FlowExecutor (iteration)
 *           → UIEngine (physical execution)
 * </pre>
 *
 * <h3>Design rules</h3>
 * <ul>
 *   <li>Actions are <b>deferred</b> — locator resolution happens inside
 *       {@link core.actions.Action#perform}, never eagerly.</li>
 *   <li>Kernel action types never reference {@code WebDriver}, {@code WebElement},
 *       {@code By}, {@code UIElement}, {@code ElementRole}, or capability interfaces.</li>
 *   <li>Hook composition is optional and fluent:
 *       {@code element.click().before(...).after(...)}</li>
 * </ul>
 *
 * <h3>Stability</h3>
 * <p>This package is <b>@Beta</b> — the API may change between releases. Do not use
 * inside stable modules. External consumers interact with Actions opaquely by passing
 * them to {@link core.flow.Flow#of} and {@link core.executor.FlowExecutor#run}.</p>
 *
 * @see core.flow.Flow
 * @see core.executor.FlowExecutor
 * @see core.engine.UIEngine
 * @see core.actions.hooks.ActionHandler
 * @see elements.api.actions
 */
package core.actions;
