package elements.api.capability;

import core.actions.ActionCapability;
import core.actions.ActionCapabilityProvider;
import core.actions.UploadAction;
import elements.api.Element;
import elements.meta.ElementRole;

/**
 * Capability interface for file upload fields (e.g., &lt;input type="file"/&gt;).
 *
 * <p>Role: {@link ElementRole#INPUT}</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → Uploadable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 */
public interface Uploadable extends Element, ActionCapabilityProvider {

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

