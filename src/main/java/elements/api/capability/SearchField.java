package elements.api.capability;

import core.actions.Action;
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
    default String getPrimaryLocator() { return Typeable.super.getPrimaryLocator(); }

    @Override
    default String getDisplayText() { return Typeable.super.getDisplayText(); }

    @Override
    String getExternalFileName();

    @Override
    Object[] getArgs();

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String input = getSearchInputLocator();
        if (input != null && !input.isBlank()) roles.put(ElementRole.SEARCH_INPUT, input);
        String btn = getSearchButtonLocator();
        if (btn != null && !btn.isBlank() && !btn.equals(input)) roles.put(ElementRole.SEARCH_BUTTON, btn);
        return roles;
    }

    // ── Action emission ─────────────────────────────────────────────────

    /** Types into the search input field. */
    default Action typeSearch(String text) {
        return engine -> {
            var d = engine.resolve(this, ElementRole.SEARCH_INPUT);
            engine.type(d, text);
        };
    }

    /** Clicks the search/submit button. */
    default Action submitSearch() {
        return engine -> {
            var d = engine.resolve(this, ElementRole.SEARCH_BUTTON);
            engine.click(d);
        };
    }
}

