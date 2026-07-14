package elements.api.capability;

import core.actions.ActionCapability;
import core.actions.ActionCapabilityProvider;
import elements.api.Element;
import elements.meta.ElementRole;

/**
 * Capability interface for list/collection containers (UL/OL, options panel, card deck).
 *
 * <p>Role: {@link ElementRole#LIST}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → Listable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface Listable extends Element, ActionCapabilityProvider {

    /** @return fully-qualified role-suffixed locator key, e.g. {@code PageName.EnumName.CONSTANT.LIST}. */
    default String getListLocator() { return Element.qualifiedLocatorKey(this, ElementRole.LIST); }

    int getIndex();

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : Element.super.getDisplayText();
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

