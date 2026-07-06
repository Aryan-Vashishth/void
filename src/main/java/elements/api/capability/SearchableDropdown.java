package elements.api.capability;

import core.actions.ActionCapability;
import core.actions.SearchAndSelectAction;
import elements.meta.ElementRole;

/**
 * Capability interface for searchable dropdowns (trigger + search input + button + result list).
 *
 * <p>Roles: {@link ElementRole#TRIGGER}, {@link ElementRole#SEARCH_INPUT},
 * {@link ElementRole#SEARCH_BUTTON}, {@link ElementRole#SEARCH_RESULT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Selectable ─┐
 *   Searchable ─┤→ SearchableDropdown
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface SearchableDropdown extends Selectable, Searchable {

    @Override
    String getSearchInputLocator();

    @Override
    String getSearchButtonLocator();

    @Override
    String getTriggerLocator();

    @Override
    String getSearchResultLocator();

    @Override
    default String getInputLocator() { return getSearchInputLocator(); }

    @Override
    default String getListLocator() { return getSearchResultLocator(); }

    @Override
    default String getPrimaryLocator() { return Selectable.super.getPrimaryLocator(); }

    @Override
    default String getSecondaryLocator() { return Selectable.super.getSecondaryLocator(); }

    @Override
    default String getDisplayText() { return Selectable.super.getDisplayText(); }

    @Override
    default int getIndex() { return Selectable.super.getIndex(); }

    @Override
    String getExternalFileName();

    @Override
    Object[] getArgs();

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String trigger = getTriggerLocator();
        if (trigger != null && !trigger.isBlank()) roles.put(ElementRole.TRIGGER, trigger);
        String input = getSearchInputLocator();
        if (input != null && !input.isBlank() && !input.equals(trigger)) roles.put(ElementRole.SEARCH_INPUT, input);
        String button = getSearchButtonLocator();
        if (button != null && !button.isBlank() && !button.equals(trigger) && !button.equals(input)) roles.put(ElementRole.SEARCH_BUTTON, button);
        String list = getListLocator();
        if (list != null && !list.isBlank() && !list.equals(trigger) && !list.equals(input) && !list.equals(button)) roles.put(ElementRole.SEARCH_RESULT, list);
        return roles;
    }

    @Override
    default ActionCapability capability() { return ActionCapability.SEARCHABLE_DROPDOWN; }

    // ── Action emission ─────────────────────────────────────────────────

    /** Emits a {@link SearchAndSelectAction} — opens trigger, types term, waits for result, clicks it. */
    default SearchAndSelectAction searchAndSelect(String term) {
        return new SearchAndSelectAction(this, term);
    }
}

