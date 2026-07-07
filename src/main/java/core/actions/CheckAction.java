package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Checkable;
import elements.meta.ElementRole;

/**
 * Concrete action for setting a {@link Checkable} element to a desired state.
 *
 * <p>Emitted by {@code Checkable.set(boolean)}. Reads the current checkbox state;
 * clicks only if the current state differs from {@code desiredState}. This is a
 * conditional click — idempotent when the element is already in the target state.</p>
 *
 * <p>To toggle unconditionally, use {@link ToggleAction} instead.</p>
 *
 * <p>Safe profile: {@link ActionProfiles#CLICKABLE_SAFE} — waits for clickability before,
 * waits for Angular loader and highlights after.</p>
 */
@Beta(since = "2.4", note = "Phase 14 — concrete action subclass")
public final class CheckAction extends ElementAction {

    private final boolean desiredState;

    public CheckAction(Checkable element, boolean desiredState) {
        super(element, ElementRole.TRIGGER, ActionCapability.CHECKABLE);
        this.desiredState = desiredState;
    }

    @Override
    protected ActionProfile defaultSafeProfile() {
        return ActionProfiles.CLICKABLE_SAFE;
    }

    @Override
    protected ActionProfile defaultReliableProfile() {
        return ActionProfiles.CLICKABLE_RELIABLE;
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        if (engine.getCheckboxState(descriptor) != desiredState) {
            engine.click(descriptor);
        }
    }
}
