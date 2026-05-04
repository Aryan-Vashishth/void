package elements.api.action;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.target.WritableTableTarget;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link WritableTableTarget}.
 *
 * <p>Provides {@link #clickAddRow()} and {@link #clickRemoveRow()} — deferred resolution.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → TableTarget → WritableTableTarget → WritableTableAction
 * </pre>
 */
public interface WritableTableAction extends WritableTableTarget {

    default Action clickAddRow() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.ADD_ROW_BUTTON);
            engine.click(descriptor);
        };
    }

    default Action clickRemoveRow() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.REMOVE_ROW_BUTTON);
            engine.click(descriptor);
        };
    }
}

