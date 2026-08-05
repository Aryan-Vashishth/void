package core.resolvers.locator.api;

import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.resolve.api.LocatorResolvers;
import domain.automation.web.vocabulary.role.ElementRole;
import elements.fixture.NestedConventionalPage;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * End-to-end integration tests for the locator resolution pipeline with nested-interface
 * page contracts.
 *
 * <p>Verifies the full chain:
 * {@code UIElement.getExternalFileName()} walks up to outermost page class ->
 * {@code JsonLocatorReader} loads the JSON file ->
 * {@code JsonNodeLookup.findText()} locates keys that start below the JSON root ->
 * {@code LocatorResolver.resolveDescriptor()} returns the correct XPath.</p>
 *
 * <p>Test page: {@link NestedConventionalPage} -- nested interfaces {@code LoginSection}
 * and {@code Header}, plus a direct enum {@code Footer}. Resources at
 * {@code elements/fixture/NestedConventionalPage/locators.json}.</p>
 */
public class NestedInterfaceLocatorResolutionTest {

    // ── getExternalFileName() -- outermost-class walk ─────────────────────────

    @Test
    public void getExternalFileName_nestedInterfaceEnum_pointsToOutermostPageJson() {
        String fileName = NestedConventionalPage.LoginSection.Fields.USERNAME.getExternalFileName();
        assertEquals(fileName, "elements/fixture/NestedConventionalPage/locators.json",
            "Nested-interface enum must resolve to the outermost page's JSON file, not an inner-class path");
    }

    @Test
    public void getExternalFileName_directEnum_pointsToPageJson() {
        String fileName = NestedConventionalPage.Footer.BACK_LINK.getExternalFileName();
        assertEquals(fileName, "elements/fixture/NestedConventionalPage/locators.json");
    }

    // ── Full pipeline: nested-interface enum -> XPath ─────────────────────────

    @Test
    public void resolve_nestedInterfaceTypeable_returnsCorrectXpath() {
        LocatorDescriptor d = LocatorResolvers.strict()
            .resolveDescriptor(NestedConventionalPage.LoginSection.Fields.USERNAME, ElementRole.INPUT);
        assertEquals(d.value(), "//input[@data-test='username']",
            "Resolution pipeline must return the XPath for a nested-interface Typeable constant");
    }

    @Test
    public void resolve_nestedInterfaceTypeable_password_returnsCorrectXpath() {
        LocatorDescriptor d = LocatorResolvers.strict()
            .resolveDescriptor(NestedConventionalPage.LoginSection.Fields.PASSWORD, ElementRole.INPUT);
        assertEquals(d.value(), "//input[@data-test='password']");
    }

    @Test
    public void resolve_nestedInterfaceClickable_returnsCorrectXpath() {
        LocatorDescriptor d = LocatorResolvers.strict()
            .resolveDescriptor(NestedConventionalPage.LoginSection.Actions.LOGIN_BUTTON, ElementRole.TRIGGER);
        assertEquals(d.value(), "//button[@data-test='login-button']");
    }

    @Test
    public void resolve_deeplyNestedReadOnly_returnsCorrectXpath() {
        LocatorDescriptor d = LocatorResolvers.strict()
            .resolveDescriptor(NestedConventionalPage.Header.Labels.PAGE_TITLE, ElementRole.TEXT);
        assertEquals(d.value(), "//h1[@class='page-title']",
            "Resolution must work for enums nested two interfaces deep");
    }

    // ── Full pipeline: direct enum -> XPath (regression: must still work) ─────

    @Test
    public void resolve_directEnum_notBrokenByFix() {
        LocatorDescriptor d = LocatorResolvers.strict()
            .resolveDescriptor(NestedConventionalPage.Footer.BACK_LINK, ElementRole.TRIGGER);
        assertEquals(d.value(), "//a[@id='back']",
            "Direct enum children of the page must still resolve correctly after the fix");
    }
}
