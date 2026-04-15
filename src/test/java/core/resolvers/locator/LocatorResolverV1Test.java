package core.resolvers.locator;

import elements.meta.ElementRole;
import elements.api.*;
import org.openqa.selenium.By;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link LocatorResolverV1}.
 * <p>
 * All tests are driver-free: they exercise pure string/locator resolution logic only.
 * File-based tests use the fixture at
 * {@code src/test/resources/locators/test-locators.properties} and
 * {@code src/test/resources/locators/json/test-locators.json}.
 * </p>
 *
 * Coverage:
 * <ul>
 *   <li>{@link LocatorResolverV1#resolveLocatorTemplate(String, Object...)} – all placeholder variants</li>
 *   <li>{@link LocatorResolverV1#getRawLocator(String, String)} – hardcoded, .properties, .json, error cases</li>
 *   <li>{@link LocatorResolverV1#getLocator(String, String, Object...)} – full pipeline via strings</li>
 *   <li>{@link LocatorResolverV1#getLocator(Element)} – Element-based resolution (hardcoded)</li>
 *   <li>{@link LocatorResolverV1#getLocator(Element, ElementRole, Object...)} – role-targeted resolution</li>
 *   <li>{@link LocatorResolverV1#getBestAvailable(Element, Object...)} – PRIMARY → SECONDARY → first fallback</li>
 * </ul>
 */

public class LocatorResolverV1Test {

    // =====================================================================
    // Shared fixture paths
    // =====================================================================

    private static final String PROP_FILE = "locators/test-locators.properties";
    private static final String JSON_FILE = "test-locators.json";   // base prepended by JsonLocatorReaderV1

    // =====================================================================
    // Helper: minimal anonymous Element implementations
    // =====================================================================

    /** Plain Element with a hardcoded (null-file) primary locator. */
    private static Element hardcodedElement(String locatorTemplate, Object... args) {
        return new Element() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return locatorTemplate; }
            @Override public Object[] getArgs()           { return args; }
        };
    }

    /** Element with PRIMARY + SECONDARY (both hardcoded). */
    private static Element dualElement(String primary, String secondary) {
        return new Element() {
            @Override public String getExternalFileName()  { return null; }
            @Override public String getPrimaryLocator()    { return primary; }
            @Override public String getSecondaryLocator()  { return secondary; }
            @Override public Object[] getArgs()            { return new Object[0]; }
        };
    }

    /** Element with only a SECONDARY locator (PRIMARY is null). */
    private static Element secondaryOnlyElement(String secondary) {
        return new Element() {
            @Override public String getExternalFileName()  { return null; }
            @Override public String getPrimaryLocator()    { return null; }
            @Override public String getSecondaryLocator()  { return secondary; }
            @Override public Object[] getArgs()            { return new Object[0]; }
        };
    }

    /** Element with no locators at all. */
    private static Element emptyElement() {
        return new Element() {
            @Override public String getExternalFileName()  { return null; }
            @Override public String getPrimaryLocator()    { return null; }
            @Override public Object[] getArgs()            { return new Object[0]; }
            @Override public Map<ElementRole, String> getAllLocatorRoles() { return new LinkedHashMap<>(); }
        };
    }

    /** Minimal Clickable (hardcoded, TRIGGER role). */
    private static Clickable hardcodedClickable(String triggerLocator) {
        return new Clickable() {
            @Override public String getTriggerLocator()   { return triggerLocator; }
            @Override public String getExternalFileName() { return null; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    /** Minimal TextInputField (hardcoded, INPUT role). */
    private static TextInputField hardcodedTextInput(String inputLocator) {
        return new TextInputField() {
            @Override public String getInputLocator()     { return inputLocator; }
            @Override public String getExternalFileName() { return null; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    /** Minimal Dropdown (hardcoded, TRIGGER + LIST roles). */
    private static Dropdown hardcodedDropdown(String trigger, String list) {
        return new Dropdown() {
            @Override public String getTriggerLocator()   { return trigger; }
            @Override public String getListLocator()      { return list; }
            @Override public String getExternalFileName() { return null; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    // =====================================================================
    // resolveLocatorTemplate()
    // =====================================================================

    @Test(description = "Template with no %s placeholder → returned unchanged")
    public void resolveLocatorTemplate_noPlaceholder_returnsOriginal() {
        String template = "//div[@id='main']";
        String result   = LocatorResolverV1.resolveLocatorTemplate(template);
        assertEquals(result, template);
    }

    @Test(description = "null template → returns null")
    public void resolveLocatorTemplate_null_returnsNull() {
        assertNull(LocatorResolverV1.resolveLocatorTemplate(null));
    }

    @Test(description = "Single %s is substituted with the supplied argument")
    public void resolveLocatorTemplate_singlePlaceholder_substituted() {
        String result = LocatorResolverV1.resolveLocatorTemplate(
                "//input[@placeholder='%s']", "Username");
        assertEquals(result, "//input[@placeholder='Username']");
    }

    @Test(description = "Two %s placeholders are filled in order")
    public void resolveLocatorTemplate_twoPlaceholders_substitutedInOrder() {
        String result = LocatorResolverV1.resolveLocatorTemplate(
                "//tr[@data-row='%s']//td[@data-col='%s']", "3", "2");
        assertEquals(result, "//tr[@data-row='3']//td[@data-col='2']");
    }

    @Test(description = "Indexed placeholder (%1$s, %2$s) is resolved correctly")
    public void resolveLocatorTemplate_indexedPlaceholders_resolved() {
        String result = LocatorResolverV1.resolveLocatorTemplate(
                "//div[@data-a='%1$s' and @data-b='%2$s']", "foo", "bar");
        assertEquals(result, "//div[@data-a='foo' and @data-b='bar']");
    }

    @Test(description = "Empty args array with no placeholder → template returned as-is")
    public void resolveLocatorTemplate_noPlaceholder_emptyArgs_returnsOriginal() {
        String template = "css=.btn-primary";
        assertEquals(LocatorResolverV1.resolveLocatorTemplate(template, new Object[0]), template);
    }

    @Test(
        description        = "More %s placeholders than args → IllegalStateException wrapping format error",
        expectedExceptions = IllegalStateException.class
    )
    public void resolveLocatorTemplate_tooFewArgs_throwsIllegalState() {
        // Two %s but zero args
        LocatorResolverV1.resolveLocatorTemplate("//td[%s]//span[%s]");
    }

    // =====================================================================
    // getRawLocator()
    // =====================================================================

    @Test(description = "null fileName (hardcoded) → key is returned as the template")
    public void getRawLocator_nullFileName_returnsKeyAsTemplate() {
        String key = "xpath=//button[@type='submit']";
        assertEquals(LocatorResolverV1.getRawLocator(null, key), key);
    }

    @Test(description = "Valid .properties file + existing key → raw value returned")
    public void getRawLocator_propertiesFile_existingKey_returnsValue() {
        String raw = LocatorResolverV1.getRawLocator(PROP_FILE, "BUTTON_TRIGGER");
        assertNotNull(raw);
        assertTrue(raw.contains("submit"));
    }

    @Test(description = "Valid .json file + dot-path key → raw value returned")
    public void getRawLocator_jsonFile_dotPathKey_returnsValue() {
        String raw = LocatorResolverV1.getRawLocator(JSON_FILE, "elements.SEARCH_INPUT");
        assertNotNull(raw, "Expected a non-null locator from JSON");
        assertTrue(raw.contains("search"), "Expected search-related locator; got: " + raw);
    }

    @Test(description = "Valid .json file + flat key → raw value returned")
    public void getRawLocator_jsonFile_flatKey_returnsValue() {
        String raw = LocatorResolverV1.getRawLocator(JSON_FILE, "FLAT_KEY");
        assertNotNull(raw);
        assertEquals(raw.trim(), "//span[@id='flat']");
    }

    @Test(
        description        = "Unsupported file extension → IllegalArgumentException",
        expectedExceptions = IllegalArgumentException.class
    )
    public void getRawLocator_unsupportedExtension_throwsIllegalArgument() {
        LocatorResolverV1.getRawLocator("locators.xml", "KEY");
    }

    @Test(
        description        = "Key not found in .properties file → IllegalStateException",
        expectedExceptions = IllegalStateException.class
    )
    public void getRawLocator_propertiesFile_missingKey_throwsIllegalState() {
        LocatorResolverV1.getRawLocator(PROP_FILE, "COMPLETELY_MISSING_KEY_XYZ");
    }

    // =====================================================================
    // getLocator(String, String, Object...) – string-based pipeline
    // =====================================================================

    @Test(description = "Hardcoded XPath template (no file) → By.xpath returned")
    public void getLocator_string_hardcoded_xpath_returnsByXpath() {
        By result = LocatorResolverV1.getLocator(null, "//div[@id='app']");
        assertEquals(result.toString(), By.xpath("//div[@id='app']").toString());
    }

    @Test(description = "Hardcoded CSS template (no file) → By.cssSelector returned")
    public void getLocator_string_hardcoded_css_returnsByCss() {
        By result = LocatorResolverV1.getLocator(null, "div.wrapper > span");
        assertEquals(result.toString(), By.cssSelector("div.wrapper > span").toString());
    }

    @Test(description = "Hardcoded id= prefixed template → By.id returned")
    public void getLocator_string_hardcoded_idPrefix_returnsById() {
        By result = LocatorResolverV1.getLocator(null, "id=mainHeader");
        assertEquals(result.toString(), By.id("mainHeader").toString());
    }

    @Test(description = "Hardcoded template with %s argument → resolved and returned as By")
    public void getLocator_string_hardcoded_withArg_resolved() {
        By result = LocatorResolverV1.getLocator(null,
                "//input[@placeholder='%s']", "Search here");
        assertEquals(result.toString(),
                By.xpath("//input[@placeholder='Search here']").toString());
    }

    @Test(description = "Properties file + key + no args → By resolved from file value")
    public void getLocator_string_propertiesFile_noArgs_returnsByXpath() {
        By result = LocatorResolverV1.getLocator(PROP_FILE, "BUTTON_TRIGGER");
        assertEquals(result.toString(), By.xpath("//button[@id='submit']").toString());
    }

    @Test(description = "Properties file + template key + arg → argument substituted and By resolved")
    public void getLocator_string_propertiesFile_withArg_templateResolved() {
        By result = LocatorResolverV1.getLocator(PROP_FILE, "TEMPLATE_WITH_ARG", "Email");
        assertEquals(result.toString(),
                By.xpath("//input[@placeholder='Email']").toString());
    }

    @Test(description = "Properties file + two-arg template → both args substituted correctly")
    public void getLocator_string_propertiesFile_twoArgs_bothSubstituted() {
        By result = LocatorResolverV1.getLocator(PROP_FILE, "TEMPLATE_TWO_ARGS", "1", "3");
        assertEquals(result.toString(),
                By.xpath("//tr[@data-row='1']//td[@data-col='3']").toString());
    }

    @Test(description = "JSON file + key → By resolved from JSON value")
    public void getLocator_string_jsonFile_flatKey_returnsByXpath() {
        By result = LocatorResolverV1.getLocator(JSON_FILE, "FLAT_KEY");
        assertEquals(result.toString(), By.xpath("//span[@id='flat']").toString());
    }

    @Test(description = "JSON file + dot-path key → nested value resolved to By.cssSelector")
    public void getLocator_string_jsonFile_dotPath_returnsByCss() {
        By result = LocatorResolverV1.getLocator(JSON_FILE, "elements.SEARCH_BUTTON");
        assertEquals(result.toString(), By.cssSelector("button.search-btn").toString());
    }

    // =====================================================================
    // getLocator(Element) – Element-based resolution via getBestAvailable
    // =====================================================================

    @Test(description = "Element with hardcoded PRIMARY → best-available resolves PRIMARY role")
    public void getLocator_element_primary_resolvesCorrectly() {
        Element e  = hardcodedElement("//button[@type='submit']");
        By result  = LocatorResolverV1.getLocator(e);
        assertEquals(result.toString(), By.xpath("//button[@type='submit']").toString());
    }

    @Test(description = "Clickable element → best-available resolves TRIGGER (first role in map)")
    public void getLocator_element_clickable_resolvesTrigger() {
        Clickable c = hardcodedClickable("//button[@id='save']");
        By result   = LocatorResolverV1.getLocator(c);
        assertEquals(result.toString(), By.xpath("//button[@id='save']").toString());
    }

    @Test(description = "TextInputField → best-available resolves INPUT role")
    public void getLocator_element_textInput_resolvesInput() {
        TextInputField f = hardcodedTextInput("css=input.form-control");
        By result        = LocatorResolverV1.getLocator(f);
        assertEquals(result.toString(), By.cssSelector("input.form-control").toString());
    }

    @Test(description = "Element with PRIMARY + SECONDARY → PRIMARY is preferred")
    public void getLocator_element_dual_primaryPreferredOverSecondary() {
        Element e = dualElement("//div[@id='primary']", "//div[@id='secondary']");
        By result = LocatorResolverV1.getLocator(e);
        assertEquals(result.toString(), By.xpath("//div[@id='primary']").toString());
    }

    @Test(
        description        = "Element with no locators → IllegalStateException",
        expectedExceptions = IllegalStateException.class
    )
    public void getLocator_element_noLocators_throwsIllegalState() {
        LocatorResolverV1.getLocator(emptyElement());
    }

    // =====================================================================
    // getLocator(Element, ElementRole, Object...) – role-targeted resolution
    // =====================================================================

    @Test(description = "PRIMARY role requested on basic Element → correct By returned")
    public void getLocator_elementRole_primary_returnsCorrectBy() {
        Element e = hardcodedElement("xpath=//span[@class='label']");
        By result = LocatorResolverV1.getLocator(e, ElementRole.PRIMARY);
        assertEquals(result.toString(), By.xpath("//span[@class='label']").toString());
    }

    @Test(description = "TRIGGER role requested on Clickable → trigger locator resolved")
    public void getLocator_elementRole_trigger_onClickable_resolved() {
        Clickable c = hardcodedClickable("//a[@data-action='open']");
        By result   = LocatorResolverV1.getLocator(c, ElementRole.TRIGGER);
        assertEquals(result.toString(), By.xpath("//a[@data-action='open']").toString());
    }

    @Test(description = "INPUT role requested on TextInputField → input locator resolved")
    public void getLocator_elementRole_input_onTextInputField_resolved() {
        TextInputField f = hardcodedTextInput("id=firstName");
        By result        = LocatorResolverV1.getLocator(f, ElementRole.INPUT);
        assertEquals(result.toString(), By.id("firstName").toString());
    }

    @Test(description = "TRIGGER role requested on Dropdown → trigger locator resolved")
    public void getLocator_elementRole_trigger_onDropdown_resolved() {
        Dropdown d  = hardcodedDropdown("//button[@data-toggle='dropdown']", "//ul[@class='dropdown-menu']");
        By result   = LocatorResolverV1.getLocator(d, ElementRole.TRIGGER);
        assertEquals(result.toString(), By.xpath("//button[@data-toggle='dropdown']").toString());
    }

    @Test(description = "LIST role requested on Dropdown → list locator resolved")
    public void getLocator_elementRole_list_onDropdown_resolved() {
        Dropdown d = hardcodedDropdown("//button[@data-toggle='dropdown']", "//ul[@class='dropdown-menu']");
        By result  = LocatorResolverV1.getLocator(d, ElementRole.LIST);
        assertEquals(result.toString(), By.xpath("//ul[@class='dropdown-menu']").toString());
    }

    @Test(description = "Role not present in element → IllegalStateException")
    @SuppressWarnings("deprecation")
    public void getLocator_elementRole_missingRole_throwsIllegalState() {
        Element e = hardcodedElement("//span");
        // Element has PRIMARY, not TRIGGER
        try {
            LocatorResolverV1.getLocator(e, ElementRole.TRIGGER);
            fail("Expected IllegalStateException for missing role");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("TRIGGER"),
                    "Exception message should mention the missing role");
        }
    }

    @Test(description = "Override args supplied at call site take precedence over element args")
    public void getLocator_elementRole_overrideArgs_usedInsteadOfElementArgs() {
        Element e = new Element() {
            @Override public String getExternalFileName()  { return null; }
            @Override public String getPrimaryLocator()    { return "//row[@index='%s']"; }
            @Override public Object[] getArgs()            { return new Object[]{"original"}; }
        };
        By result = LocatorResolverV1.getLocator(e, ElementRole.PRIMARY, "overridden");
        assertEquals(result.toString(), By.xpath("//row[@index='overridden']").toString());
    }

    // =====================================================================
    // getBestAvailable()
    // =====================================================================

    @Test(description = "PRIMARY present → getBestAvailable uses PRIMARY")
    public void getBestAvailable_primaryPresent_usesPrimary() {
        Element e = dualElement("//div[@id='primary']", "//div[@id='secondary']");
        By result = LocatorResolverV1.getBestAvailable(e);
        assertEquals(result.toString(), By.xpath("//div[@id='primary']").toString());
    }

    @Test(description = "No PRIMARY, SECONDARY present → getBestAvailable falls back to SECONDARY")
    public void getBestAvailable_noPrimary_secondaryPresent_usesSecondary() {
        Element e = secondaryOnlyElement("//div[@id='secondary']");
        By result = LocatorResolverV1.getBestAvailable(e);
        assertEquals(result.toString(), By.xpath("//div[@id='secondary']").toString());
    }

    @Test(description = "No PRIMARY/SECONDARY but TRIGGER role → getBestAvailable uses first available")
    public void getBestAvailable_noStandardRoles_usesFirstAvailableRole() {
        Clickable c = hardcodedClickable("//button[@id='trigger']");
        // Clickable.getAllLocatorRoles() has TRIGGER (not PRIMARY/SECONDARY)
        By result   = LocatorResolverV1.getBestAvailable(c);
        assertEquals(result.toString(), By.xpath("//button[@id='trigger']").toString());
    }

    @Test(
        description        = "Empty roles map → getBestAvailable throws IllegalStateException",
        expectedExceptions = IllegalStateException.class
    )
    public void getBestAvailable_emptyRoles_throwsIllegalState() {
        LocatorResolverV1.getBestAvailable(emptyElement());
    }

    @Test(description = "Override args in getBestAvailable override element's own args")
    public void getBestAvailable_overrideArgs_usedInResolution() {
        Element e = hardcodedElement("//option[text()='%s']", "DefaultLabel");
        By result = LocatorResolverV1.getBestAvailable(e, "OverrideLabel");
        assertEquals(result.toString(),
                By.xpath("//option[text()='OverrideLabel']").toString());
    }

    // =====================================================================
    // DataProvider – string pipeline parametric
    // =====================================================================

    @DataProvider(name = "hardcodedLocators")
    public Object[][] hardcodedLocatorData() {
        return new Object[][]{
            {"//div",                  By.xpath("//div").toString()},
            {"/html/body",             By.xpath("/html/body").toString()},
            {"(//tr)[last()]",         By.xpath("(//tr)[last()]").toString()},
            {".//span",                By.xpath(".//span").toString()},
            {"div.container",          By.cssSelector("div.container").toString()},
            {"id=header",             By.id("header").toString()},
            {"name=q",                By.name("q").toString()},
            {"css=#form > input",     By.cssSelector("#form > input").toString()},
        };
    }

    @Test(
        dataProvider  = "hardcodedLocators",
        description   = "Hardcoded getLocator(null, template) resolves correctly for all By types"
    )
    public void getLocator_string_hardcoded_parametrized(String template, String expectedToString) {
        By result = LocatorResolverV1.getLocator(null, template);
        assertEquals(result.toString(), expectedToString, "Template: '" + template + "'");
    }
}

