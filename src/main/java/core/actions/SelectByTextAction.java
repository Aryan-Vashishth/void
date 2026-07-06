package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Selectable;
import elements.meta.ElementRole;

/**
 * Concrete action for selecting a dropdown option by visible text in a {@link Selectable}.
 *
 * <p>Emitted by {@code Selectable.selectByText(String)}. Resolves the LIST locator,
 * then delegates to {@link UIEngine#selectByVisibleText}.</p>
 *
 * <p>Safe profile: {@link ActionProfiles#SELECTABLE_SAFE} — waits for visibility,
 * clickability, and Angular loader before; highlights after.</p>
 */
@Beta(since = "2.4", note = "Phase 14 — concrete action subclass")
public final class SelectByTextAction extends ElementAction {

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
