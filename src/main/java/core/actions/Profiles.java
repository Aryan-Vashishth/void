package core.actions;

import core.interactions.hooks.After;
import core.interactions.hooks.AfterActionHandler;
import core.interactions.hooks.Before;
import core.interactions.hooks.BeforeActionHandler;

import java.util.List;
import java.util.Locale;

/**
 * Built-in action profile presets.
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

    public static final ActionProfile SAFE = new ActionProfile() {
        @Override public String name() { return "SAFE"; }
        @Override
        public List<BeforeActionHandler> before(Action action) {
            return switch (action.capability()) {
                case TYPEABLE -> List.of(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE);
                case SELECTABLE -> List.of(
                        Before.WAIT_FOR_ELEMENT_VISIBLE,
                        Before.WAIT_FOR_ELEMENT_CLICKABLE,
                        Before.WAIT_FOR_ANGULAR_LOADER);
                case CLICKABLE -> List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE);
                default -> List.of(Before.WAIT_FOR_ELEMENT_VISIBLE);
            };
        }

        @Override
        public List<AfterActionHandler> after(Action action) {
            return switch (action.capability()) {
                case CLICKABLE -> List.of(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT);
                default -> List.of(After.HIGHLIGHT_ELEMENT);
            };
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

    public static final ActionProfile RELIABLE = new ActionProfile() {
        @Override public String name() { return "RELIABLE"; }
        @Override
        public List<BeforeActionHandler> before(Action action) {
            return switch (action.capability()) {
                case TYPEABLE -> List.of(
                        Before.WAIT_FOR_ANGULAR_LOADER,
                        Before.WAIT_FOR_ELEMENT_VISIBLE,
                        Before.CLEAR_FIELD);
                case SELECTABLE -> List.of(
                        Before.WAIT_FOR_ANGULAR_LOADER,
                        Before.WAIT_FOR_ELEMENT_VISIBLE,
                        Before.WAIT_FOR_ELEMENT_CLICKABLE);
                case CLICKABLE -> List.of(
                        Before.WAIT_FOR_ANGULAR_LOADER,
                        Before.WAIT_FOR_ELEMENT_CLICKABLE);
                default -> List.of(Before.WAIT_FOR_ELEMENT_VISIBLE);
            };
        }

        @Override
        public List<AfterActionHandler> after(Action action) {
            return List.of(
                    After.WAIT_FOR_ANGULAR_LOADER,
                    After.WAIT_FOR_SPIN_SPINNER_LOADER,
                    After.HIGHLIGHT_ELEMENT);
        }
    };

    public static ActionProfile fromName(String name) {
        if (name == null || name.isBlank()) {
            return RAW;
        }

        return switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "SAFE" -> SAFE;
            case "DEBUG" -> DEBUG;
            case "FAST" -> FAST;
            case "VISUAL" -> VISUAL;
            case "RELIABLE" -> RELIABLE;
            case "RAW" -> RAW;
            default -> RAW;
        };
    }
}

