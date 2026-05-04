package elements.api;

import core.actions.Action;
import core.resolvers.locator.api.LocatorResolvers;
import elements.meta.ElementRole;

/**
 * Action-producing extension of {@link FileInputTarget}.
 *
 * <p>Provides an {@link #upload(String)} method that returns a deferred {@link Action}.
 * Locator resolution happens inside the Action — NOT eagerly.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → FileInputTarget → FileInputAction
 * </pre>
 *
 * <p>Usage:</p>
 * <pre>
 *   Flow.of(DetailPanel.FILE_UPLOAD.upload("/path/to/document.pdf"));
 * </pre>
 */
public interface FileInputAction extends FileInputTarget {

    /**
     * Produces an Action that uploads a file via this input.
     * Resolution is deferred — descriptor is resolved at execution time.
     *
     * @param filePath absolute path to the file to upload
     * @return upload Action
     */
    default Action upload(String filePath) {
        return (engine) -> {
            var descriptor = LocatorResolvers.strict().resolveDescriptor(this, ElementRole.INPUT);
            engine.uploadFile(descriptor, filePath);
        };
    }
}

