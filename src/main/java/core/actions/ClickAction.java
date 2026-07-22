package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Clickable;
import elements.meta.ElementRole;

/**
 * Concrete action for clicking a {@link Clickable} element.
 *
 * <p>Implements the Template Method pattern: {@code perform()} resolves the TRIGGER
 * locator, then {@code execute()} delegates to {@link UIEngine#click}.</p>
 *
 * <p>Safe profile: {@link ActionProfiles#CLICKABLE_SAFE} — waits for clickability before,
 * waits for Angular loader and highlights after.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class ClickAction extends ClickableElementAction {

    public ClickAction(Clickable element) {
        super(element, ElementRole.TRIGGER, ActionCapability.CLICKABLE);
    }

    @Override
    public String operationLabel() { return "click"; }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.click(descriptor);
    }
}
