package elements.api.capability;

import core.actions.ActionCapability;
import core.actions.CheckAction;
import core.actions.ToggleAction;
import elements.meta.ElementRole;

/**
 * Capability interface for checkbox elements (clickable + checkable).
 *
 * <p>Extends {@link Clickable}. Inherits TRIGGER role.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   UIElement → Clickable → Checkable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface Checkable extends Clickable {

    @Override
    default ActionCapability capability() { return ActionCapability.CHECKABLE; }

    // ── Action emission ─────────────────────────────────────────────────

    /** Emits a {@link ToggleAction} — clicks the checkbox unconditionally. */
    default ToggleAction toggle() {
        return new ToggleAction(this);
    }

    /** Emits a {@link CheckAction} — clicks only if the current state differs from {@code desiredState}. */
    default CheckAction set(boolean desiredState) {
        return new CheckAction(this, desiredState);
    }
}
