package elements.api.capability;

import core.actions.ActionCapability;
import elements.api.actions.SearchAndSelectAction;
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
 *
 * <p><b>Domain ownership:</b> Web ({@code elements.api.capability}, ADR-021, I3.3).
 * Not a kernel type. The kernel references capabilities solely through
 * {@link core.actions.ActionCapability}.</p>
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
        return LocatorRoles.roleMap(
            LocatorRoles.role(ElementRole.TRIGGER,       getTriggerLocator()),
            LocatorRoles.role(ElementRole.SEARCH_INPUT,  getSearchInputLocator()),
            LocatorRoles.role(ElementRole.SEARCH_BUTTON, getSearchButtonLocator()),
            LocatorRoles.role(ElementRole.SEARCH_RESULT, getListLocator())
        );
    }

    @Override
    default ActionCapability capability() { return ActionCapability.SEARCHABLE_DROPDOWN; }

    // ── Action emission ─────────────────────────────────────────────────

    /** Emits a {@link SearchAndSelectAction} — opens trigger, types term, waits for result, clicks it. */
    default SearchAndSelectAction searchAndSelect(String term) {
        return new SearchAndSelectAction(this, term);
    }
}

