package core.utils.web;

import elements.api.ResolvableEnum;
import core.driver.DriverContext;
import core.driver.Waiter;
import core.resolvers.locator.LocatorResolverV1;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.error;

/**
 * Static utility for extracting and verifying key→value pairs from UI.
 * Uses {@link DriverContext} and {@link Waiter} — no instantiation needed.
 */
public class KeyValuePairHandler {
    private KeyValuePairHandler() {
        // prevent instantiation
    }

    /**
     * Resolves the UI value for a given element.
     *
     * @param element KeyValuePairElement enum constant
     * @return trimmed text value
     */
    public static String getValue(ResolvableEnum element) {
        String label = element.getLabel();
        try {
            String file = element.getExternalFileName(); // may be null
            String key = element.getPrimaryLocator();
            By by = LocatorResolverV1.getLocator(file, key, element.getArgs());
            WebElement el = Waiter.get().until(ExpectedConditions.visibilityOfElementLocated(by));
            String value = el.getText() == null ? "" : el.getText().trim();
            debug.resolved("Resolved key-value", "Label", label, "Value", value, "By", by);
            return value;
        } catch (Exception e) {
            error.log("Failed to resolve value", "Label", label, "Error", e.getMessage());
            throw new RuntimeException("Unable to resolve value for [" + label + "]", e);
        }
    }

    /**
     * Collect all key→value pairs defined by the enum class.
     */
    public static <E extends Enum<E> & ResolvableEnum> Map<String, String> collectAll(Class<E> enumClass) {
        Map<String, String> map = new HashMap<>();
        for (E element : enumClass.getEnumConstants()) {
            map.put(element.getLabel(), getValue(element));
        }
        debug.table(map, "Collected Key→Value");
        return map;
    }

    /**
     * Verify UI values against expected map (case-insensitive equality).
     */
    public static <E extends Enum<E> & ResolvableEnum> void verifyValues(Class<E> enumClass, Map<String, String> expected) {
        Map<String, String> actual = collectAll(enumClass);
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String key = entry.getKey();
            String expectedValue = entry.getValue();
            String actualValue = actual.get(key);
            if (actualValue == null || !expectedValue.equalsIgnoreCase(actualValue)) {
                error.log("Mismatch", "Key", key, "Expected", expectedValue, "Actual", actualValue);
                throw new AssertionError("Value mismatch for [" + key + "]");
            } else {
                debug.success("Verified " + key + " = " + actualValue);
            }
        }
        debug.complete("All key-values verified OK");
    }

    /**
     * Get values for a list of elements.
     */
    public static List<String> getValues(List<? extends ResolvableEnum> elements) {
        return elements.stream().map(KeyValuePairHandler::getValue).collect(Collectors.toList());
    }
}
