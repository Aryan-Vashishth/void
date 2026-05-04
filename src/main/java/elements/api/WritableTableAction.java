package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link WritableTableTarget}.
 *
 * <p>Provides {@link #clickAddRow()} and {@link #clickRemoveRow()} methods that return
 * deferred {@link Action}s. Locator resolution happens inside the Action — NOT eagerly.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → TableTarget → WritableTableTarget → WritableTableAction
 * </pre>
 */
public interface WritableTableAction extends WritableTableTarget {

    /**
     * Produces an Action that clicks the "Add Row" button.
     * Resolution is deferred against ADD_ROW_BUTTON role.
     *
     * @return addRow Action
     */
    default Action clickAddRow() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.ADD_ROW_BUTTON);
            engine.click(descriptor);
        };
    }

    /**
     * Produces an Action that clicks the "Remove Row" button.
     * Resolution is deferred against REMOVE_ROW_BUTTON role.
     *
     * @return removeRow Action
     */
    default Action clickRemoveRow() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.REMOVE_ROW_BUTTON);
            engine.click(descriptor);
        };
    }
}

