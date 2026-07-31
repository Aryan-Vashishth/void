package domain.automation.web.vocabulary.actions;
import core.actions.ActionCapability;

import core.annotations.Beta;
import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.vocabulary.capability.Selectable;
import domain.automation.web.vocabulary.role.ElementRole;

/**
 * Concrete action for selecting a dropdown option by value attribute in a {@link Selectable}.
 *
 * <p>Emitted by {@code Selectable.selectByValue(String)}. Resolves the LIST locator,
 * then delegates to {@link UIEngine#selectByValue}.</p>
 *
 * <p>Safe profile: {@link CapabilityProfiles#SELECTABLE_SAFE} — waits for visibility,
 * clickability, and Angular loader before; highlights after.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class SelectByValueAction extends SelectableElementAction {

    private final String value;

    public SelectByValueAction(Selectable element, String value) {
        super(element, ElementRole.LIST, ActionCapability.SELECTABLE);
        this.value = value;
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.selectByValue(descriptor, value);
    }
}
