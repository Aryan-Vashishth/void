package core.resolvers.locator.api;

import elements.api.Element;

/**
 * Default {@link LocatorContext} implementation that delegates to
 * {@link Element#getExternalFileName()}.
 *
 * <p>The convention probing logic (Phase 5 conventional path → Phase 8 simple-name fallback
 * → preferred conventional target) lives in {@code Element.getExternalFileName()}'s default
 * implementation. This class is the resolver-side seam that makes it replaceable and
 * cacheable (Phase 14) without further changes to {@link LocatorResolver}.</p>
 *
 * <p>Stateless singleton — thread-safe.</p>
 */
public final class DefaultLocatorContext implements LocatorContext {

    /** Shared singleton — stateless, thread-safe. */
    public static final DefaultLocatorContext INSTANCE = new DefaultLocatorContext();

    private DefaultLocatorContext() {}

    /**
     * Delegates to {@link Element#getExternalFileName()}, which probes the classpath
     * for the conventional path and falls back as described in {@link Element}.
     */
    @Override
    public String resolveFileName(Element element) {
        return element.getExternalFileName();
    }
}
