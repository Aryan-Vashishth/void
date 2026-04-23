package core.resolvers.locator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ordered registry of {@link LocatorSource}s implementing the
 * <em>Chain of Responsibility</em> pattern. {@link #select(String)} returns the
 * first registered source that {@linkplain LocatorSource#supports(String) supports}
 * the given file name.
 *
 * <p>Replaces the closed {@code pickReader(...)} switch — adding a new format
 * is now done by constructing a registry with an extra source, with no edits
 * to the resolver itself.</p>
 *
 * <p>The {@link #DEFAULT} registry contains, in order:
 * {@link HardcodedLocatorSource}, {@link PropertiesLocatorSource},
 * {@link JsonLocatorSource}.</p>
 *
 * <p>This class is immutable and thread-safe.</p>
 */
public final class LocatorSourceRegistry {

    /** Default registry: hardcoded → properties → json. */
    public static final LocatorSourceRegistry DEFAULT = new LocatorSourceRegistry(List.of(
            HardcodedLocatorSource.INSTANCE,
            PropertiesLocatorSource.INSTANCE,
            JsonLocatorSource.INSTANCE
    ));

    private final List<LocatorSource> sources;

    public LocatorSourceRegistry(List<LocatorSource> sources) {
        this.sources = List.copyOf(sources);
    }

    /** Convenience builder: {@code DEFAULT.with(yamlSource)}. */
    public LocatorSourceRegistry with(LocatorSource extra) {
        List<LocatorSource> combined = new ArrayList<>(this.sources.size() + 1);
        combined.addAll(this.sources);
        combined.add(extra);
        return new LocatorSourceRegistry(combined);
    }

    /** Immutable view of the registered sources, in resolution order. */
    public List<LocatorSource> sources() {
        return Collections.unmodifiableList(sources);
    }

    /**
     * Return the first source that supports {@code fileName}.
     *
     * @throws IllegalArgumentException if no registered source supports the file name
     */
    public LocatorSource select(String fileName) {
        for (LocatorSource s : sources) {
            if (s.supports(fileName)) return s;
        }
        throw new IllegalArgumentException(
                "Unsupported locator file: " + fileName +
                " (no registered LocatorSource supports it; expected null/.properties/.json)");
    }
}

