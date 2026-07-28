/**
 * UI-domain concrete action layer -- {@link elements.api.actions.ElementAction} and its
 * family (the three abstract intermediaries, the 17 concrete leaf classes, and the
 * {@link elements.api.actions.ElementActions} factory), plus
 * {@link elements.api.actions.CapabilityProfiles}.
 *
 * <p>Relocated from {@code core.actions} in runtime-redesign I2, phase 2.2 (kernel/UI
 * action split, ADR-021, audit Part I bounded-context finding): the kernel/UI boundary
 * previously ran invisibly through the middle of {@code core.actions}. This package is
 * the UI-domain side of that boundary -- everything here is genuinely UI-specific
 * (it types against {@link elements.api.UIElement}, {@link elements.meta.ElementRole},
 * and the capability interfaces in {@link elements.api.capability}), while
 * {@code core.actions} keeps only the domain-neutral kernel contracts (Action, Flow,
 * ActionProfile, ActionCapability, HookChainAction).</p>
 *
 * <p>Elements NEVER execute — they emit intent only. Execution is handled by
 * {@link core.engine.UIEngine} at runtime.</p>
 */
package elements.api.actions;
