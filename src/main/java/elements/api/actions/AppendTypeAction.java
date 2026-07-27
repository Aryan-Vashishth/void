package elements.api.actions;
import core.actions.ActionCapability;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Typeable;
import elements.meta.ElementRole;

/**
 * Concrete action for appending text to a {@link Typeable} element without clearing first.
 *
 * <p>Emitted by {@code Typeable.append(String)}. Resolves the INPUT locator, then
 * delegates to {@link UIEngine#appendType}.</p>
 *
 * <p>Safe profile: {@link CapabilityProfiles#TYPEABLE_SAFE} — clears field and waits for
 * visibility before; highlights after.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class AppendTypeAction extends TypeableElementAction {

    private final String text;

    public AppendTypeAction(Typeable element, String text) {
        super(element, ElementRole.INPUT, ActionCapability.TYPEABLE);
        this.text = text;
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.appendType(descriptor, text);
    }
}
