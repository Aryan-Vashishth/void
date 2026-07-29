package core.actions;

import core.actions.hooks.ActionHandler;
import core.actions.hooks.AfterActionHandler;
import core.actions.hooks.BeforeActionHandler;
import core.annotations.Beta;
import core.engine.Executor;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;

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
 *   Target → Action → Flow → FlowExecutor → UIEngine        (kernel-neutral)
 *   UIElement (extends Target) → Action → ...               (Web/UI domain)
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
     * Executes this action against the given executor.
     *
     * @param executor the execution context that performs the actual interaction
     */
    void perform(Executor executor);

    /**
     * Resolves the {@link LocatorDescriptor} for this action's target element.
     *
     * <p>Override in element-bound actions (via {@link ElementActions}) to enable
     * hook composition APIs. The default throws for raw lambda actions
     * that don't target a specific element.</p>
     *
     * @param executor the executor to resolve against
     * @return resolved descriptor
     * @throws UnsupportedOperationException if this action doesn't support resolution
     */
    default LocatorDescriptor resolve(Executor executor) {
        throw new UnsupportedOperationException(
                "This action does not support descriptor resolution. " +
                "Use a concrete ElementAction subclass (e.g., ClickAction, TypeAction).");
    }

    /**
     * @deprecated Use {@link #perform(Executor)} instead.
     *             Bridge overload; delegates to the primary. Scheduled for deletion in I9.4.
     */
    @Deprecated(since = "0.5", forRemoval = true)
    default void perform(UIEngine engine) {
        perform((Executor) engine);
    }

    /**
     * @deprecated Use {@link #resolve(Executor)} instead.
     *             Bridge overload; delegates to the primary. Scheduled for deletion in I9.4.
     */
    @Deprecated(since = "0.5", forRemoval = true)
    default LocatorDescriptor resolve(UIEngine engine) {
        return resolve((Executor) engine);
    }

    /**
     * Extension hook: merges before/after hook lists into this action.
     *
     * <p>The default wraps this action in a new {@link HookChainAction}. Composable
     * wrappers (e.g. {@code RetryAction}) override this to re-apply themselves around
     * the merged result so their own state is preserved. Overrides must delegate first
     * then re-wrap: {@code new RetryAction(delegate.mergeHooks(b, a), retryCount)}.</p>
     *
     * <p>Framework consumers should use {@link #before}/{@link #after}/{@link #withHooks}
     * rather than calling this directly.</p>
     */
    default Action mergeHooks(List<? extends ActionHandler> before,
                              List<? extends ActionHandler> after) {
        return new HookChainAction(this, before, after);
    }

    /**
     * Extension hook: attaches a profile name to this action.
     *
     * <p>The default wraps this action in a new {@link HookChainAction} carrying the
     * profile name. {@link HookChainAction} overrides this to set the name on itself
     * without adding another wrapper layer.</p>
     *
     * <p>Framework consumers should use {@link #using} rather than calling this directly.</p>
     */
    default Action withProfile(ActionProfile profile) {
        return new HookChainAction(this, List.of(), List.of())
                .withProfileName(profile.name());
    }

    /**
     * Adds before-hooks to this action.
     *
     * @param hooks hooks to run before the core action
     * @return action with appended before-hooks
     */
    default Action before(@Nullable BeforeActionHandler... hooks) {
        return mergeHooks(toList(hooks), List.of());
    }

    /**
     * Adds after-hooks to this action.
     *
     * @param hooks hooks to run after the core action
     * @return action with appended after-hooks
     */
    default Action after(@Nullable AfterActionHandler... hooks) {
        return mergeHooks(List.of(), toList(hooks));
    }

    /**
     * Applies the framework's SAFE profile to this action.
     *
     * <p>For {@link ElementAction} subclasses, this is overridden to use the
     * polymorphic {@code defaultSafeProfile()} path. For any action, calling
     * {@code safely()} on an action with {@link ActionCapability#UNKNOWN} capability
     * throws {@link IllegalStateException}: the runtime cannot select browser-wait
     * hooks for a capability it does not recognise. Declare a specific capability
     * or call {@link #raw()} instead (runtime-redesign I3.2).</p>
     */
    default Action safely() {
        if (ActionCapability.UNKNOWN.equals(capability())) {
            throw new IllegalStateException(
                "safely() cannot select hooks for an action with UNKNOWN capability. " +
                "Declare a specific ActionCapability via ActionCapability.of(), " +
                "or call .raw() for an action that requires no browser-wait contract.");
        }
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

        return mergeHooks(
                beforeHooks != null ? beforeHooks : List.of(),
                afterHooks  != null ? afterHooks  : List.of()
        ).withProfile(profile);
    }

    /**
     * Returns the capability category of this action for profile resolution.
     */
    default ActionCapability capability() {
        return ActionCapability.UNKNOWN;
    }

    /**
     * Returns a display name for the target element (for trace/logging output).
     *
     * <p>Overridden by {@link ElementAction} to return the enum constant name or class simple name.
     * Lambda actions return {@code "action"} as a neutral fallback.</p>
     */
    default String elementLabel() {
        return "action";
    }

    /**
     * Returns a display name for the operation (for trace/logging output).
     *
     * <p>Overridden by {@link ElementAction} to derive the label from the concrete class name.
     * Lambda actions return {@code "perform"} as a neutral fallback.</p>
     */
    default String operationLabel() {
        return "perform";
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
        return mergeHooks(
                before != null ? before : List.of(),
                after  != null ? after  : List.of());
    }

    private static List<ActionHandler> toList(@Nullable ActionHandler... hooks) {
        if (hooks == null || hooks.length == 0) {
            return List.of();
        }
        return Arrays.asList(hooks);
    }
}
