package core.resolvers.locator.api;

import elements.api.Element;
import elements.api.capability.Clickable;
import elements.fixture.ConventionalTestPage;
import org.testng.annotations.Test;
import tests.demo.pages.DemoLoginPage;

import java.util.concurrent.atomic.AtomicInteger;

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
        String name = CTX.resolveFileName(DemoLoginPage.Credentials.USERNAME);
        assertEquals(name, "tests/demo/pages/DemoLoginPage/locators.properties");
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
    // Phase 14 — caching: getExternalFileName() runs at most once per element class
    // -----------------------------------------------------------------------

    private static final AtomicInteger PROBE_COUNT = new AtomicInteger();

    // CachingPageA.El and CachingPageB.El are isolated enum classes for caching tests.
    // Each overrides getExternalFileName() with a counter so we can observe call frequency.
    // Constants without a body share their enum class as the cache key.

    private static class CachingPageA {
        enum El implements Element {
            X, Y;
            @Override public String getExternalFileName() { PROBE_COUNT.incrementAndGet(); return "caching-a/locators.json"; }
            @Override public String getPrimaryLocator()   { return name() + ".TRIGGER"; }
        }
    }

    private static class CachingPageB {
        enum El implements Element {
            Z;
            @Override public String getExternalFileName() { PROBE_COUNT.incrementAndGet(); return "caching-b/locators.json"; }
            @Override public String getPrimaryLocator()   { return name() + ".TRIGGER"; }
        }
    }

    @Test
    public void caching_firstCall_invokesGetExternalFileName() {
        // CachingPageA.El.class is only used in these caching tests; first call is a cache miss.
        PROBE_COUNT.set(0);
        CTX.resolveFileName(CachingPageA.El.X);
        assertEquals(PROBE_COUNT.get(), 1, "First call must invoke getExternalFileName exactly once");
    }

    @Test(dependsOnMethods = "caching_firstCall_invokesGetExternalFileName")
    public void caching_subsequentCalls_useCache() {
        // CachingPageA.El.class already cached — repeated calls must not re-invoke resolution.
        PROBE_COUNT.set(0);
        CTX.resolveFileName(CachingPageA.El.X);
        CTX.resolveFileName(CachingPageA.El.Y); // different constant, same enum class → same cache entry
        assertEquals(PROBE_COUNT.get(), 0, "Cached enum class must not re-invoke getExternalFileName");
    }

    @Test(dependsOnMethods = "caching_firstCall_invokesGetExternalFileName")
    public void caching_differentEnumClasses_independentEntries() {
        // CachingPageA.El.class is cached; CachingPageB.El.class is new → triggers its own resolution.
        PROBE_COUNT.set(0);
        String a = CTX.resolveFileName(CachingPageA.El.X);
        String b = CTX.resolveFileName(CachingPageB.El.Z);
        assertEquals(PROBE_COUNT.get(), 1, "Only CachingPageB.El.class should trigger resolution");
        assertNotEquals(a, b, "Different enum classes must resolve to different file names");
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
