package core.actions;

import core.interactions.hooks.After;
import core.interactions.hooks.Before;
import core.logging.CustomLogger;
import core.utils.ConfigLoader;

/**
 * Internal helper for applying config-driven default action profiles.
 *
 * <p>Owns capability-specific safe and reliable profile constants. These live here —
 * in the action layer — because execution policy is an execution concern,
 * not a capability concern. Capability interfaces must not reference these constants.</p>
 */
final class ActionProfiles {

    static final String DEFAULT_PROFILE_KEY = "void.profile.default";

    // ── Capability-specific safe profiles ──────────────────────────────────
    // Policy lives in the action layer. Capabilities declare what they are;
    // actions declare how they execute.

    static final ActionProfile DEFAULT_SAFE = ActionProfile.builder()
            .name("SAFE")
            .before(Before.WAIT_FOR_ELEMENT_VISIBLE)
            .build();

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

    static final ActionProfile DEFAULT_RELIABLE = ActionProfile.builder()
            .name("RELIABLE")
            .before(Before.WAIT_FOR_ELEMENT_VISIBLE)
            .after(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT)
            .build();

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

    private ActionProfiles() {}

    static Action applyConfiguredDefault(Action action) {
        ActionProfile configured = configuredDefault();
        if (configured == Profiles.RAW) {
            return action;
        }
        return action.using(configured);
    }

    static ActionProfile configuredDefault() {
        String configuredName = ConfigLoader.get(DEFAULT_PROFILE_KEY, "RAW");
        ActionProfile profile = Profiles.fromName(configuredName);
        if (profile == Profiles.RAW && configuredName != null && !configuredName.isBlank()
                && !"RAW".equalsIgnoreCase(configuredName)) {
            CustomLogger.warn.log("Unknown action profile '" + configuredName + "' for key "
                    + DEFAULT_PROFILE_KEY + ". Falling back to RAW.");
        }
        return profile;
    }
}

