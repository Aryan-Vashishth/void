package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.SearchField;
import elements.meta.ElementRole;

/**
 * Concrete action for clicking the search/submit button of a {@link SearchField} element.
 *
 * <p>Emitted by {@code SearchField.submitSearch()}. Resolves the SEARCH_BUTTON locator,
 * then delegates to {@link UIEngine#click}.</p>
 *
 * <p>Safe profile: {@link ActionProfiles#TYPEABLE_SAFE} — clears field and waits for
 * visibility before; highlights after. Inherits SEARCH_FIELD capability.</p>
 */
@Beta(since = "2.4", note = "Phase 14 — concrete action subclass")
public final class SubmitSearchAction extends ElementAction {

    public SubmitSearchAction(SearchField element) {
        super(element, ElementRole.SEARCH_BUTTON, ActionCapability.SEARCH_FIELD);
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
        engine.click(descriptor);
    }
}
