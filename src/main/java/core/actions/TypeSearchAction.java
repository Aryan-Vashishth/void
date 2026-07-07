package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.SearchField;
import elements.meta.ElementRole;

/**
 * Concrete action for typing into the search input of a {@link SearchField} element.
 *
 * <p>Emitted by {@code SearchField.typeSearch(String)}. Resolves the SEARCH_INPUT locator,
 * then delegates to {@link UIEngine#type}.</p>
 *
 * <p>Safe profile: {@link ActionProfiles#TYPEABLE_SAFE} — clears field and waits for
 * visibility before; highlights after.</p>
 */
@Beta(since = "2.4", note = "Phase 14 — concrete action subclass")
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
