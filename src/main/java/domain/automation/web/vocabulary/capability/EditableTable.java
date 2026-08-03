package domain.automation.web.vocabulary.capability;

import core.actions.Action;
import core.actions.ActionCapability;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.vocabulary.role.ElementRole;

/**
 * Capability interface for writable tables supporting row insertion/removal.
 *
 * <p>Additional roles: ADD_ROW_BUTTON, REMOVE_ROW_BUTTON, FOOTER_INPUT_ROW</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   UIElement → Table → EditableTable
 * </pre>
 *
 * <h3>Action emission</h3>
 * <p>Contains NO execution logic. Emits Action (intent) only.</p>
 *
 * <p><b>Domain ownership:</b> Web ({@code elements.api.capability}, ADR-021, I3.3).
 * Not a kernel type. The kernel references capabilities solely through
 * {@link core.actions.ActionCapability}.</p>
 */
public interface EditableTable extends Table {

    default String getAddRowButtonLocator() { return null; }

    default String getRemoveRowButtonLocator() { return null; }

    default String getFooterInputRowLocator() { return null; }

    @Override
    default java.util.Map<ElementRole, String> getAllLocatorRoles() {
        java.util.Map<ElementRole, String> roles = new java.util.LinkedHashMap<>(Table.super.getAllLocatorRoles());
        String add = getAddRowButtonLocator();
        if (add != null && !add.isBlank() && !roles.containsValue(add)) roles.put(ElementRole.ADD_ROW_BUTTON, add);
        String remove = getRemoveRowButtonLocator();
        if (remove != null && !remove.isBlank() && !roles.containsValue(remove)) roles.put(ElementRole.REMOVE_ROW_BUTTON, remove);
        String footer = getFooterInputRowLocator();
        if (footer != null && !footer.isBlank() && !roles.containsValue(footer)) roles.put(ElementRole.FOOTER_INPUT_ROW, footer);
        return roles;
    }

    @Override
    default ActionCapability capability() { return ActionCapability.EDITABLE_TABLE; }

    // ── Action emission ─────────────────────────────────────────────────

    default Action clickAddRow() {
        return executor -> {
            UIEngine engine = (UIEngine) executor;
            var d = engine.resolve(this, ElementRole.ADD_ROW_BUTTON);
            engine.click(d);
        };
    }

    default Action clickRemoveRow() {
        return executor -> {
            UIEngine engine = (UIEngine) executor;
            var d = engine.resolve(this, ElementRole.REMOVE_ROW_BUTTON);
            engine.click(d);
        };
    }
}

