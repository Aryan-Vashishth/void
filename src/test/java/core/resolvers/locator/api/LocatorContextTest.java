package core.resolvers.locator.api;

import elements.api.Element;
import elements.api.capability.Clickable;
import elements.fixture.ConventionalTestPage;
import org.testng.annotations.Test;
import tests.demo.pages.DemoLoginPage;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link LocatorContext} and {@link DefaultLocatorContext}.
 *
 * <p>Verifies the resolver-side file-name resolution contract introduced in Phase 13.</p>
 */
public class LocatorContextTest {

    private static final LocatorContext CTX = DefaultLocatorContext.INSTANCE;

    // -----------------------------------------------------------------------
    // DefaultLocatorContext — singleton
    // -----------------------------------------------------------------------

    @Test
    public void defaultLocatorContext_isSingleton() {
        assertSame(DefaultLocatorContext.INSTANCE, DefaultLocatorContext.INSTANCE);
    }

    // -----------------------------------------------------------------------
    // resolveFileName — conventional path (Phase 5)
    // -----------------------------------------------------------------------

    @Test
    public void resolveFileName_conventionalElement_returnsPackageQualifiedPath() {
        String name = CTX.resolveFileName(ConventionalTestPage.Buttons.SUBMIT);
        assertEquals(name, "elements/fixture/ConventionalTestPage/locators.json");
    }

    @Test
    public void resolveFileName_demoCredentials_returnsConventionalPath() {
        String name = CTX.resolveFileName(DemoLoginPage.Credentials.USERNAME_INPUT);
        assertEquals(name, "tests/demo/pages/DemoLoginPage/locators.json");
    }

    // -----------------------------------------------------------------------
    // resolveFileName — explicit getExternalFileName() override
    // -----------------------------------------------------------------------

    @Test
    public void resolveFileName_explicitOverride_returnsThatFile() {
        Element e = new Clickable() {
            @Override public String getExternalFileName() { return "my-locators.json"; }
            @Override public String getTriggerLocator()   { return "MY_KEY"; }
        };
        assertEquals(CTX.resolveFileName(e), "my-locators.json");
    }

    @Test
    public void resolveFileName_nullOverride_returnsNull() {
        Element e = new Clickable() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getTriggerLocator()   { return "//button"; }
        };
        assertNull(CTX.resolveFileName(e));
    }

    // -----------------------------------------------------------------------
    // LocatorResolver.Builder — locatorContext wired
    // -----------------------------------------------------------------------

    @Test
    public void resolverBuilder_defaultLocatorContextIsDefault() {
        LocatorResolver r = LocatorResolver.builder().build();
        assertSame(r.locatorContext(), DefaultLocatorContext.INSTANCE);
    }

    @Test
    public void resolverBuilder_customLocatorContextIsHonoured() {
        LocatorContext custom = element -> "custom-file.json";
        LocatorResolver r = LocatorResolver.builder().locatorContext(custom).build();
        assertSame(r.locatorContext(), custom);
    }

    // -----------------------------------------------------------------------
    // Integration — resolver uses LocatorContext for file resolution
    // -----------------------------------------------------------------------

    @Test
    public void resolver_usesLocatorContext_overridesFileResolution() {
        // Install a custom context that always redirects to the conventional test JSON
        LocatorContext redirect = element ->
                "elements/fixture/ConventionalTestPage/locators.json";
        LocatorResolver r = LocatorResolver.builder().locatorContext(redirect).build();

        // ConventionalTestPage.Buttons.SUBMIT -> "SUBMIT.TRIGGER" key -> should resolve via redirect
        org.openqa.selenium.By by = r.resolve(ConventionalTestPage.Buttons.SUBMIT);
        assertEquals(by.toString(),
                org.openqa.selenium.By.xpath("//button[@id='submit']").toString());
    }
}
