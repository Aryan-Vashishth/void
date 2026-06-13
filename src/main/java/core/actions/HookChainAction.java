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
final class HookChainAction implements Action {

    private final Action delegate;
    private final List<ActionHandler> before;
    private final List<ActionHandler> after;

    HookChainAction(Action delegate,
                    @Nullable List<ActionHandler> before,
                    @Nullable List<ActionHandler> after) {
        this.delegate = Objects.requireNonNull(delegate, "delegate action must not be null");
        this.before = normalize(before);
        this.after = normalize(after);
    }

    HookChainAction withAdditionalHooks(@Nullable List<ActionHandler> additionalBefore,
                                        @Nullable List<ActionHandler> additionalAfter) {
        return new HookChainAction(
                delegate,
                concat(before, additionalBefore),
                concat(after, additionalAfter)
        );
    }

    @Override
    public void perform(UIEngine engine) {
        LocatorDescriptor descriptor = delegate.resolve(engine);
        new HookedAction(delegate, descriptor, before, after).perform(engine);
    }

    @Override
    public LocatorDescriptor resolve(UIEngine engine) {
        return delegate.resolve(engine);
    }

    @Override
    public ActionCapability capability() {
        return delegate.capability();
    }

    private static List<ActionHandler> normalize(@Nullable List<ActionHandler> hooks) {
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
                                               @Nullable List<ActionHandler> additional) {
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

