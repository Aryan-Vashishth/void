package core.engine;

/**
 * Supported locator strategies for engine-agnostic element resolution.
 *
 * <p>Each {@link UIEngine} implementation translates a {@link LocatorDescriptor}
 * (which includes a strategy) into its native locator representation:</p>
 * <ul>
 *   <li>Selenium: {@code By.xpath(...)}, {@code By.cssSelector(...)}, etc.</li>
 *   <li>Playwright: {@code page.locator("xpath=...")}, {@code page.locator("#id")}, etc.</li>
 * </ul>
 */
public enum LocatorStrategy {

    /** XPath expression (e.g., {@code //button[@id='apply']}). */
    XPATH,

    /** CSS selector (e.g., {@code div.container > button.primary}). */
    CSS,

    /** Element ID (e.g., {@code username}). */
    ID,

    /** Element name attribute (e.g., {@code email}). */
    NAME;

    /**
     * Infers the strategy from a raw locator string using common heuristics.
     * <ul>
     *   <li>Starts with {@code //} or {@code (//} → XPATH</li>
     *   <li>Starts with {@code #} → ID (CSS shorthand)</li>
     *   <li>Otherwise → CSS (safest default)</li>
     * </ul>
     *
     * @param locatorValue raw locator string
     * @return inferred strategy
     */
    public static LocatorStrategy infer(String locatorValue) {
        if (locatorValue == null || locatorValue.isBlank()) return CSS;
        String trimmed = locatorValue.trim();
        if (trimmed.startsWith("//") || trimmed.startsWith("(//") || trimmed.startsWith("(./")) {
            return XPATH;
        }
        return CSS;
    }
}

