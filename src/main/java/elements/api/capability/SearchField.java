package elements.api.capability;

import core.actions.ActionCapability;
import core.actions.SubmitSearchAction;
import core.actions.TypeSearchAction;
import elements.meta.ElementRole;

/**
 * Capability interface for a composite search input + action button pair.
 *
 * <p>Roles: {@link ElementRole#SEARCH_INPUT} and {@link ElementRole#SEARCH_BUTTON}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → Typeable  ─┐
 *   Element → Clickable ─┤→ SearchField
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface SearchField extends Typeable, Clickable {

    String getSearchInputLocator();

    String getSearchButtonLocator();

    @Override
    default String getTriggerLocator() { return getSearchButtonLocator(); }

    @Override
    default String getInputLocator() { return getSearchInputLocator(); }

    @Override
    default String getDisplayText() { return Typeable.super.getDisplayText(); }

    @Override
    String getExternalFileName();

    @Override
    Object[] getArgs();

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        return LocatorRoles.roleMap(
            LocatorRoles.role(ElementRole.SEARCH_INPUT,  getSearchInputLocator()),
            LocatorRoles.role(ElementRole.SEARCH_BUTTON, getSearchButtonLocator())
        );
    }

    @Override
    default ActionCapability capability() { return ActionCapability.SEARCH_FIELD; }

    // ── Action emission ─────────────────────────────────────────────────

    /** Emits a {@link TypeSearchAction} targeting this element's SEARCH_INPUT locator. */
    default TypeSearchAction typeSearch(String text) {
        return new TypeSearchAction(this, text);
    }

    /** Emits a {@link SubmitSearchAction} targeting this element's SEARCH_BUTTON locator. */
    default SubmitSearchAction submitSearch() {
        return new SubmitSearchAction(this);
    }
}

