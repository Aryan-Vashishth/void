package core.resolvers.locator.json;

import domain.automation.web.resolve.json.JsonLocatorReader;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link JsonLocatorReader}.
 * <p>
 * All tests are driver-free: they exercise pure classpath JSON-lookup logic only.
 * </p>
 *
 * <p><b>Test fixture:</b> {@code src/test/resources/locators/json/test-locators.json}</p>
 * <p>{@link JsonLocatorReader} prepends {@code "locators/json/"} ({@code LOCATOR_BASE_PATH})
 * to the fileName, so pass {@code "test-locators.json"} (base name only).</p>
 *
 * <pre>
 * Fixture layout:
 * {
 *   "elements": {
 *     "SEARCH_INPUT":  "//input[@type='search']",
 *     "SEARCH_BUTTON": "css=button.search-btn"
 *   },
 *   "FLAT_KEY": "//span[@id='flat']",
 *   "nested": {
 *     "NESTED_KEY": "xpath=//div[@data-id='nested']"
 *   }
 * }
 * </pre>
 *
 * Coverage:
 * <ul>
 *   <li>{@link JsonLocatorReader#getRaw(String, String)} – dot-path, flat, deep, null/blank
 *       key, null fileName, missing file, missing key</li>
 * </ul>
 */

public class JsonLocatorReaderV1Test {

    // =====================================================================
    // Shared fixture – JsonLocatorReaderV1 prepends "locators/json/" automatically
    // =====================================================================

    private static final String JSON_FILE = "test-locators.json";

    // =====================================================================
    // getRaw() – null / blank guards
    // =====================================================================

    @Test(description = "null fileName (hardcoded safeguard) → key is returned as-is")
    public void getRaw_nullFileName_returnsKeyAsTemplate() {
        String key = "xpath=//button[@id='submit']";
        assertEquals(JsonLocatorReader.getRaw(null, key), key,
                "Null file should return the key itself (hardcoded safeguard)");
    }

    @Test(description = "null key → returns null")
    public void getRaw_nullKey_returnsNull() {
        assertNull(JsonLocatorReader.getRaw(JSON_FILE, null));
    }

    @Test(description = "Blank key (spaces only) → returns null")
    public void getRaw_blankKey_returnsNull() {
        assertNull(JsonLocatorReader.getRaw(JSON_FILE, "   "));
    }

    @Test(description = "Empty string key → returns null")
    public void getRaw_emptyKey_returnsNull() {
        assertNull(JsonLocatorReader.getRaw(JSON_FILE, ""));
    }

    // =====================================================================
    // getRaw() – missing file
    // =====================================================================

    @Test(description = "Non-existent file → returns null (load returns null gracefully)")
    public void getRaw_missingFile_returnsNull() {
        assertNull(JsonLocatorReader.getRaw("no-such-file.json", "FLAT_KEY"));
    }

    // =====================================================================
    // getRaw() – flat key lookup
    // =====================================================================

    @Test(description = "Flat top-level key → correct raw value returned")
    public void getRaw_flatKey_returnsCorrectValue() {
        String raw = JsonLocatorReader.getRaw(JSON_FILE, "FLAT_KEY");
        assertNotNull(raw, "FLAT_KEY should exist in fixture");
        assertEquals(raw.trim(), "//span[@id='flat']");
    }

    // =====================================================================
    // getRaw() – dot-path lookup (nested keys)
    // =====================================================================

    @Test(description = "Dot-path key for SEARCH_INPUT → raw XPath returned")
    public void getRaw_dotPath_searchInput_returnsXpath() {
        String raw = JsonLocatorReader.getRaw(JSON_FILE, "elements.SEARCH_INPUT");
        assertNotNull(raw, "elements.SEARCH_INPUT should exist in fixture");
        assertEquals(raw.trim(), "//input[@type='search']");
    }

    @Test(description = "Dot-path key for SEARCH_BUTTON → raw CSS selector returned")
    public void getRaw_dotPath_searchButton_returnsCss() {
        String raw = JsonLocatorReader.getRaw(JSON_FILE, "elements.SEARCH_BUTTON");
        assertNotNull(raw, "elements.SEARCH_BUTTON should exist in fixture");
        assertEquals(raw.trim(), "css=button.search-btn");
    }

    @Test(description = "Deep dot-path key nested.NESTED_KEY → raw value returned")
    public void getRaw_deepDotPath_nestedKey_returnsValue() {
        String raw = JsonLocatorReader.getRaw(JSON_FILE, "nested.NESTED_KEY");
        assertNotNull(raw, "nested.NESTED_KEY should exist in fixture");
        assertEquals(raw.trim(), "xpath=//div[@data-id='nested']");
    }

    // =====================================================================
    // getRaw() – deep/fallback field search (no dot-path, field found anywhere)
    // =====================================================================

    @Test(description = "Key with no dot (field search fallback) – NESTED_KEY found anywhere in tree")
    public void getRaw_flatKeyFallback_nestedKeyFoundAnywhere() {
        // No dot-path; deepFindField will find NESTED_KEY inside the 'nested' object
        String raw = JsonLocatorReader.getRaw(JSON_FILE, "NESTED_KEY");
        assertNotNull(raw, "NESTED_KEY should be discoverable via deep field search");
        assertTrue(raw.contains("nested"), "Expected a value referencing 'nested'; got: " + raw);
    }

    @Test(description = "SEARCH_INPUT found via deep field search (no dot prefix)")
    public void getRaw_flatKeyFallback_searchInputFoundAnywhere() {
        String raw = JsonLocatorReader.getRaw(JSON_FILE, "SEARCH_INPUT");
        assertNotNull(raw);
        assertTrue(raw.contains("search"), "Expected a search-related locator; got: " + raw);
    }

    // =====================================================================
    // getRaw() – missing key
    // =====================================================================

    @Test(description = "Key not present in the JSON file → null returned")
    public void getRaw_missingKey_returnsNull() {
        assertNull(JsonLocatorReader.getRaw(JSON_FILE, "COMPLETELY_MISSING_KEY_XYZ"));
    }

    @Test(description = "Dot-path with non-existent intermediate node → null returned")
    public void getRaw_dotPath_missingIntermediateNode_returnsNull() {
        assertNull(JsonLocatorReader.getRaw(JSON_FILE, "nonExistent.SOME_KEY"));
    }

    // =====================================================================
    // getRaw() – fileName already contains the base path (joinBase guard)
    // =====================================================================

    @Test(description = "fileName already has locators/json/ prefix → not doubled")
    public void getRaw_fileNameWithFullPrefix_notDoubled() {
        // If the full path is passed, joinBase should not prepend again
        String raw = JsonLocatorReader.getRaw("locators/json/test-locators.json", "FLAT_KEY");
        assertNotNull(raw, "FLAT_KEY should still be found when full prefix is included");
        assertEquals(raw.trim(), "//span[@id='flat']");
    }

    // =====================================================================
    // DataProvider – all fixture keys accessible via dot-path or flat lookup
    // =====================================================================

    @DataProvider(name = "fixtureKeys")
    public Object[][] fixtureKeyData() {
        return new Object[][]{
            {"FLAT_KEY",              "//span[@id='flat']"},
            {"elements.SEARCH_INPUT", "//input[@type='search']"},
            {"elements.SEARCH_BUTTON","css=button.search-btn"},
            {"nested.NESTED_KEY",     "xpath=//div[@data-id='nested']"},
        };
    }

    @Test(
            dataProvider  = "fixtureKeys",
            description   = "All fixture keys return the expected raw locator string"
    )
    public void getRaw_allFixtureKeys_returnExpectedValues(String key, String expectedValue) {
        String raw = JsonLocatorReader.getRaw(JSON_FILE, key);
        assertNotNull(raw, "Expected non-null value for key: " + key);
        assertEquals(raw.trim(), expectedValue, "Key: '" + key + "'");
    }
}

