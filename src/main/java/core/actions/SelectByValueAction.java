package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Selectable;
import elements.meta.ElementRole;

/**
 * Concrete action for selecting a dropdown option by value attribute in a {@link Selectable}.
 *
 * <p>Emitted by {@code Selectable.selectByValue(String)}. Resolves the LIST locator,
 * then delegates to {@link UIEngine#selectByValue}.</p>
 *
 * <p>Safe profile: {@link ActionProfiles#SELECTABLE_SAFE} — waits for visibility,
 * clickability, and Angular loader before; highlights after.</p>
 */
@Beta(since = "2.4", note = "Phase 14 — concrete action subclass")
public final class SelectByValueAction extends ElementAction {

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
