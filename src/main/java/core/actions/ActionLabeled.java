package core.actions;

/**
 * Package-private contract for actions that can report a human-readable
 * element name and operation label for trace output.
 *
 * <p>Implemented by {@code ElementActions.ElementBoundAction} and delegated
 * through {@code HookChainAction}. Not part of the public {@link Action} API.</p>
 */
interface ActionLabeled {

    /** Returns a display name for the target element (e.g. {@code "LOGIN_BUTTON"}). */
    String elementLabel();

    /** Returns a display name for the operation (e.g. {@code "click"}, {@code "type"}). */
    String operationLabel();
}
