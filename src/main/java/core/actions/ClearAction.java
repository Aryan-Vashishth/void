package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Typeable;
import elements.meta.ElementRole;

/**
 * Concrete action for clearing a {@link Typeable} element's input field.
 *
 * <p>Emitted by {@code Typeable.clear()}. Resolves the INPUT locator, then
 * delegates to {@link UIEngine#clear}.</p>
 *
 * <p>Safe profile: {@link ActionProfiles#TYPEABLE_SAFE} — clears field and waits for
 * visibility before; highlights after.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class ClearAction extends TypeableElementAction {

    public ClearAction(Typeable element) {
        super(element, ElementRole.INPUT, ActionCapability.TYPEABLE);
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.clear(descriptor);
    }
}
