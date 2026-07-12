package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Hoverable;
import elements.meta.ElementRole;

/**
 * Concrete action for hovering over a {@link Hoverable} element.
 *
 * <p>Emitted by {@code Hoverable.hover()}. Resolves the TEXT locator, then delegates
 * to {@link UIEngine#hover}.</p>
 *
 * <p>Safe profile: {@link ActionProfiles#DEFAULT_SAFE} — waits for element visibility before.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class HoverAction extends ElementAction {

    public HoverAction(Hoverable element) {
        super(element, ElementRole.TEXT, ActionCapability.HOVERABLE);
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.hover(descriptor);
    }
}
