package domain.automation.web.vocabulary.actions;
import core.actions.ActionCapability;

import core.annotations.Beta;
import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.vocabulary.capability.SearchableDropdown;
import domain.automation.web.vocabulary.role.ElementRole;

import java.time.Duration;

/**
 * Concrete action for searching and selecting an option in a {@link SearchableDropdown}.
 *
 * <p>Emitted by {@code SearchableDropdown.searchAndSelect(String)}. Composite action:
 * <ol>
 *   <li>Clicks the TRIGGER locator to open the dropdown</li>
 *   <li>Types the search term into the SEARCH_INPUT locator</li>
 *   <li>Waits for the SEARCH_RESULT locator to become visible</li>
 *   <li>Clicks the matched SEARCH_RESULT</li>
 * </ol>
 * </p>
 *
 * <p>Safe profile: {@link CapabilityProfiles#SELECTABLE_SAFE} — waits for visibility,
 * clickability, and Angular loader before; highlights after.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class SearchAndSelectAction extends SelectableElementAction {

    private final String term;

    public SearchAndSelectAction(SearchableDropdown element, String term) {
        super(element, ElementRole.TRIGGER, ActionCapability.SEARCHABLE_DROPDOWN);
        this.term = term;
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.click(descriptor);
        engine.type(engine.resolve(element, ElementRole.SEARCH_INPUT), term);
        LocatorDescriptor result = engine.resolve(element, ElementRole.SEARCH_RESULT, term);
        engine.waitForVisible(result, Duration.ofSeconds(10));
        engine.click(result);
    }
}
