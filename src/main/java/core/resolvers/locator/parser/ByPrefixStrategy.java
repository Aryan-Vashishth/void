package core.resolvers.locator.parser;

import org.openqa.selenium.By;

import java.util.function.Function;

/**
 * One entry in the prefix → {@link By} factory table used by {@link ByParser}.
 *
 * @deprecated Part of the deprecated {@link ByParser} By-returning pipeline. Deletes in I9.3.
 */
@Deprecated(forRemoval = true)
public record ByPrefixStrategy(String prefix, String label, Function<String, By> factory) {

    /** {@code true} if {@code lowerTrimmed} starts with this strategy's prefix. */
    public boolean matches(String lowerTrimmed) {
        return lowerTrimmed.startsWith(prefix);
    }

    /** Apply the factory to the substring of {@code trimmed} after the prefix, validating non-empty. */
    public By apply(String trimmed) {
        String value = trimmed.substring(prefix.length()).trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Empty value after '" + label + "=' in locator: " + trimmed);
        }
        return factory.apply(value);
    }
}

