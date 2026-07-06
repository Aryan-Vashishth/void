package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Typeable;
import elements.meta.ElementRole;

/**
 * Concrete action for typing text into a {@link Typeable} element.
 *
 * <p>Emitted by {@code Typeable.type(String)}. Resolves the INPUT locator, then
 * delegates to {@link UIEngine#type} (clear then type).</p>
 *
 * <p>Safe profile: {@link ActionProfiles#TYPEABLE_SAFE} — clears field and waits for
 * visibility before; highlights after.</p>
 */
@Beta(since = "2.4", note = "Phase 14 — concrete action subclass")
public final class TypeAction extends ElementAction {

    private final String text;

    public TypeAction(Typeable element, String text) {
        super(element, ElementRole.INPUT, ActionCapability.TYPEABLE);
        this.text = text;
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.type(descriptor, text);
    }
}
