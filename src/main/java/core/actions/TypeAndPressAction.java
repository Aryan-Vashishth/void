package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Typeable;
import elements.meta.ElementRole;

/**
 * Concrete action for typing text then sending a key to a {@link Typeable} element.
 *
 * <p>Emitted by {@code Typeable.typeAndPress(String, String)}. Resolves the INPUT locator,
 * types the text, then sends the key (e.g., "ENTER", "TAB") to the same element.</p>
 *
 * <p>Safe profile: {@link ActionProfiles#TYPEABLE_SAFE} — clears field and waits for
 * visibility before; highlights after.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class TypeAndPressAction extends TypeableElementAction {

    private final String text;
    private final String key;

    public TypeAndPressAction(Typeable element, String text, String key) {
        super(element, ElementRole.INPUT, ActionCapability.TYPEABLE);
        this.text = text;
        this.key = key;
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.type(descriptor, text);
        engine.sendKey(descriptor, key);
    }
}
