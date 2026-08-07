package core.resolvers.locator.properties;

import domain.automation.web.resolve.properties.PropertiesFileLocatorReader;
import org.openqa.selenium.By;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link PropertiesFileLocatorReader}.
 * <p>
 * Covers:
 * <ul>
 *   <li>{@link PropertiesFileLocatorReader#toBy(String)} – all explicit prefixes, heuristic fallback,
 *       case-insensitivity, whitespace trimming, and error cases</li>
 *   <li>{@link PropertiesFileLocatorReader#getRaw(String, String)} – classpath property file lookup</li>
 *   <li>{@link PropertiesFileLocatorReader#getLocatorValueSafely(String, String)} – end-to-end convenience method</li>
 * </ul>
 * </p>
 *
 * <b>Test fixture:</b> {@code src/test/resources/locators/test-locators.properties}
 */

@Test(groups = {"integration"})
public class PropertiesFileLocatorReaderV1Test {

    // =====================================================================
    // Constants – shared test fixture path
    // =====================================================================

    private static final String FIXTURE_FILE = "test-locators.properties";  // base prepended by reader

    // =====================================================================
    // toBy() – Explicit Prefixes (happy path)
    // =====================================================================

    @Test(description = "id= prefix → By.id")
    public void toBy_idPrefix_returnsById() {
        By result = PropertiesFileLocatorReader.toBy("id=loginBtn");
        assertEquals(result.toString(), By.id("loginBtn").toString(),
                "Expected By.id but got: " + result);
    }

    @Test(description = "name= prefix → By.name")
    public void toBy_namePrefix_returnsByName() {
        By result = PropertiesFileLocatorReader.toBy("name=email");
        assertEquals(result.toString(), By.name("email").toString());
    }

    @Test(description = "class= prefix → By.className")
    public void toBy_classPrefix_returnsByClassName() {
        By result = PropertiesFileLocatorReader.toBy("class=btn-primary");
        assertEquals(result.toString(), By.className("btn-primary").toString());
    }

    @Test(description = "tag= prefix → By.tagName")
    public void toBy_tagPrefix_returnsByTagName() {
        By result = PropertiesFileLocatorReader.toBy("tag=input");
        assertEquals(result.toString(), By.tagName("input").toString());
    }

    @Test(description = "linkText= prefix → By.linkText")
    public void toBy_linkTextPrefix_returnsByLinkText() {
        By result = PropertiesFileLocatorReader.toBy("linkText=Click here");
        assertEquals(result.toString(), By.linkText("Click here").toString());
    }

    @Test(description = "partialLinkText= prefix → By.partialLinkText")
    public void toBy_partialLinkTextPrefix_returnsByPartialLinkText() {
        By result = PropertiesFileLocatorReader.toBy("partialLinkText=Read more");
        assertEquals(result.toString(), By.partialLinkText("Read more").toString());
    }

    @Test(description = "css= prefix → By.cssSelector")
    public void toBy_cssPrefix_returnsByCssSelector() {
        By result = PropertiesFileLocatorReader.toBy("css=div.container > span");
        assertEquals(result.toString(), By.cssSelector("div.container > span").toString());
    }

    @Test(description = "xpath= prefix → By.xpath")
    public void toBy_xpathPrefix_returnsByXpath() {
        By result = PropertiesFileLocatorReader.toBy("xpath=//div[@id='main']");
        assertEquals(result.toString(), By.xpath("//div[@id='main']").toString());
    }

    // =====================================================================
    // toBy() – Case Insensitivity
    // =====================================================================

    @Test(description = "ID= (uppercase) prefix is recognised case-insensitively")
    public void toBy_idPrefix_caseInsensitive_upper() {
        By result = PropertiesFileLocatorReader.toBy("ID=myId");
        assertEquals(result.toString(), By.id("myId").toString());
    }

    @Test(description = "XPATH= (uppercase) prefix is recognised case-insensitively")
    public void toBy_xpathPrefix_caseInsensitive_upper() {
        By result = PropertiesFileLocatorReader.toBy("XPATH=//span");
        assertEquals(result.toString(), By.xpath("//span").toString());
    }

    @Test(description = "CSS= (uppercase) prefix is recognised case-insensitively")
    public void toBy_cssPrefix_caseInsensitive_upper() {
        By result = PropertiesFileLocatorReader.toBy("CSS=.active");
        assertEquals(result.toString(), By.cssSelector(".active").toString());
    }

    @Test(description = "Name= (mixed case) prefix is recognised case-insensitively")
    public void toBy_namePrefix_caseInsensitive_mixed() {
        By result = PropertiesFileLocatorReader.toBy("Name=q");
        assertEquals(result.toString(), By.name("q").toString());
    }

    @Test(description = "LINKTEXT= (uppercase) prefix is recognised case-insensitively")
    public void toBy_linkTextPrefix_caseInsensitive_upper() {
        By result = PropertiesFileLocatorReader.toBy("LINKTEXT=Home");
        assertEquals(result.toString(), By.linkText("Home").toString());
    }

    @Test(description = "PARTIALLINKTEXT= (uppercase) prefix is recognised case-insensitively")
    public void toBy_partialLinkTextPrefix_caseInsensitive_upper() {
        By result = PropertiesFileLocatorReader.toBy("PARTIALLINKTEXT=Home");
        assertEquals(result.toString(), By.partialLinkText("Home").toString());
    }

    // =====================================================================
    // toBy() – Heuristic Fallback (no prefix)
    // =====================================================================

    @Test(description = "String starting with // → heuristic XPath")
    public void toBy_doubleSlash_heuristicXpath() {
        By result = PropertiesFileLocatorReader.toBy("//div[@class='row']");
        assertEquals(result.toString(), By.xpath("//div[@class='row']").toString());
    }

    @Test(description = "String starting with / → heuristic XPath")
    public void toBy_singleSlash_heuristicXpath() {
        By result = PropertiesFileLocatorReader.toBy("/html/body/div");
        assertEquals(result.toString(), By.xpath("/html/body/div").toString());
    }

    @Test(description = "String starting with ( → heuristic XPath (indexed)")
    public void toBy_openParen_heuristicXpath() {
        By result = PropertiesFileLocatorReader.toBy("(//table//tr)[1]");
        assertEquals(result.toString(), By.xpath("(//table//tr)[1]").toString());
    }

    @Test(description = "String starting with .// → heuristic XPath (relative)")
    public void toBy_dotDoubleSlash_heuristicXpath() {
        By result = PropertiesFileLocatorReader.toBy(".//td[2]");
        assertEquals(result.toString(), By.xpath(".//td[2]").toString());
    }

    @Test(description = "Plain CSS selector string → heuristic cssSelector")
    public void toBy_plainString_heuristicCss() {
        By result = PropertiesFileLocatorReader.toBy("div.my-class");
        assertEquals(result.toString(), By.cssSelector("div.my-class").toString());
    }

    @Test(description = "CSS attribute selector string → heuristic cssSelector")
    public void toBy_cssAttributeSelector_heuristicCss() {
        By result = PropertiesFileLocatorReader.toBy("input[type='submit']");
        assertEquals(result.toString(), By.cssSelector("input[type='submit']").toString());
    }

    // =====================================================================
    // toBy() – Whitespace Handling
    // =====================================================================

    @Test(description = "Leading and trailing whitespace is trimmed before parsing")
    public void toBy_withSurroundingWhitespace_trims() {
        By result = PropertiesFileLocatorReader.toBy("  id=trimmed  ");
        assertEquals(result.toString(), By.id("trimmed").toString());
    }

    @Test(description = "Whitespace between prefix and value is trimmed")
    public void toBy_whitespaceAfterPrefix_trimmedFromValue() {
        By result = PropertiesFileLocatorReader.toBy("name=  fieldName  ");
        // valueAfter trims the substring, so value should be "fieldName"
        assertEquals(result.toString(), By.name("fieldName").toString());
    }

    // =====================================================================
    // toBy() – Error Cases
    // =====================================================================

    @Test(
        description   = "null locator string → IllegalStateException",
        expectedExceptions = IllegalStateException.class
    )
    public void toBy_null_throwsIllegalState() {
        PropertiesFileLocatorReader.toBy(null);
    }

    @Test(
        description   = "Blank locator string → IllegalStateException",
        expectedExceptions = IllegalStateException.class
    )
    public void toBy_blank_throwsIllegalState() {
        PropertiesFileLocatorReader.toBy("   ");
    }

    @Test(
        description   = "Empty string → IllegalStateException",
        expectedExceptions = IllegalStateException.class
    )
    public void toBy_emptyString_throwsIllegalState() {
        PropertiesFileLocatorReader.toBy("");
    }

    @Test(
        description   = "id= with no value → IllegalStateException (empty after prefix)",
        expectedExceptions = IllegalStateException.class
    )
    public void toBy_idPrefixEmptyValue_throwsIllegalState() {
        PropertiesFileLocatorReader.toBy("id=");
    }

    @Test(
        description   = "xpath= with no value → IllegalStateException",
        expectedExceptions = IllegalStateException.class
    )
    public void toBy_xpathPrefixEmptyValue_throwsIllegalState() {
        PropertiesFileLocatorReader.toBy("xpath=");
    }

    @Test(
        description   = "css= with only whitespace value → IllegalStateException",
        expectedExceptions = IllegalStateException.class
    )
    public void toBy_cssPrefixWhitespaceValue_throwsIllegalState() {
        PropertiesFileLocatorReader.toBy("css=   ");
    }

    // =====================================================================
    // DataProvider-driven prefix tests
    // =====================================================================

    @DataProvider(name = "explicitPrefixes")
    public Object[][] explicitPrefixData() {
        return new Object[][]{
            {"id=myId",               By.id("myId").toString()},
            {"name=myName",           By.name("myName").toString()},
            {"class=myClass",         By.className("myClass").toString()},
            {"tag=div",               By.tagName("div").toString()},
            {"css=#submitBtn",        By.cssSelector("#submitBtn").toString()},
            {"xpath=//h1",            By.xpath("//h1").toString()},
            {"linkText=Sign In",      By.linkText("Sign In").toString()},
            {"partialLinkText=Sign",  By.partialLinkText("Sign").toString()},
        };
    }

    @Test(
        dataProvider  = "explicitPrefixes",
        description   = "All explicit prefixes map to the correct By strategy"
    )
    public void toBy_explicitPrefixes_parametrized(String locatorString, String expectedToString) {
        By result = PropertiesFileLocatorReader.toBy(locatorString);
        assertEquals(result.toString(), expectedToString,
                "Locator: '" + locatorString + "'");
    }

    // =====================================================================
    // getRaw() – Classpath .properties lookup
    // =====================================================================

    @Test(description = "getRaw returns the raw value for an existing key")
    public void getRaw_existingKey_returnsRawValue() {
        String raw = PropertiesFileLocatorReader.getRaw(FIXTURE_FILE, "BUTTON_TRIGGER");
        assertNotNull(raw, "Expected a non-null value for BUTTON_TRIGGER");
        assertTrue(raw.contains("submit"), "Expected value to reference 'submit'; got: " + raw);
    }

    @Test(description = "getRaw returns null for a non-existent key")
    public void getRaw_missingKey_returnsNull() {
        String raw = PropertiesFileLocatorReader.getRaw(FIXTURE_FILE, "NON_EXISTENT_KEY_XYZ");
        assertNull(raw, "Expected null for missing key");
    }

    @Test(description = "getRaw returns values for every entry in the fixture file")
    public void getRaw_allFixtureKeys_notNull() {
        String[] keys = {
            "BUTTON_TRIGGER", "LOGIN_INPUT", "SUBMIT_BUTTON", "NAME_ELEMENT",
            "CLASS_ELEMENT", "TAG_ELEMENT", "LINK_TEXT", "PARTIAL_LINK",
            "XPATH_ELEMENT", "TEMPLATE_WITH_ARG", "TEMPLATE_TWO_ARGS"
        };
        for (String key : keys) {
            String raw = PropertiesFileLocatorReader.getRaw(FIXTURE_FILE, key);
            assertNotNull(raw, "Expected non-null value for key: " + key);
            assertFalse(raw.isBlank(), "Expected non-blank value for key: " + key);
        }
    }

    // =====================================================================
    // getLocatorValueSafely() – end-to-end convenience
    // =====================================================================

    @Test(description = "getLocatorValueSafely converts raw XPath value to By.xpath")
    public void getLocatorValueSafely_rawXpath_returnsByXpath() {
        By result = PropertiesFileLocatorReader.getLocatorValueSafely(FIXTURE_FILE, "BUTTON_TRIGGER");
        assertNotNull(result);
        assertEquals(result.toString(), By.xpath("//button[@id='submit']").toString());
    }

    @Test(description = "getLocatorValueSafely converts css= prefixed value to By.cssSelector")
    public void getLocatorValueSafely_cssPrefix_returnsByCssSelector() {
        By result = PropertiesFileLocatorReader.getLocatorValueSafely(FIXTURE_FILE, "LOGIN_INPUT");
        assertNotNull(result);
        assertEquals(result.toString(), By.cssSelector("input#username").toString());
    }

    @Test(description = "getLocatorValueSafely converts id= prefixed value to By.id")
    public void getLocatorValueSafely_idPrefix_returnsById() {
        By result = PropertiesFileLocatorReader.getLocatorValueSafely(FIXTURE_FILE, "SUBMIT_BUTTON");
        assertNotNull(result);
        assertEquals(result.toString(), By.id("submitBtn").toString());
    }

    @Test(description = "getLocatorValueSafely converts name= prefixed value to By.name")
    public void getLocatorValueSafely_namePrefix_returnsByName() {
        By result = PropertiesFileLocatorReader.getLocatorValueSafely(FIXTURE_FILE, "NAME_ELEMENT");
        assertNotNull(result);
        assertEquals(result.toString(), By.name("searchField").toString());
    }

    @Test(description = "getLocatorValueSafely converts xpath= prefixed value to By.xpath")
    public void getLocatorValueSafely_xpathPrefix_returnsByXpath() {
        By result = PropertiesFileLocatorReader.getLocatorValueSafely(FIXTURE_FILE, "XPATH_ELEMENT");
        assertNotNull(result);
        assertEquals(result.toString(), By.xpath("//div[@class='container']").toString());
    }
}

