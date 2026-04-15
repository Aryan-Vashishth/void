package core.resolvers.locator;

import elements.api.*;
import org.openqa.selenium.By;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link ElementLocatorResolverV1}.
 * <p>
 * All tests are driver-free: they exercise pure string/locator resolution logic only.
 * File-based tests use the fixture at
 * {@code src/test/resources/locators/test-locators.properties}.
 * </p>
 *
 * <p><b>Note on file lookup:</b> {@code ElementLocatorResolverV1} prepends
 * {@code "locators/"} (its {@code CLASSPATH_BASE}) to the {@code fileName} argument
 * before querying the classpath. Therefore pass {@code "test-locators.properties"}
 * (without the prefix) so it resolves to {@code "locators/test-locators.properties"}.</p>
 *
 * Coverage:
 * <ul>
 *   <li>{@link ElementLocatorResolverV1#countPlaceholders(String)}</li>
 *   <li>{@link ElementLocatorResolverV1#resolveLocatorTemplate(String, Object...)} – including last-arg padding</li>
 *   <li>{@link ElementLocatorResolverV1#getRawLocator(String, String)} – hardcoded and file-based</li>
 *   <li>{@link ElementLocatorResolverV1#getLocatorTemplate(String, String)}</li>
 *   <li>{@link ElementLocatorResolverV1#getLocator(String, String, Object...)} – full pipeline</li>
 *   <li>{@link ElementLocatorResolverV1#getLocator(Element)} – element-based</li>
 *   <li>{@link ElementLocatorResolverV1#getLocatorCaseInsensitive(String, String, Object...)}</li>
 *   <li>Dropdown / SearchField / Searchable helpers</li>
 * </ul>
 */
public class ElementLocatorResolverV1Test {

    // =====================================================================
    // Shared fixture – note: resolver prepends "locators/" automatically
    // =====================================================================

    private static final String PROP_FILE = "test-locators.properties";

    // =====================================================================
    // Helper factories for minimal anonymous interface implementations
    // =====================================================================

    private static Element hardcodedElement(String template, Object... args) {
        return new Element() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return template; }
            @Override public Object[] getArgs()           { return args; }
        };
    }

    private static Clickable hardcodedClickable(String trigger) {
        return new Clickable() {
            @Override public String getTriggerLocator()   { return trigger; }
            @Override public String getExternalFileName() { return null; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    private static TextInputField hardcodedTextInput(String inputLocator) {
        return new TextInputField() {
            @Override public String getInputLocator()     { return inputLocator; }
            @Override public String getExternalFileName() { return null; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    private static Dropdown hardcodedDropdown(String trigger, String list) {
        return new Dropdown() {
            @Override public String getTriggerLocator()   { return trigger; }
            @Override public String getListLocator()      { return list; }
            @Override public String getExternalFileName() { return null; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    private static MultipleIdenticalDropdowns hardcodedMultiDropdown(String trigger, String list, Object... args) {
        return new MultipleIdenticalDropdowns() {
            @Override public String getTriggerLocator()   { return trigger; }
            @Override public String getListLocator()      { return list; }
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return trigger; }
            @Override public Object[] getArgs()           { return args; }
        };
    }

    private static SearchField hardcodedSearchField(String inputLoc, String buttonLoc) {
        return new SearchField() {
            @Override public String getSearchInputLocator()  { return inputLoc; }
            @Override public String getSearchButtonLocator() { return buttonLoc; }
            @Override public String getExternalFileName()    { return null; }
            @Override public Object[] getArgs()              { return new Object[0]; }
        };
    }

    private static Searchable hardcodedSearchable(String inputLoc, String buttonLoc, String resultLoc, Object... args) {
        return new Searchable() {
            @Override public String getSearchInputLocator()  { return inputLoc; }
            @Override public String getSearchButtonLocator() { return buttonLoc; }
            @Override public String getSearchResultLocator() { return resultLoc; }
            @Override public String getExternalFileName()    { return null; }
            @Override public Object[] getArgs()              { return args; }
        };
    }

    // =====================================================================
    // countPlaceholders()
    // =====================================================================

    @Test(description = "null template → 0 placeholders")
    public void countPlaceholders_null_returnsZero() {
        assertEquals(ElementLocatorResolverV1.countPlaceholders(null), 0);
    }

    @Test(description = "Empty string → 0 placeholders")
    public void countPlaceholders_empty_returnsZero() {
        assertEquals(ElementLocatorResolverV1.countPlaceholders(""), 0);
    }

    @Test(description = "Template without %s → 0 placeholders")
    public void countPlaceholders_noPlaceholder_returnsZero() {
        assertEquals(ElementLocatorResolverV1.countPlaceholders("//div[@id='main']"), 0);
    }

    @Test(description = "Template with one %s → 1 placeholder")
    public void countPlaceholders_singlePlaceholder_returnsOne() {
        assertEquals(ElementLocatorResolverV1.countPlaceholders("//input[@placeholder='%s']"), 1);
    }

    @Test(description = "Template with two %s → 2 placeholders")
    public void countPlaceholders_twoPlaceholders_returnsTwo() {
        assertEquals(ElementLocatorResolverV1.countPlaceholders("//tr[@data-row='%s']//td[@data-col='%s']"), 2);
    }

    @Test(description = "Template with %S (uppercase) is counted case-insensitively")
    public void countPlaceholders_uppercaseS_isCounted() {
        assertEquals(ElementLocatorResolverV1.countPlaceholders("//span[@class='%S']"), 1);
    }

    // =====================================================================
    // resolveLocatorTemplate()
    // =====================================================================

    @Test(description = "null template → returns null")
    public void resolveLocatorTemplate_null_returnsNull() {
        assertNull(ElementLocatorResolverV1.resolveLocatorTemplate(null));
    }

    @Test(description = "Template with no %s → returned unchanged")
    public void resolveLocatorTemplate_noPlaceholder_returnsOriginal() {
        String tpl = "//button[@type='submit']";
        assertEquals(ElementLocatorResolverV1.resolveLocatorTemplate(tpl), tpl);
    }

    @Test(description = "Single %s with one arg → substituted correctly")
    public void resolveLocatorTemplate_singlePlaceholder_substituted() {
        String result = ElementLocatorResolverV1.resolveLocatorTemplate(
                "//input[@placeholder='%s']", "Search");
        assertEquals(result, "//input[@placeholder='Search']");
    }

    @Test(description = "Two %s with two args → both substituted in order")
    public void resolveLocatorTemplate_twoPlaceholders_bothSubstituted() {
        String result = ElementLocatorResolverV1.resolveLocatorTemplate(
                "//tr[@data-row='%s']//td[@data-col='%s']", "3", "2");
        assertEquals(result, "//tr[@data-row='3']//td[@data-col='2']");
    }

    @Test(description = "More %s than args → last arg is repeated (padding behaviour)")
    public void resolveLocatorTemplate_morePlaceholdersThanArgs_lastArgRepeated() {
        // Template has 2 %s, only 1 arg supplied – resolver pads last arg
        String result = ElementLocatorResolverV1.resolveLocatorTemplate(
                "//tr[%s]//td[%s]", "5");
        assertEquals(result, "//tr[5]//td[5]",
                "Expected last arg to be repeated for the extra placeholder");
    }

    @Test(description = "Three %s with one arg → single arg repeated for all slots")
    public void resolveLocatorTemplate_threePlaceholders_oneArg_allRepeated() {
        String result = ElementLocatorResolverV1.resolveLocatorTemplate(
                "[%s][%s][%s]", "X");
        assertEquals(result, "[X][X][X]");
    }

    // =====================================================================
    // getRawLocator()
    // =====================================================================

    @Test(description = "null fileName (hardcoded) → key returned as-is")
    public void getRawLocator_nullFile_returnsKeyAsTemplate() {
        String key = "xpath=//button[@type='submit']";
        assertEquals(ElementLocatorResolverV1.getRawLocator(null, key), key);
    }

    @Test(description = "Valid properties file + existing key → raw value returned")
    public void getRawLocator_propertiesFile_existingKey_returnsValue() {
        String raw = ElementLocatorResolverV1.getRawLocator(PROP_FILE, "BUTTON_TRIGGER");
        assertNotNull(raw);
        assertTrue(raw.contains("submit"), "Expected 'submit' in raw locator; got: " + raw);
    }

    @Test(description = "Valid properties file + template key → raw template returned (with %s)")
    public void getRawLocator_propertiesFile_templateKey_returnsRawTemplate() {
        String raw = ElementLocatorResolverV1.getRawLocator(PROP_FILE, "TEMPLATE_WITH_ARG");
        assertNotNull(raw);
        assertTrue(raw.contains("%s"), "Expected %s in raw template; got: " + raw);
    }

    @Test(
            description        = "Missing key in properties file → RuntimeException",
            expectedExceptions = RuntimeException.class
    )
    public void getRawLocator_propertiesFile_missingKey_throwsRuntimeException() {
        ElementLocatorResolverV1.getRawLocator(PROP_FILE, "COMPLETELY_MISSING_KEY_XYZ");
    }

    // =====================================================================
    // getLocatorTemplate()
    // =====================================================================

    @Test(description = "Key with %s placeholders → getLocatorTemplate returns the template string")
    public void getLocatorTemplate_keyWithPlaceholder_returnsTemplate() {
        String tpl = ElementLocatorResolverV1.getLocatorTemplate(PROP_FILE, "TEMPLATE_WITH_ARG");
        assertNotNull(tpl, "Expected a template string for TEMPLATE_WITH_ARG");
        assertTrue(tpl.contains("%s"));
    }

    @Test(description = "Key without %s → getLocatorTemplate returns null (no template needed)")
    public void getLocatorTemplate_keyWithoutPlaceholder_returnsNull() {
        String tpl = ElementLocatorResolverV1.getLocatorTemplate(PROP_FILE, "BUTTON_TRIGGER");
        assertNull(tpl, "Expected null when no placeholder is present");
    }

    @Test(description = "Hardcoded null-file template with %s → returns template")
    public void getLocatorTemplate_hardcoded_withPlaceholder_returnsTemplate() {
        String tpl = ElementLocatorResolverV1.getLocatorTemplate(null, "//option[text()='%s']");
        assertNotNull(tpl);
        assertTrue(tpl.contains("%s"));
    }

    // =====================================================================
    // getLocator(String, String, Object...) – full pipeline
    // =====================================================================

    @Test(description = "Hardcoded XPath (null file) → By.xpath returned")
    public void getLocator_string_hardcoded_xpath_returnsByXpath() {
        By result = ElementLocatorResolverV1.getLocator(null, "//div[@id='app']");
        assertEquals(result.toString(), By.xpath("//div[@id='app']").toString());
    }

    @Test(description = "Hardcoded CSS selector (null file) → By.cssSelector returned")
    public void getLocator_string_hardcoded_css_returnsByCssSelector() {
        By result = ElementLocatorResolverV1.getLocator(null, "div.wrapper > span");
        assertEquals(result.toString(), By.cssSelector("div.wrapper > span").toString());
    }

    @Test(description = "Hardcoded id= prefix (null file) → By.id returned")
    public void getLocator_string_hardcoded_idPrefix_returnsById() {
        By result = ElementLocatorResolverV1.getLocator(null, "id=mainHeader");
        assertEquals(result.toString(), By.id("mainHeader").toString());
    }

    @Test(description = "Hardcoded template with %s argument → argument substituted and By resolved")
    public void getLocator_string_hardcoded_withArg_resolved() {
        By result = ElementLocatorResolverV1.getLocator(null,
                "//input[@placeholder='%s']", "Search");
        assertEquals(result.toString(), By.xpath("//input[@placeholder='Search']").toString());
    }

    @Test(description = "Properties file + key without args → By resolved from file value")
    public void getLocator_string_propertiesFile_noArgs_returnsByXpath() {
        By result = ElementLocatorResolverV1.getLocator(PROP_FILE, "BUTTON_TRIGGER");
        assertEquals(result.toString(), By.xpath("//button[@id='submit']").toString());
    }

    @Test(description = "Properties file + css= prefixed key → By.cssSelector")
    public void getLocator_string_propertiesFile_cssPrefix_returnsByCssSelector() {
        By result = ElementLocatorResolverV1.getLocator(PROP_FILE, "LOGIN_INPUT");
        assertEquals(result.toString(), By.cssSelector("input#username").toString());
    }

    @Test(description = "Properties file + id= prefixed key → By.id")
    public void getLocator_string_propertiesFile_idPrefix_returnsById() {
        By result = ElementLocatorResolverV1.getLocator(PROP_FILE, "SUBMIT_BUTTON");
        assertEquals(result.toString(), By.id("submitBtn").toString());
    }

    @Test(description = "Properties file + template key + one arg → arg substituted correctly")
    public void getLocator_string_propertiesFile_withArg_templateResolved() {
        By result = ElementLocatorResolverV1.getLocator(PROP_FILE, "TEMPLATE_WITH_ARG", "Email");
        assertEquals(result.toString(),
                By.xpath("//input[@placeholder='Email']").toString());
    }

    @Test(description = "Properties file + two-arg template → both args substituted")
    public void getLocator_string_propertiesFile_twoArgs_bothSubstituted() {
        By result = ElementLocatorResolverV1.getLocator(PROP_FILE, "TEMPLATE_TWO_ARGS", "1", "3");
        assertEquals(result.toString(),
                By.xpath("//tr[@data-row='1']//td[@data-col='3']").toString());
    }

    @Test(description = "Properties file + two-arg template + only one arg supplied → last arg padded")
    public void getLocator_string_propertiesFile_twoArgsOnlyOneSupplied_lastArgPadded() {
        By result = ElementLocatorResolverV1.getLocator(PROP_FILE, "TEMPLATE_TWO_ARGS", "7");
        assertEquals(result.toString(),
                By.xpath("//tr[@data-row='7']//td[@data-col='7']").toString());
    }

    // =====================================================================
    // getLocator(Element) – Element-based resolution
    // =====================================================================

    @Test(description = "Element with hardcoded XPath primary → correct By returned")
    public void getLocator_element_hardcodedXpath_resolved() {
        Element e = hardcodedElement("//button[@type='submit']");
        assertEquals(ElementLocatorResolverV1.getLocator(e).toString(),
                By.xpath("//button[@type='submit']").toString());
    }

    @Test(description = "Element with hardcoded id= primary → By.id returned")
    public void getLocator_element_hardcodedIdPrefix_resolved() {
        Element e = hardcodedElement("id=saveBtn");
        assertEquals(ElementLocatorResolverV1.getLocator(e).toString(),
                By.id("saveBtn").toString());
    }

    @Test(description = "Element with primary template + args → template resolved before returning By")
    public void getLocator_element_withArgs_templateResolved() {
        Element e = hardcodedElement("//option[text()='%s']", "Admin");
        assertEquals(ElementLocatorResolverV1.getLocator(e).toString(),
                By.xpath("//option[text()='Admin']").toString());
    }

    @Test(description = "Clickable element → trigger locator returned as By")
    public void getLocator_clickableElement_triggerResolved() {
        Clickable c = hardcodedClickable("//button[@id='save']");
        assertEquals(ElementLocatorResolverV1.getLocator(c).toString(),
                By.xpath("//button[@id='save']").toString());
    }

    @Test(description = "TextInputField element → input locator returned as By.cssSelector")
    public void getLocator_textInputField_inputResolved() {
        TextInputField f = hardcodedTextInput("css=input.form-control");
        assertEquals(ElementLocatorResolverV1.getLocator(f).toString(),
                By.cssSelector("input.form-control").toString());
    }

    @Test(description = "File-backed element (properties) → By resolved from file value")
    public void getLocator_element_fileBacked_resolved() {
        Element e = new Element() {
            @Override public String getExternalFileName() { return PROP_FILE; }
            @Override public String getPrimaryLocator()   { return "BUTTON_TRIGGER"; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
        assertEquals(ElementLocatorResolverV1.getLocator(e).toString(),
                By.xpath("//button[@id='submit']").toString());
    }

    // =====================================================================
    // getLocatorCaseInsensitive()
    // =====================================================================

    @Test(description = "Case-insensitive resolver lowercases string args before substitution")
    public void getLocatorCaseInsensitive_argsAreLowercased() {
        By result = ElementLocatorResolverV1.getLocatorCaseInsensitive(
                null, "//option[text()='%s']", "ADMIN");
        // args lowercased: "admin"
        assertEquals(result.toString(),
                By.xpath("//option[text()='admin']").toString());
    }

    @Test(description = "Case-insensitive resolver with already-lowercase arg → unchanged")
    public void getLocatorCaseInsensitive_alreadyLowercaseArg_unchanged() {
        By result = ElementLocatorResolverV1.getLocatorCaseInsensitive(
                null, "//option[text()='%s']", "admin");
        assertEquals(result.toString(), By.xpath("//option[text()='admin']").toString());
    }

    @Test(description = "Case-insensitive Element overload lowercases args before resolution")
    public void getLocatorCaseInsensitive_elementOverload_lowercasesArgs() {
        Element e = hardcodedElement("//label[text()='%s']", "HELLO WORLD");
        By result = ElementLocatorResolverV1.getLocatorCaseInsensitive(e);
        assertEquals(result.toString(),
                By.xpath("//label[text()='hello world']").toString());
    }

    // =====================================================================
    // getDropdownTriggerLocator(Dropdown) / getDropdownListLocator(Dropdown)
    // =====================================================================

    @Test(description = "getDropdownTriggerLocator → trigger locator resolved")
    public void getDropdownTriggerLocator_hardcoded_resolvesCorrectly() {
        Dropdown d = hardcodedDropdown(
                "//button[@data-toggle='dropdown']", "//ul[@class='dropdown-menu']");
        By result = ElementLocatorResolverV1.getDropdownTriggerLocator(d);
        assertEquals(result.toString(),
                By.xpath("//button[@data-toggle='dropdown']").toString());
    }

    @Test(description = "getDropdownListLocator → list locator resolved")
    public void getDropdownListLocator_hardcoded_resolvesCorrectly() {
        Dropdown d = hardcodedDropdown(
                "//button[@data-toggle='dropdown']", "//ul[@class='dropdown-menu']");
        By result = ElementLocatorResolverV1.getDropdownListLocator(d);
        assertEquals(result.toString(),
                By.xpath("//ul[@class='dropdown-menu']").toString());
    }

    @Test(description = "getDropdownTriggerLocator with CSS selector → By.cssSelector")
    public void getDropdownTriggerLocator_cssSelector_returnsByCss() {
        Dropdown d = hardcodedDropdown("css=button.dropdown-toggle", "css=ul.dropdown-menu");
        By result = ElementLocatorResolverV1.getDropdownTriggerLocator(d);
        assertEquals(result.toString(), By.cssSelector("button.dropdown-toggle").toString());
    }

    // =====================================================================
    // getDropdownTriggerLocator / ListLocator (MultipleIdenticalDropdowns, Integer)
    // =====================================================================

    @Test(description = "MultiDropdown trigger with null index → normal args used")
    public void getDropdownTriggerLocator_multiDropdown_nullIndex_usesNormalArgs() {
        MultipleIdenticalDropdowns m = hardcodedMultiDropdown(
                "//button[@id='dd']", "//ul[@id='list']");
        By result = ElementLocatorResolverV1.getDropdownTriggerLocator(m, null);
        assertEquals(result.toString(), By.xpath("//button[@id='dd']").toString());
    }

    @Test(description = "MultiDropdown trigger with index → index prepended to args, template resolved")
    public void getDropdownTriggerLocator_multiDropdown_withIndex_indexPrependedToArgs() {
        MultipleIdenticalDropdowns m = hardcodedMultiDropdown(
                "(//table//tr)[%s]//button", "(//table//tr)[%s]//ul");
        By result = ElementLocatorResolverV1.getDropdownTriggerLocator(m, 3);
        assertEquals(result.toString(),
                By.xpath("(//table//tr)[3]//button").toString());
    }

    @Test(description = "MultiDropdown list with null index → normal args used")
    public void getDropdownListLocator_multiDropdown_nullIndex_usesNormalArgs() {
        MultipleIdenticalDropdowns m = hardcodedMultiDropdown(
                "//button[@id='dd']", "//ul[@id='list']");
        By result = ElementLocatorResolverV1.getDropdownListLocator(m, null);
        assertEquals(result.toString(), By.xpath("//ul[@id='list']").toString());
    }

    @Test(description = "MultiDropdown list with index → index used in template resolution")
    public void getDropdownListLocator_multiDropdown_withIndex_templateResolved() {
        MultipleIdenticalDropdowns m = hardcodedMultiDropdown(
                "(//table//tr)[%s]//button", "(//table//tr)[%s]//ul");
        By result = ElementLocatorResolverV1.getDropdownListLocator(m, 2);
        assertEquals(result.toString(),
                By.xpath("(//table//tr)[2]//ul").toString());
    }

    // =====================================================================
    // getSearchFieldLocator(SearchField)
    // =====================================================================

    @Test(description = "getSearchFieldLocator → search input locator resolved")
    public void getSearchFieldLocator_hardcoded_resolvesSearchInput() {
        SearchField sf = hardcodedSearchField(
                "css=input[type='search']", "css=button.search-btn");
        By result = ElementLocatorResolverV1.getSearchFieldLocator(sf);
        assertEquals(result.toString(), By.cssSelector("input[type='search']").toString());
    }

    @Test(description = "getSearchFieldLocator with XPath input → By.xpath returned")
    public void getSearchFieldLocator_xpathInput_returnsByXpath() {
        SearchField sf = hardcodedSearchField(
                "//input[@role='searchbox']", "//button[@aria-label='Search']");
        By result = ElementLocatorResolverV1.getSearchFieldLocator(sf);
        assertEquals(result.toString(), By.xpath("//input[@role='searchbox']").toString());
    }

    // =====================================================================
    // getSearchResultLocator(Searchable, Object...)
    // =====================================================================

    @Test(description = "getSearchResultLocator with resultArgs → resultArgs used for template")
    public void getSearchResultLocator_withResultArgs_resultArgsUsed() {
        Searchable s = hardcodedSearchable(
                "css=input.search", "css=button.go",
                "//li[text()='%s']",
                "DefaultArg");
        By result = ElementLocatorResolverV1.getSearchResultLocator(s, "OverrideArg");
        assertEquals(result.toString(), By.xpath("//li[text()='OverrideArg']").toString());
    }

    @Test(description = "getSearchResultLocator without resultArgs → searchable's own args used")
    public void getSearchResultLocator_noResultArgs_usesElementArgs() {
        Searchable s = hardcodedSearchable(
                "css=input.search", "css=button.go",
                "//li[text()='%s']",
                "MyResult");
        By result = ElementLocatorResolverV1.getSearchResultLocator(s);
        assertEquals(result.toString(), By.xpath("//li[text()='MyResult']").toString());
    }

    @Test(
            description        = "getSearchResultLocator with null result key → IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class
    )
    public void getSearchResultLocator_nullResultKey_throwsIllegalArgument() {
        Searchable s = hardcodedSearchable("css=input", "css=button", null);
        ElementLocatorResolverV1.getSearchResultLocator(s);
    }

    @Test(
            description        = "getSearchResultLocator with blank result key → IllegalArgumentException",
            expectedExceptions = IllegalArgumentException.class
    )
    public void getSearchResultLocator_blankResultKey_throwsIllegalArgument() {
        Searchable s = hardcodedSearchable("css=input", "css=button", "   ");
        ElementLocatorResolverV1.getSearchResultLocator(s);
    }

    // =====================================================================
    // DataProvider – parametric hardcoded locator pipeline
    // =====================================================================

    @DataProvider(name = "hardcodedLocators")
    public Object[][] hardcodedLocatorData() {
        return new Object[][]{
            {"//div",                      By.xpath("//div").toString()},
            {"/html/body",                 By.xpath("/html/body").toString()},
            {"(//tr)[last()]",             By.xpath("(//tr)[last()]").toString()},
            {".//span",                    By.xpath(".//span").toString()},
            {"div.container",              By.cssSelector("div.container").toString()},
            {"id=myId",                    By.id("myId").toString()},
            {"name=q",                     By.name("q").toString()},
            {"class=btn",                  By.className("btn").toString()},
            {"tag=input",                  By.tagName("input").toString()},
            {"css=#form > input",          By.cssSelector("#form > input").toString()},
            {"xpath=//h1",                 By.xpath("//h1").toString()},
            {"linkText=Click",             By.linkText("Click").toString()},
            {"partialLinkText=Read",       By.partialLinkText("Read").toString()},
        };
    }

    @Test(
            dataProvider  = "hardcodedLocators",
            description   = "All hardcoded locator types resolve to the correct By strategy"
    )
    public void getLocator_string_hardcoded_parametrized(String template, String expectedToString) {
        By result = ElementLocatorResolverV1.getLocator(null, template);
        assertEquals(result.toString(), expectedToString, "Template: '" + template + "'");
    }
}

