package elements.api.actions;
import core.actions.ActionCapability;

import core.annotations.Beta;
import elements.locator.LocatorDescriptor;
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
 * <p>Safe profile: {@link CapabilityProfiles#CLICKABLE_SAFE} — waits for clickability before,
 * waits for Angular loader and highlights after.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class ToggleAction extends ClickableElementAction {

    public ToggleAction(Checkable element) {
        super(element, ElementRole.TRIGGER, ActionCapability.CHECKABLE);
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.click(descriptor);
    }
}
