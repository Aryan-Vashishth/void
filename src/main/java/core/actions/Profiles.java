package core.actions;

import core.actions.hooks.AfterActionHandler;
import core.actions.hooks.BeforeActionHandler;
import core.interactions.hooks.After;
import core.interactions.hooks.Before;

import java.util.List;
import java.util.Locale;

/**
 * Built-in action profile presets.
 *
 * <p>Only action-independent presets live here — profiles whose hook lists are the same
 * regardless of which action they are applied to. Profiles that were capability-dependent
 * ({@code SAFE}, {@code RELIABLE}) were removed in Phase 17. Their per-action behavior
 * now lives in {@link ActionProfiles} and is resolved polymorphically via
 * {@link ElementAction#defaultSafeProfile()} and {@link ElementAction#defaultReliableProfile()}.</p>
 */
public final class Profiles {

    private Profiles() {}

    public static final ActionProfile RAW = new ActionProfile() {
        @Override public String name() { return "RAW"; }
    };

    public static final ActionProfile DEBUG = new ActionProfile() {
        @Override public String name() { return "DEBUG"; }

        @Override
        public List<BeforeActionHandler> before() {
            return List.of(Before.LOG_INTENT, Before.HIGHLIGHT_ELEMENT);
        }

        @Override
        public List<AfterActionHandler> after() {
            return List.of(After.HIGHLIGHT_ELEMENT);
        }
    };

    public static final ActionProfile FAST = new ActionProfile() {
        @Override public String name() { return "FAST"; }

        @Override
        public List<AfterActionHandler> after() {
            return List.of(After.DO_NOTHING);
        }
    };

    public static final ActionProfile VISUAL = new ActionProfile() {
        @Override public String name() { return "VISUAL"; }

        @Override
        public List<BeforeActionHandler> before() {
            return List.of(Before.HIGHLIGHT_ELEMENT);
        }

        @Override
        public List<AfterActionHandler> after() {
            return List.of(After.HIGHLIGHT_ELEMENT);
        }
    };

    public static ActionProfile fromName(String name) {
        if (name == null || name.isBlank()) {
            return RAW;
        }

        return switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "DEBUG"  -> DEBUG;
            case "FAST"   -> FAST;
            case "VISUAL" -> VISUAL;
            case "RAW"    -> RAW;
            default       -> RAW;
        };
    }
}
