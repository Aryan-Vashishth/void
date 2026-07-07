package core.actions;

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
 * <p>Safe profile: {@link ActionProfiles#TYPEABLE_SAFE} — clears field and waits for
 * visibility before; highlights after.</p>
 */
@Beta(since = "2.4", note = "Phase 14 — concrete action subclass")
public final class AppendTypeAction extends ElementAction {

    private final String text;

    public AppendTypeAction(Typeable element, String text) {
        super(element, ElementRole.INPUT, ActionCapability.TYPEABLE);
        this.text = text;
    }

    @Override
    protected ActionProfile defaultSafeProfile() {
        return ActionProfiles.TYPEABLE_SAFE;
    }

    @Override
    protected ActionProfile defaultReliableProfile() {
        return ActionProfiles.TYPEABLE_RELIABLE;
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.appendType(descriptor, text);
    }
}
