package core.actions;

import core.interactions.hooks.Before;
import core.logging.CustomLogger;
import core.utils.ConfigLoader;

/**
 * Internal helper for applying config-driven default action profiles.
 */
final class ActionProfiles {

    static final String DEFAULT_PROFILE_KEY = "void.profile.default";

    /** Generic safe profile: wait for element visible. No capability dispatch. */
    static final ActionProfile DEFAULT_SAFE = ActionProfile.builder()
            .before(Before.WAIT_FOR_ELEMENT_VISIBLE)
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

