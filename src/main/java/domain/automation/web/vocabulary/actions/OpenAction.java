package domain.automation.web.vocabulary.actions;
import core.actions.ActionCapability;

import core.annotations.Beta;
import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.vocabulary.capability.Selectable;
import domain.automation.web.vocabulary.role.ElementRole;

/**
 * Concrete action for opening a {@link Selectable} dropdown trigger.
 *
 * <p>Emitted by {@code Selectable.open()}. Resolves the TRIGGER locator and clicks it
 * to reveal the options panel. Does not wait for the list or select an option.</p>
 *
 * <p>To open and select an option, use {@link SelectAction}, {@link SelectByTextAction},
 * or {@link SelectByValueAction}.</p>
 *
 * <p>Safe profile: {@link CapabilityProfiles#SELECTABLE_SAFE} — waits for visibility,
 * clickability, and Angular loader before; highlights after.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class OpenAction extends SelectableElementAction {

    public OpenAction(Selectable element) {
        super(element, ElementRole.TRIGGER, ActionCapability.SELECTABLE);
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.click(descriptor);
    }
}
