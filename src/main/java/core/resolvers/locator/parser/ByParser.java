package core.resolvers.locator.parser;

import org.openqa.selenium.By;

import java.util.List;
import java.util.Locale;

/**
 * Single canonical implementation of "raw locator string &rarr; Selenium {@link By}" parsing.
 *
 * <p>Supports a registered prefix table (see {@link ByPrefixStrategy}) plus a heuristic fallback:
 * strings starting with {@code "/"}, {@code "("} or {@code ".//"} are treated as XPath, otherwise
 * CSS. This consolidates the two slightly-divergent {@code toBy} implementations that previously
 * lived in {@code ElementLocatorResolverV1} and {@code PropertiesFileLocatorReaderV1}.</p>
 *
 * @deprecated The By-returning resolve pipeline is deprecated as of I7.3.
 *     {@link core.resolvers.locator.api.LocatorResolver#resolveDescriptor} is the replacement.
 *     Retained only by {@code PropertiesFileLocatorReader} legacy bridge; deletes in I9.3.
 */
@Deprecated(forRemoval = true)
public final class ByParser {

    /** Default prefix table — case-insensitive matching. */
    public static final List<ByPrefixStrategy> DEFAULT_STRATEGIES = List.of(
            new ByPrefixStrategy("id=",              "id",              By::id),
            new ByPrefixStrategy("name=",            "name",            By::name),
            new ByPrefixStrategy("class=",           "class",           By::className),
            new ByPrefixStrategy("tag=",             "tag",             By::tagName),
            new ByPrefixStrategy("linktext=",        "linkText",        By::linkText),
            new ByPrefixStrategy("partiallinktext=", "partialLinkText", By::partialLinkText),
            new ByPrefixStrategy("css=",             "css",             By::cssSelector),
            new ByPrefixStrategy("xpath=",           "xpath",           By::xpath)
    );

    /** Shared, immutable, default-configured parser. */
    public static final ByParser DEFAULT = new ByParser(DEFAULT_STRATEGIES);

    private final List<ByPrefixStrategy> strategies;

    public ByParser(List<ByPrefixStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    /**
     * Parse a raw locator string into a {@link By}.
     *
     * @throws IllegalArgumentException if {@code raw} is {@code null}
     * @throws IllegalStateException    if {@code raw} is blank or has an empty value after a prefix
     */
    public By parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Locator is null.");
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException("Locator string is null or blank.");
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);

        for (ByPrefixStrategy s : strategies) {
            if (s.matches(lower)) return s.apply(trimmed);
        }
        return heuristic(trimmed);
    }

    /** Heuristic fallback when no prefix matched: XPath-looking strings → XPath, else CSS. */
    private static By heuristic(String trimmed) {
        if (trimmed.startsWith("/") || trimmed.startsWith("(") || trimmed.startsWith(".//")) {
            return By.xpath(trimmed);
        }
        return By.cssSelector(trimmed);
    }
}

