package core.actions;

import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import core.interactions.hooks.ActionHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Internal action wrapper that stores composable before/after hooks.
 */
final class HookChainAction implements Action, ActionLabeled {

    private final Action delegate;
    private final List<ActionHandler> before;
    private final List<ActionHandler> after;
    @Nullable private final String profileName;

    HookChainAction(Action delegate,
                    @Nullable List<? extends ActionHandler> before,
                    @Nullable List<? extends ActionHandler> after) {
        this(delegate, before, after, null);
    }

    private HookChainAction(Action delegate,
                            @Nullable List<? extends ActionHandler> before,
                            @Nullable List<? extends ActionHandler> after,
                            @Nullable String profileName) {
        this.delegate    = Objects.requireNonNull(delegate, "delegate action must not be null");
        this.before      = normalize(before);
        this.after       = normalize(after);
        this.profileName = profileName;
    }

    @Override
    public Action mergeHooks(List<? extends ActionHandler> additionalBefore,
                             List<? extends ActionHandler> additionalAfter) {
        return new HookChainAction(
                delegate,
                concat(before, additionalBefore),
                concat(after, additionalAfter),
                profileName
        );
    }

    @Override
    public Action withProfile(ActionProfile profile) {
        return withProfileName(profile.name());
    }

    HookChainAction withProfileName(String name) {
        return new HookChainAction(delegate, before, after, name);
    }

    @Override
    public void perform(UIEngine engine) {
        LocatorDescriptor descriptor = delegate.resolve(engine);
        new HookedAction(delegate, descriptor, before, after, profileName).perform(engine);
    }

    @Override
    public String elementLabel() {
        if (delegate instanceof ActionLabeled l) return l.elementLabel();
        return "ACTION";
    }

    @Override
    public String operationLabel() {
        if (delegate instanceof ActionLabeled l) return l.operationLabel();
        return switch (capability()) {
            case CLICKABLE  -> "click";
            case TYPEABLE   -> "type";
            case SELECTABLE -> "select";
            default         -> "perform";
        };
    }

    @Override
    public LocatorDescriptor resolve(UIEngine engine) {
        return delegate.resolve(engine);
    }

    @Override
    public ActionCapability capability() {
        return delegate.capability();
    }

    private static List<ActionHandler> normalize(@Nullable List<? extends ActionHandler> hooks) {
        if (hooks == null || hooks.isEmpty()) {
            return List.of();
        }

        List<ActionHandler> normalized = new ArrayList<>(hooks.size());
        for (ActionHandler hook : hooks) {
            if (hook != null) {
                normalized.add(hook);
            }
        }

        return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }

    private static List<ActionHandler> concat(List<ActionHandler> existing,
                                               @Nullable List<? extends ActionHandler> additional) {
        if (additional == null || additional.isEmpty()) {
            return existing;
        }

        List<ActionHandler> merged = new ArrayList<>(existing.size() + additional.size());
        merged.addAll(existing);
        for (ActionHandler hook : additional) {
            if (hook != null) {
                merged.add(hook);
            }
        }

        return merged.isEmpty() ? List.of() : List.copyOf(merged);
    }
}

