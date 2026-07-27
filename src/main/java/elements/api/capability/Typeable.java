package elements.api.capability;

import core.actions.ActionCapability;

import elements.api.actions.AppendTypeAction;
import elements.api.actions.ClearAction;
import elements.api.actions.TypeAction;
import elements.api.actions.TypeAndPressAction;
import elements.api.UIElement;
import elements.meta.ElementRole;

/**
 * Capability interface for text input fields (e.g., &lt;input type="text"/&gt; or textarea).
 *
 * <p>Primary locator role: {@link ElementRole#INPUT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   UIElement → Typeable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Emits deferred {@link core.actions.Action} objects for type, clear, append, and typeAndPress.
 * Contains NO execution logic. Elements emit Action (intent), engine executes.</p>
 */
public interface Typeable extends UIElement {

    /** @return fully-qualified role-suffixed locator key, e.g. {@code PageName.EnumName.CONSTANT.INPUT}. */
    default String getInputLocator() { return locatorKeyForRole(ElementRole.INPUT); }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : UIElement.super.getDisplayText();
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

    /** Emits a {@link TypeAction} — clears the field then types the given text. */
    default TypeAction type(String text) {
        return new TypeAction(this, text);
    }

    /** Emits a {@link ClearAction} — clears the field. */
    default ClearAction clear() {
        return new ClearAction(this);
    }

    /** Emits an {@link AppendTypeAction} — types without clearing first. */
    default AppendTypeAction append(String text) {
        return new AppendTypeAction(this, text);
    }

    /** Emits a {@link TypeAndPressAction} — types the text then sends {@code key} (e.g., "ENTER"). */
    default TypeAndPressAction typeAndPress(String text, String key) {
        return new TypeAndPressAction(this, text, key);
    }
}
