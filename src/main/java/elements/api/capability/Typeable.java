package elements.api.capability;

import core.actions.Action;
import core.actions.ActionCapability;
import core.actions.ActionCapabilityProvider;
import core.actions.ElementActions;
import elements.api.Element;
import elements.meta.ElementRole;

/**
 * Capability interface for text input fields (e.g., &lt;input type="text"/&gt; or textarea).
 *
 * <p>Primary locator role: {@link ElementRole#INPUT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → Typeable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Emits deferred {@link Action} objects for type, clear, append, and typeAndPress.
 * Contains NO execution logic. Elements emit Action (intent), engine executes.</p>
 */
public interface Typeable extends Element, ActionCapabilityProvider {

    String getInputLocator();

    @Override
    default String getPrimaryLocator() { return getInputLocator(); }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : getInputLocator();
    }

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String input = getInputLocator();
        if (input != null && !input.isBlank()) roles.put(ElementRole.INPUT, input);
        return roles;
    }

    @Override
    default ActionCapability capability() { return ActionCapability.TYPEABLE; }

    // ── Action emission ─────────────────────────────────────────────────

    /** Deferred type action — clears then types. */
    default Action type(String text) {
        return ElementActions.of(this, ElementRole.INPUT,
                (engine, d) -> engine.type(d, text));
    }

    /** Deferred clear action. */
    default Action clear() {
        return ElementActions.of(this, ElementRole.INPUT,
                (engine, d) -> engine.clear(d));
    }

    /** Deferred append action — types without clearing. */
    default Action append(String text) {
        return ElementActions.of(this, ElementRole.INPUT,
                (engine, d) -> engine.appendType(d, text));
    }

    /** Deferred type-and-press action — types then sends a key (e.g., "ENTER"). */
    default Action typeAndPress(String text, String key) {
        return ElementActions.of(this, ElementRole.INPUT,
                (engine, d) -> { engine.type(d, text); engine.sendKey(d, key); });
    }
}
