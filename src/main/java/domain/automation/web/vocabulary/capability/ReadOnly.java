package domain.automation.web.vocabulary.capability;

import core.actions.ActionCapability;

import domain.automation.web.vocabulary.actions.ReadTextAction;
import domain.automation.web.vocabulary.element.UIElement;
import domain.automation.web.vocabulary.role.ElementRole;

/**
 * Capability interface for non-interactive text/display elements (label, span, static cell).
 *
 * <p>Primary locator role: {@link ElementRole#TEXT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   UIElement → ReadOnly
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 *
 * <p><b>Domain ownership:</b> Web ({@code elements.api.capability}, ADR-021, I3.3).
 * Not a kernel type. The kernel references capabilities solely through
 * {@link core.actions.ActionCapability}.</p>
 */
public interface ReadOnly extends UIElement {

    /** @return fully-qualified role-suffixed locator key, e.g. {@code PageName.EnumName.CONSTANT.TEXT}. */
    default String getTextLocator() { return locatorKeyForRole(ElementRole.TEXT); }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : UIElement.super.getDisplayText();
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
    default ReadTextAction getText() {
        return new ReadTextAction(this);
    }

    /**
     * @deprecated since 0.9 -- use {@link #getText()} instead.
     *             Will be removed in 1.0.
     */
    @Deprecated(since = "0.9", forRemoval = true)
    default ReadTextAction readText() {
        return getText();
    }
}
