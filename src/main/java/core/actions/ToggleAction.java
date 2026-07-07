package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Checkable;
import elements.meta.ElementRole;

/**
 * Concrete action for toggling a {@link Checkable} element (unconditional click).
 *
 * <p>Emitted by {@code Checkable.toggle()}. Resolves the TRIGGER locator, then clicks
 * regardless of the current checkbox state.</p>
 *
 * <p>To set a checkbox to a specific state, use {@link CheckAction} instead.</p>
 *
 * <p>Safe profile: {@link ActionProfiles#CLICKABLE_SAFE} — waits for clickability before,
 * waits for Angular loader and highlights after.</p>
 */
@Beta(since = "2.4", note = "Phase 14 — concrete action subclass")
public final class ToggleAction extends ElementAction {

    public ToggleAction(Checkable element) {
        super(element, ElementRole.TRIGGER, ActionCapability.CHECKABLE);
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
        engine.click(descriptor);
    }
}
