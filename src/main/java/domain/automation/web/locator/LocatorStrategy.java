package domain.automation.web.locator;

/**
 * Locator strategy token identifying how an element is addressed.
 *
 * <p>The set is intentionally open: any domain or engine can define new strategies
 * via {@link #of(String)} without editing framework-owned files.</p>
 *
 * <p>Each {@link core.engine.UIEngine} implementation translates a
 * {@link LocatorDescriptor} (which includes a strategy) into its native locator
 * representation:</p>
 * <ul>
 *   <li>Selenium: {@code By.xpath(...)}, {@code By.cssSelector(...)}, etc.</li>
 *   <li>Playwright: {@code page.locator("xpath=...")}, {@code page.locator("#id")}, etc.</li>
 * </ul>
 *
 * <p>Engines that do not support a given strategy must throw
 * {@link IllegalStateException} with a message naming the unsupported strategy,
 * rather than silently falling back to a different strategy.</p>
 */
public interface LocatorStrategy {

    /** Returns the canonical name of this strategy, used in logging and diagnostics. */
    String name();

    /**
     * Creates a strategy with the given name.
     *
     * <p>Equality is name-based: two strategies with the same name are equal.
     * Prefer static constants over repeated {@code of()} calls to guarantee
     * consistent identity and avoid allocation.</p>
     *
     * @param name non-blank canonical name (conventionally upper-case, e.g. {@code "XPATH"})
     * @return a LocatorStrategy for that name
     * @throws IllegalArgumentException if name is null or blank
     */
    static LocatorStrategy of(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("LocatorStrategy name must not be blank");
        return new NamedStrategy(name);
    }

    // ── Built-in web-domain strategies ──────────────────────────────────────

    /** XPath expression (e.g., {@code //button[@id='apply']}). */
    LocatorStrategy XPATH = of("XPATH");

    /** CSS selector (e.g., {@code div.container > button.primary}). */
    LocatorStrategy CSS   = of("CSS");

    /** Element ID attribute (e.g., {@code username}). */
    LocatorStrategy ID    = of("ID");

    /** Element name attribute (e.g., {@code email}). */
    LocatorStrategy NAME  = of("NAME");

    // ── Inference ───────────────────────────────────────────────────────────

    /**
     * Infers the strategy from a raw locator string using common heuristics.
     * <ul>
     *   <li>Starts with {@code //}, {@code (//}, or {@code (./} &rarr; XPATH</li>
     *   <li>Otherwise &rarr; CSS (safest default)</li>
     * </ul>
     *
     * <p>Explicit prefix strings (e.g., {@code xpath=...}, {@code id=...}) are
     * handled by {@link core.resolvers.locator.api.LocatorResolver} before this
     * method is called; this method operates on already-stripped values.</p>
     *
     * @param locatorValue raw locator string
     * @return inferred strategy
     */
    static LocatorStrategy infer(String locatorValue) {
        if (locatorValue == null || locatorValue.isBlank()) return CSS;
        String trimmed = locatorValue.trim();
        if (trimmed.startsWith("//") || trimmed.startsWith("(//") || trimmed.startsWith("(./")) {
            return XPATH;
        }
        return CSS;
    }
}
