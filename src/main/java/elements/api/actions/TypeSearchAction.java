package elements.api.actions;
import core.actions.ActionCapability;

import core.annotations.Beta;
import elements.locator.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.SearchField;
import elements.meta.ElementRole;

/**
 * Concrete action for typing into the search input of a {@link SearchField} element.
 *
 * <p>Emitted by {@code SearchField.typeSearch(String)}. Resolves the SEARCH_INPUT locator,
 * then delegates to {@link UIEngine#type}.</p>
 *
 * <p>Safe profile: {@link CapabilityProfiles#TYPEABLE_SAFE} — clears field and waits for
 * visibility before; highlights after.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class TypeSearchAction extends TypeableElementAction {

    private final String text;

    public TypeSearchAction(SearchField element, String text) {
        super(element, ElementRole.SEARCH_INPUT, ActionCapability.SEARCH_FIELD);
        this.text = text;
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.type(descriptor, text);
    }
}
