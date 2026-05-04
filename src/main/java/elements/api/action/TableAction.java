package elements.api.action;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.target.TableTarget;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link TableTarget}.
 *
 * <p>Provides {@link #scrollToTable()} — deferred resolution.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → TableTarget → TableAction
 * </pre>
 */
public interface TableAction extends TableTarget {

    default Action scrollToTable() {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.TABLE);
            engine.scrollTo(descriptor);
        };
    }
}

