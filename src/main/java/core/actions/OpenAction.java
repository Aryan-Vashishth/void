package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Selectable;
import elements.meta.ElementRole;

/**
 * Concrete action for opening a {@link Selectable} dropdown trigger.
 *
 * <p>Emitted by {@code Selectable.open()}. Resolves the TRIGGER locator and clicks it
 * to reveal the options panel. Does not wait for the list or select an option.</p>
 *
 * <p>To open and select an option, use {@link SelectAction}, {@link SelectByTextAction},
 * or {@link SelectByValueAction}.</p>
 *
 * <p>Safe profile: {@link ActionProfiles#SELECTABLE_SAFE} — waits for visibility,
 * clickability, and Angular loader before; highlights after.</p>
 */
@Beta(since = "2.4", note = "Phase 14 — concrete action subclass")
public final class OpenAction extends ElementAction {

    public OpenAction(Selectable element) {
        super(element, ElementRole.TRIGGER, ActionCapability.SELECTABLE);
    }

    @Override
    protected ActionProfile defaultSafeProfile() {
        return ActionProfiles.SELECTABLE_SAFE;
    }

    @Override
    protected ActionProfile defaultReliableProfile() {
        return ActionProfiles.SELECTABLE_RELIABLE;
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.click(descriptor);
    }
}
