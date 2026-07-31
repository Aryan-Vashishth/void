package domain.automation.web.resolve.api;

import domain.automation.web.vocabulary.element.UIElement;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link LocatorContext} implementation that delegates to
 * {@link UIElement#getExternalFileName()}.
 *
 * <p>The convention probing logic (Phase 5 conventional path → Phase 8 simple-name fallback
 * → preferred conventional target) lives in {@code UIElement.getExternalFileName()}'s default
 * implementation. This class is the resolver-side seam that makes it replaceable and
 * cacheable without further changes to {@link LocatorResolver}.</p>
 *
 * <p>Phase 14: the resolved file name is cached keyed by {@code element.getClass()} so that
 * the classpath probes in {@code getExternalFileName()} run at most once per element class
 * per session. Enum constants without a body share their enum class as the key; constants
 * that override {@code getExternalFileName()} per-body each get their own entry.</p>
 *
 * <p>Stateless singleton — thread-safe.</p>
 */
public final class DefaultLocatorContext implements LocatorContext {

    /** Shared singleton — stateless, thread-safe. */
    public static final DefaultLocatorContext INSTANCE = new DefaultLocatorContext();

    private static final ConcurrentHashMap<Class<?>, Optional<String>> FILE_NAME_CACHE =
            new ConcurrentHashMap<>();

    private DefaultLocatorContext() {}

    /**
     * Returns the locator file name for {@code element}, caching the result per element class.
     *
     * <p>Using {@code element.getClass()} as the key means that enum constants without a
     * body all share one cache entry (their common enum class), while constants that override
     * {@code getExternalFileName()} per-body each get their own entry. This avoids
     * cross-contamination between enum classes in the same page that use different strategies
     * (e.g., some hardcoded, some file-backed).</p>
     */
    @Override
    public String resolveFileName(UIElement element) {
        return FILE_NAME_CACHE
                .computeIfAbsent(element.getClass(), k -> Optional.ofNullable(element.getExternalFileName()))
                .orElse(null);
    }
}
