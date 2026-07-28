package elements.api.capability;

import core.actions.ActionCapability;

import elements.api.actions.UploadAction;
import elements.api.UIElement;
import elements.meta.ElementRole;

/**
 * Capability interface for file upload fields (e.g., &lt;input type="file"/&gt;).
 *
 * <p>Role: {@link ElementRole#INPUT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   UIElement → Uploadable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 *
 * <p><b>Domain ownership:</b> Web ({@code elements.api.capability}, ADR-021, I3.3).
 * Not a kernel type. The kernel references capabilities solely through
 * {@link core.actions.ActionCapability}.</p>
 */
public interface Uploadable extends UIElement {

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
        String key = getInputLocator();
        if (key != null && !key.isBlank()) roles.put(ElementRole.INPUT, key);
        return roles;
    }

    @Override
    default ActionCapability capability() { return ActionCapability.UPLOADABLE; }

    // ── Action emission ─────────────────────────────────────────────────

    /** Emits an {@link UploadAction} for the given file path. */
    default UploadAction upload(String filePath) {
        return new UploadAction(this, filePath);
    }
}

