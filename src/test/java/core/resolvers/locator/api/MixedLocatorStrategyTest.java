package core.resolvers.locator.api;

import elements.api.capability.Clickable;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Phase 10 — Mixed Locator Strategies.
 *
 * <p>Validates that a single page interface can freely mix conventional elements
 * (file name derived from the page class, resolved from the classpath) and
 * hardcoded elements (null file name, key returned as-is) without any
 * cross-enum interference.</p>
 */
public class MixedLocatorStrategyTest {

    // -------------------------------------------------------------------------
    // Test page — two enums, two strategies
    // -------------------------------------------------------------------------

    interface MixedStrategyPage {

        /** Conventional: inherits default getExternalFileName() → "MixedStrategyPage.json". */
        enum Buttons implements Clickable {
            SAVE, CANCEL;
        }

        /**
         * Hardcoded: overrides getExternalFileName() to null so the resolver treats
         * getTriggerLocator() as the final XPath template with no file lookup.
         */
        enum Dynamic implements Clickable {
            DELETE_ROW;

            @Override public String getExternalFileName() { return null; }
            @Override public String getTriggerLocator()   { return "//tr[td='%s']//button"; }
            @Override public Object[] getArgs()           { return new Object[]{"Alice"}; }
        }
    }

    // -------------------------------------------------------------------------
    // getExternalFileName() — strategy derivation
    // -------------------------------------------------------------------------

    @Test
    public void conventional_derivesMixedStrategyPageJson() {
        assertEquals(MixedStrategyPage.Buttons.SAVE.getExternalFileName(), "MixedStrategyPage.json");
    }

    @Test
    public void conventional_sameFileForAllEnumConstants() {
        assertEquals(
            MixedStrategyPage.Buttons.SAVE.getExternalFileName(),
            MixedStrategyPage.Buttons.CANCEL.getExternalFileName()
        );
    }

    @Test
    public void hardcoded_returnsNullFileName() {
        assertNull(MixedStrategyPage.Dynamic.DELETE_ROW.getExternalFileName());
    }

    // -------------------------------------------------------------------------
    // End-to-end resolution — conventional path
    // -------------------------------------------------------------------------

    @Test
    public void conventional_save_resolvesFromJsonFile() {
        By by = LocatorResolvers.strict().resolve(MixedStrategyPage.Buttons.SAVE);
        assertEquals(by.toString(), By.xpath("//button[@id='save']").toString());
    }

    @Test
    public void conventional_cancel_resolvesFromJsonFile() {
        By by = LocatorResolvers.strict().resolve(MixedStrategyPage.Buttons.CANCEL);
        assertEquals(by.toString(), By.xpath("//button[@id='cancel']").toString());
    }

    // -------------------------------------------------------------------------
    // End-to-end resolution — hardcoded path
    // -------------------------------------------------------------------------

    @Test
    public void hardcoded_deleteRow_resolvesTriggerLocatorDirectly() {
        By by = LocatorResolvers.strict().resolve(MixedStrategyPage.Dynamic.DELETE_ROW);
        assertEquals(by.toString(), By.xpath("//tr[td='Alice']//button").toString());
    }

    // -------------------------------------------------------------------------
    // Non-interference — resolving one enum does not corrupt the other
    // -------------------------------------------------------------------------

    @Test
    public void noInterference_hardcodedThenConventional() {
        LocatorResolvers.strict().resolve(MixedStrategyPage.Dynamic.DELETE_ROW);
        By by = LocatorResolvers.strict().resolve(MixedStrategyPage.Buttons.SAVE);
        assertEquals(by.toString(), By.xpath("//button[@id='save']").toString());
    }

    @Test
    public void noInterference_conventionalThenHardcoded() {
        LocatorResolvers.strict().resolve(MixedStrategyPage.Buttons.CANCEL);
        By by = LocatorResolvers.strict().resolve(MixedStrategyPage.Dynamic.DELETE_ROW);
        assertEquals(by.toString(), By.xpath("//tr[td='Alice']//button").toString());
    }
}
