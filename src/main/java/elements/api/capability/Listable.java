package elements.api.capability;

import core.actions.ActionCapability;

import elements.api.UIElement;
import elements.meta.ElementRole;

/**
 * Capability interface for list/collection containers (UL/OL, options panel, card deck).
 *
 * <p>Role: {@link ElementRole#LIST}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   UIElement → Listable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 *
 * <p><b>Domain ownership:</b> Web ({@code elements.api.capability}, ADR-021, I3.3).
 * Not a kernel type. The kernel references capabilities solely through
 * {@link core.actions.ActionCapability}.</p>
 */
public interface Listable extends UIElement {

    /** @return fully-qualified role-suffixed locator key, e.g. {@code PageName.EnumName.CONSTANT.LIST}. */
    default String getListLocator() { return locatorKeyForRole(ElementRole.LIST); }

    /**
     * Returns the zero-based index of this element in its parent list.
     *
     * <p>Default: delegates to the enum ordinal. Non-enum implementors that lack ordinal
     * semantics must override explicitly -- the default throws rather than silently
     * returning {@code 0} to avoid undetected wrong offsets.</p>
     */
    default int getIndex() {
        if (this instanceof Enum<?> en) return en.ordinal();
        throw new UnsupportedOperationException(
            getClass().getSimpleName() +
            " implements Listable but has no ordinal semantics. Override Listable.getIndex().");
    }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : UIElement.super.getDisplayText();
    }

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>();
        String list = getListLocator();
        if (list != null && !list.isBlank()) roles.put(ElementRole.LIST, list);
        return roles;
    }

    @Override
    default ActionCapability capability() { return ActionCapability.LISTABLE; }

}

