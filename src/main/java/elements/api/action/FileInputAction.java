package elements.api.action;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.api.target.FileInputTarget;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link FileInputTarget}.
 *
 * <p>Provides {@link #upload(String)} — deferred resolution.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → FileInputTarget → FileInputAction
 * </pre>
 */
public interface FileInputAction extends FileInputTarget {

    default Action upload(String filePath) {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.INPUT);
            engine.uploadFile(descriptor, filePath);
        };
    }
}

