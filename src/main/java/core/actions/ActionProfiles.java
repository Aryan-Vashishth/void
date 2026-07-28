package core.actions;

import core.interactions.hooks.After;
import core.interactions.hooks.Before;
import core.logging.CustomLogger;
import core.utils.ConfigLoader;

/**
 * Kernel helper for applying config-driven default action profiles.
 *
 * <p>Owns only the domain-neutral default safe/reliable profiles and the config-driven
 * default-profile selection mechanism. Capability-specific profile constants
 * (CLICKABLE_SAFE, TYPEABLE_SAFE, etc.) are UI-domain vocabulary and live in
 * {@link elements.api.actions.CapabilityProfiles} (runtime-redesign I2.2), not here.</p>
 */
public final class ActionProfiles {

    static final String DEFAULT_PROFILE_KEY = "void.profile.default";

    public static final ActionProfile DEFAULT_SAFE = ActionProfile.builder()
            .name("SAFE")
            .before(Before.WAIT_FOR_ELEMENT_VISIBLE)
            .build();

    public static final ActionProfile DEFAULT_RELIABLE = ActionProfile.builder()
            .name("RELIABLE")
            .before(Before.WAIT_FOR_ELEMENT_VISIBLE)
            .after(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT)
            .build();

    private ActionProfiles() {}

    public static Action applyConfiguredDefault(Action action) {
        if (ActionCapability.UNKNOWN.equals(action.capability())) {
            CustomLogger.warn.log(
                "applyConfiguredDefault: action with UNKNOWN capability skipped -- no profile applied. " +
                "Declare a specific ActionCapability, or set " + DEFAULT_PROFILE_KEY + "=RAW to suppress this warning.");
            return action;
        }
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
