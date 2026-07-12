package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import core.interactions.hooks.ActionHandler;
import core.interactions.hooks.AfterActionHandler;
import core.interactions.hooks.BeforeActionHandler;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Single execution contract — a UI operation deferred for Flow-based execution.
 *
 * <p>An Action is produced by element capability interfaces (e.g., {@code element.click()},
 * {@code element.type("text")}) and executed later by a {@link core.executor.FlowExecutor}.
 * Both locator resolution and browser execution are <b>deferred</b> until
 * {@link #perform(UIEngine)} is called.</p>
 *
 * <h3>Single execution path</h3>
 * <pre>
 *   Element → Action → Flow → FlowExecutor → UIEngine
 * </pre>
 *
 * <h3>Hook support</h3>
 * <p>Actions created via {@link ElementActions} support fluent
 * {@link #before(BeforeActionHandler...)} / {@link #after(AfterActionHandler...)}
 * composition:</p>
 * <pre>
 *   LoginPage.USERNAME.type("user")
 *       .before(Before.CLEAR_FIELD)
 *       .after(After.HIGHLIGHT_ELEMENT);
 * </pre>
 *
 * <p><b>Rules:</b></p>
 * <ul>
 *   <li>Resolve locators <b>inside</b> perform (deferred, not eager).</li>
 *   <li>Never reference {@code WebDriver}, {@code WebElement}, or {@code By}.</li>
 *   <li>Action = deferred execution intent. Engine = smart executor.</li>
 * </ul>
 */
@Beta(since = "0.1", note = "Action/Flow/FlowExecutor pipeline is evolving — API may change")
@FunctionalInterface
public interface Action {

    /**
     * Executes this action against the given engine.
     *
     * @param engine the UI engine that performs the actual browser interaction
     */
    void perform(UIEngine engine);

    /**
     * Resolves the {@link LocatorDescriptor} for this action's target element.
     *
     * <p>Override in element-bound actions (via {@link ElementActions}) to enable
     * hook composition APIs. The default throws for raw lambda actions
     * that don't target a specific element.</p>
     *
     * @param engine the engine to resolve against
     * @return resolved descriptor
     * @throws UnsupportedOperationException if this action doesn't support resolution
     */
    default LocatorDescriptor resolve(UIEngine engine) {
        throw new UnsupportedOperationException(
                "This action does not support descriptor resolution. " +
                "Use a concrete ElementAction subclass (e.g., ClickAction, TypeAction).");
    }

    /**
     * Adds before-hooks to this action.
     *
     * @param hooks hooks to run before the core action
     * @return action with appended before-hooks
     */
    default Action before(@Nullable BeforeActionHandler... hooks) {
        if (this instanceof HookChainAction chain) {
            return chain.withAdditionalHooks(toList(hooks), null);
        }
        return new HookChainAction(this, toList(hooks), null);
    }

    /**
     * Adds after-hooks to this action.
     *
     * @param hooks hooks to run after the core action
     * @return action with appended after-hooks
     */
    default Action after(@Nullable AfterActionHandler... hooks) {
        if (this instanceof HookChainAction chain) {
            return chain.withAdditionalHooks(null, toList(hooks));
        }
        return new HookChainAction(this, null, toList(hooks));
    }

    /**
     * Applies the framework's SAFE profile to this action.
     *
     * <p>For {@link ElementAction} subclasses, this is overridden to use the
     * polymorphic {@code defaultSafeProfile()} path. This default applies
     * {@link ActionProfiles#DEFAULT_SAFE} (wait-for-visible) as a minimal guard
     * for plain lambda actions that do not extend {@code ElementAction}.</p>
     */
    default Action safely() {
        return using(ActionProfiles.DEFAULT_SAFE);
    }

    /**
     * Applies the framework's DEBUG profile to this action.
     */
    default Action debug() {
        return using(Profiles.DEBUG);
    }

    /**
     * Applies the framework's RAW profile (no additional hooks) to this action.
     */
    default Action raw() {
        return using(Profiles.RAW);
    }

    /**
     * Applies a named execution profile to this action.
     */
    default Action using(ActionProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");

        List<BeforeActionHandler> beforeHooks = profile.before(this);
        List<AfterActionHandler> afterHooks = profile.after(this);
        if ((beforeHooks == null || beforeHooks.isEmpty()) && (afterHooks == null || afterHooks.isEmpty())) {
            return this;
        }

        Action profiled = this;
        if (beforeHooks != null && !beforeHooks.isEmpty()) {
            profiled = profiled.before(beforeHooks.toArray(new BeforeActionHandler[0]));
        }
        if (afterHooks != null && !afterHooks.isEmpty()) {
            profiled = profiled.after(afterHooks.toArray(new AfterActionHandler[0]));
        }
        if (profiled instanceof HookChainAction chain) {
            profiled = chain.withProfileName(profile.name());
        }
        return profiled;
    }

    /**
     * Returns the capability category of this action for profile resolution.
     */
    default ActionCapability capability() {
        return ActionCapability.UNKNOWN;
    }


    /**
     * Wraps this action with before/after hooks, returning a new {@link Action}.
     *
     * <p>The descriptor is resolved once at execution time via {@link #resolve(UIEngine)}
     * and shared across all hooks and the delegate action.</p>
     *
     * <pre>
     *   LoginPage.USERNAME.type("user")
     *       .withHooks(
     *           List.of(Before.CLEAR_FIELD, Before.HIGHLIGHT_ELEMENT),
     *           List.of(After.HIGHLIGHT_ELEMENT));
     * </pre>
     *
     * @param before before-hooks (null = none)
     * @param after  after-hooks (null = none)
     * @return a new action that runs: before hooks → this → after hooks
     * @deprecated Prefer fluent directional APIs:
     *             {@code action.before(...).after(...)}
     */
    @Deprecated(forRemoval = false, since = "0.1")
    default Action withHooks(@Nullable List<ActionHandler> before,
                             @Nullable List<ActionHandler> after) {
        if (this instanceof HookChainAction chain) {
            return chain.withAdditionalHooks(before, after);
        }
        return new HookChainAction(this, before, after);
    }

    private static List<ActionHandler> toList(@Nullable ActionHandler... hooks) {
        if (hooks == null || hooks.length == 0) {
            return List.of();
        }
        return Arrays.asList(hooks);
    }
}
