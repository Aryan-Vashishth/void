package elements.api.capability;

import core.actions.Action;
import core.actions.ElementActions;
import elements.meta.ElementRole;

/**
 * Capability interface for checkbox elements (clickable + checkable).
 *
 * <p>Extends {@link Clickable}. Inherits TRIGGER role.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → Clickable → Checkable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface Checkable extends Clickable {

    // ── Action emission ─────────────────────────────────────────────────

    /** Toggles the checkbox (click). */
    default Action toggle() {
        return ElementActions.of(this, ElementRole.TRIGGER,
                (engine, d) -> engine.click(d));
    }

    /** Sets the checkbox to the desired state. Reads current state, clicks only if needed. */
    default Action set(boolean desiredState) {
        return ElementActions.of(this, ElementRole.TRIGGER,
                (engine, d) -> {
                    if (engine.getCheckboxState(d) != desiredState) {
                        engine.click(d);
                    }
                });
    }
}
