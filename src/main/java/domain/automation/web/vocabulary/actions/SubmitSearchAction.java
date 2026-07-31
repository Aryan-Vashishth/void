package domain.automation.web.vocabulary.actions;
import core.actions.ActionCapability;

import core.annotations.Beta;
import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.vocabulary.capability.SearchField;
import domain.automation.web.vocabulary.role.ElementRole;

/**
 * Concrete action for clicking the search/submit button of a {@link SearchField} element.
 *
 * <p>Emitted by {@code SearchField.submitSearch()}. Resolves the SEARCH_BUTTON locator,
 * then delegates to {@link UIEngine#click}.</p>
 *
 * <p>Safe profile: {@link CapabilityProfiles#TYPEABLE_SAFE} — clears field and waits for
 * visibility before; highlights after. Inherits SEARCH_FIELD capability.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class SubmitSearchAction extends TypeableElementAction {

    public SubmitSearchAction(SearchField element) {
        super(element, ElementRole.SEARCH_BUTTON, ActionCapability.SEARCH_FIELD);
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.click(descriptor);
    }
}
