package core.resolvers.locator.api;

import elements.api.UIElement;

import javax.annotation.Nullable;

/**
 * Abstraction for resolving which repository file name an element should use.
 *
 * <p>Decouples {@link LocatorResolver} from the specific convention or override
 * mechanism used to locate a page's repository. The resolver delegates all
 * "where is the file?" decisions to this interface rather than calling
 * {@link UIElement#getExternalFileName()} directly.</p>
 *
 * <h3>Resolution contract</h3>
 * <p>Returns the classpath-relative file name compatible with
 * {@link LocatorRequest#fileName()} — i.e. the value passed to
 * {@link core.resolvers.locator.source.LocatorSourceRegistry#select(String)}.
 * {@code null} signals a hardcoded template (no external file).</p>
 *
 * <h3>Phase 14 — caching</h3>
 * <p>Phase 14 wraps this interface in a caching decorator. Phase 13 introduces
 * only the abstraction and the default implementation; no caching is applied yet.</p>
 *
 * @see DefaultLocatorContext
 */
public interface LocatorContext {

    /**
     * Returns the classpath-relative file name for the given element's repository,
     * or {@code null} if the element's locator is hardcoded (no external file).
     */
    @Nullable
    String resolveFileName(UIElement element);
}
