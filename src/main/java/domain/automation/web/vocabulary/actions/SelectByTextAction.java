package domain.automation.web.vocabulary.actions;
import core.actions.ActionCapability;

import core.annotations.Beta;
import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.vocabulary.capability.Selectable;
import domain.automation.web.vocabulary.role.ElementRole;

/**
 * Concrete action for selecting a dropdown option by visible text in a {@link Selectable}.
 *
 * <p>Emitted by {@code Selectable.selectByText(String)}. Resolves the LIST locator,
 * then delegates to {@link UIEngine#selectByVisibleText}.</p>
 *
 * <p>Safe profile: {@link CapabilityProfiles#SELECTABLE_SAFE} — waits for visibility,
 * clickability, and Angular loader before; highlights after.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class SelectByTextAction extends SelectableElementAction {

    private final String text;

    public SelectByTextAction(Selectable element, String text) {
        super(element, ElementRole.LIST, ActionCapability.SELECTABLE);
        this.text = text;
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.selectByVisibleText(descriptor, text);
    }
}
