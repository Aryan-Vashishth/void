package core.resolvers.locator.api;

import domain.automation.web.resolve.api.ConventionalLocatorPath;
import domain.automation.web.resolve.api.LocatorPaths;
import domain.automation.web.resolve.api.LocatorResolvers;
import elements.fixture.ConventionalTestPage;
import org.openqa.selenium.By;
import org.testng.annotations.Test;
import examples.pages.DemoLoginPage;

import static org.testng.Assert.*;

/**
 * Phase 5 — Deterministic Locator Repository Convention.
 *
 * <p>Covers {@link ConventionalLocatorPath} path derivation and end-to-end
 * resolution for elements whose repositories live at the conventional
 * package-qualified classpath path.</p>
 */
public class ConventionalLocatorPathTest {

    // -------------------------------------------------------------------------
    // ConventionalLocatorPath — derivation algorithm
    // -------------------------------------------------------------------------

    @Test
    public void forClass_derivesPackageQualifiedJsonPath() {
        assertEquals(
            ConventionalLocatorPath.forClass(DemoLoginPage.class),
                "examples/demo/pages/DemoLoginPage/locators.json"
        );
    }

    @Test
    public void forClassProperties_derivesPackageQualifiedPropertiesPath() {
        assertEquals(
            ConventionalLocatorPath.forClassProperties(DemoLoginPage.class),
                "examples/demo/pages/DemoLoginPage/locators.properties"
        );
    }

    @Test
    public void forClass_testFixture_derivesCorrectPath() {
        assertEquals(
            ConventionalLocatorPath.forClass(ConventionalTestPage.class),
            "elements/fixture/ConventionalTestPage/locators.json"
        );
    }

    @Test
    public void dirFor_returnsDirWithTrailingSlash() {
        assertEquals(
            ConventionalLocatorPath.dirFor(DemoLoginPage.class),
                "examples/demo/pages/DemoLoginPage/"
        );
    }

    /** Two page classes with the same simple name in different packages produce distinct paths. */
    @Test
    public void collisionFree_sameSimpleName_differentPackages() {
        String demoPath    = ConventionalLocatorPath.forClass(DemoLoginPage.class);
        String fixturePath = ConventionalLocatorPath.forClass(ConventionalTestPage.class);
        assertNotEquals(demoPath, fixturePath);
        assertTrue(demoPath.startsWith("examples/demo/pages/"));
        assertTrue(fixturePath.startsWith("elements/fixture/"));
    }

    // -------------------------------------------------------------------------
    // getExternalFileName() default — conventional path takes priority
    // -------------------------------------------------------------------------

    @Test
    public void defaultFileName_conventional_returnsPackageQualifiedPath() {
        String name = ConventionalTestPage.Buttons.SUBMIT.getExternalFileName();
        assertEquals(name, "elements/fixture/ConventionalTestPage/locators.json");
    }

    @Test
    public void defaultFileName_demo_returnsPackageQualifiedPath() {
        String name = DemoLoginPage.Credentials.USERNAME.getExternalFileName();
        assertEquals(name, "examples/demo/pages/DemoLoginPage/locators.properties");
    }

    // -------------------------------------------------------------------------
    // End-to-end resolution — conventional path
    // -------------------------------------------------------------------------

    @Test
    public void resolve_submit_fromConventionalPath() {
        By by = LocatorResolvers.strict().resolve(ConventionalTestPage.Buttons.SUBMIT);
        assertEquals(by.toString(), By.xpath("//button[@id='submit']").toString());
    }

    @Test
    public void resolve_cancel_fromConventionalPath() {
        By by = LocatorResolvers.strict().resolve(ConventionalTestPage.Buttons.CANCEL);
        assertEquals(by.toString(), By.xpath("//button[@id='cancel']").toString());
    }

    @Test
    public void resolve_statusMessage_fromConventionalPath() {
        By by = LocatorResolvers.strict().resolve(ConventionalTestPage.Labels.STATUS_MESSAGE);
        assertEquals(by.toString(), By.xpath("//div[@class='status']").toString());
    }

    @Test
    public void resolve_demoCredentials_fromConventionalPath() {
        By by = LocatorResolvers.strict().resolve(DemoLoginPage.Credentials.USERNAME);
        assertEquals(by.toString(), By.xpath("//input[@id='username']").toString());
    }

    // -------------------------------------------------------------------------
    // LocatorPaths.under() — rooted paths pass through unchanged
    // -------------------------------------------------------------------------

    @Test
    public void locatorPaths_rootedPath_notPrefixed() {
        String path = "examples/demo/pages/DemoLoginPage/locators.json";
        assertEquals(LocatorPaths.underJson(path), path);
    }

    @Test
    public void locatorPaths_bareName_prefixed() {
        assertEquals(LocatorPaths.underJson("DemoLoginPage.json"),
                     "locators/json/DemoLoginPage.json");
    }
}
