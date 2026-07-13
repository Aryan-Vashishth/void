package elements.api.capability;

import core.actions.ActionCapability;
import core.actions.ActionCapabilityProvider;
import core.actions.ReadTextAction;
import elements.api.Element;
import elements.meta.ElementRole;

/**
 * Capability interface for non-interactive text/display elements (label, span, static cell).
 *
 * <p>Primary locator role: {@link ElementRole#TEXT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ReadOnly
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface ReadOnly extends Element, ActionCapabilityProvider {

    default String getTextLocator() { return ((Enum<?>) this).name(); }

    @Override
    default String getPrimaryLocator() { return getTextLocator(); }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getTextLocator();
    }

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String text = getTextLocator();
        if (text != null && !text.isBlank()) roles.put(ElementRole.TEXT, text);
        return roles;
    }

    @Override
    default ActionCapability capability() { return ActionCapability.READ_ONLY; }

    // ── Action emission ─────────────────────────────────────────────────

    /** Reads the visible text of this element. Engine handles scroll internally. */
    default ReadTextAction readText() {
        return new ReadTextAction(this);
    }
}
