package core.bridge.selenium;

import core.engine.LocatorDescriptor;
import core.engine.LocatorStrategy;
import core.logging.CustomLogger;
import org.openqa.selenium.By;

/**
 * Temporary compatibility bridge between Selenium {@link By} locators and
 * the engine-neutral {@link LocatorDescriptor} model.
 *
 * <p>Exists solely to support the deprecated {@code By}-parameter methods in
 * {@link core.interactions.Interactions}. Delete together with those methods.</p>
 *
 * @deprecated Selenium-specific. No new call sites.
 */
@Deprecated(forRemoval = true)
public final class SeleniumLocatorBridge {

    private SeleniumLocatorBridge() {}

    /**
     * Converts a Selenium {@link By} into a {@link LocatorDescriptor}.
     *
     * <p>Recognises four prefixes from {@code By.toString()}: {@code By.xpath:},
     * {@code By.cssSelector:}, {@code By.id:}, {@code By.name:}. Any unrecognised
     * prefix falls back to {@link LocatorStrategy#XPATH} and emits a warning.</p>
     *
     * @param by Selenium By locator
     * @return equivalent LocatorDescriptor
     * @deprecated Use element-based or string-based locator resolution instead.
     */
    @Deprecated(forRemoval = true)
    public static LocatorDescriptor fromBy(By by) {
        String byString = by.toString();
        if (byString.startsWith("By.xpath:")) {
            return LocatorDescriptor.of(byString.substring("By.xpath: ".length()), LocatorStrategy.XPATH);
        } else if (byString.startsWith("By.cssSelector:")) {
            return LocatorDescriptor.of(byString.substring("By.cssSelector: ".length()), LocatorStrategy.CSS);
        } else if (byString.startsWith("By.id:")) {
            return LocatorDescriptor.of(byString.substring("By.id: ".length()), LocatorStrategy.ID);
        } else if (byString.startsWith("By.name:")) {
            return LocatorDescriptor.of(byString.substring("By.name: ".length()), LocatorStrategy.NAME);
        }
        CustomLogger.warn.log("[SeleniumLocatorBridge] Unrecognised By prefix: '"
                + byString + "' -- falling back to XPATH. Migrate this call site.");
        return LocatorDescriptor.of(byString, LocatorStrategy.XPATH);
    }
}
