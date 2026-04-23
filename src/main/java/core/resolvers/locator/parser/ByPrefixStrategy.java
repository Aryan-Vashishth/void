package core.resolvers.locator.parser;

import org.openqa.selenium.By;

import java.util.function.Function;

/**
 * One entry in the prefix → {@link By} factory table used by {@link ByParser}.
 *
 * <p>Replaces the long {@code if/else} chain that used to appear in both
 * {@code ElementLocatorResolverV1.toBy} and
 * {@code PropertiesFileLocatorReaderV1.toBy}.</p>
 *
 * @param prefix  the lowercase prefix including the trailing {@code "="}
 *                (e.g. {@code "id="}, {@code "partiallinktext="})
 * @param label   human-readable label used in error messages (e.g. {@code "id"}, {@code "partialLinkText"})
 * @param factory function turning the value-after-prefix into a {@link By}
 */
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

