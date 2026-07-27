package elements.api.actions;

import core.actions.ActionProfile;
import core.interactions.hooks.After;
import core.interactions.hooks.Before;

/**
 * Capability-specific safe and reliable profile constants for the concrete UI action
 * families ({@link ClickableElementAction}, {@link TypeableElementAction},
 * {@link SelectableElementAction}).
 *
 * <p>Moved out of {@code core.actions.ActionProfiles} in runtime-redesign I2.2 (kernel/UI
 * action split): this content is UI-domain vocabulary -- it encodes per-capability
 * execution policy (which loader waits, which highlight color) using {@code Before}/
 * {@code After} hook constants, not domain-neutral defaults. The kernel keeps only
 * {@code ActionProfiles.DEFAULT_SAFE}/{@code DEFAULT_RELIABLE}.</p>
 */
final class CapabilityProfiles {

    private CapabilityProfiles() {}

    // ── Capability-specific safe profiles ──────────────────────────────────
    // Policy lives in the action layer. Capabilities declare what they are;
    // actions declare how they execute.

    static final ActionProfile CLICKABLE_SAFE = ActionProfile.builder()
            .name("SAFE")
            .before(Before.WAIT_FOR_ELEMENT_CLICKABLE)
            .after(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT)
            .build();

    static final ActionProfile TYPEABLE_SAFE = ActionProfile.builder()
            .name("SAFE")
            .before(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE)
            .after(After.HIGHLIGHT_ELEMENT)
            .build();

    static final ActionProfile SELECTABLE_SAFE = ActionProfile.builder()
            .name("SAFE")
            .before(Before.WAIT_FOR_ELEMENT_VISIBLE,
                    Before.WAIT_FOR_ELEMENT_CLICKABLE,
                    Before.WAIT_FOR_ANGULAR_LOADER)
            .after(After.HIGHLIGHT_ELEMENT)
            .build();

    // ── Capability-specific reliable profiles ─────────────────────────────
    // Reliable execution adds loader waits before and after. Per-capability
    // before-hooks vary; after-hooks are uniform across all capabilities.

    static final ActionProfile CLICKABLE_RELIABLE = ActionProfile.builder()
            .name("RELIABLE")
            .before(Before.WAIT_FOR_ANGULAR_LOADER, Before.WAIT_FOR_ELEMENT_CLICKABLE)
            .after(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT)
            .build();

    static final ActionProfile TYPEABLE_RELIABLE = ActionProfile.builder()
            .name("RELIABLE")
            .before(Before.WAIT_FOR_ANGULAR_LOADER, Before.WAIT_FOR_ELEMENT_VISIBLE, Before.CLEAR_FIELD)
            .after(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT)
            .build();

    static final ActionProfile SELECTABLE_RELIABLE = ActionProfile.builder()
            .name("RELIABLE")
            .before(Before.WAIT_FOR_ANGULAR_LOADER, Before.WAIT_FOR_ELEMENT_VISIBLE, Before.WAIT_FOR_ELEMENT_CLICKABLE)
            .after(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT)
            .build();
}
