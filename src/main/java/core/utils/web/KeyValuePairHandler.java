package core.utils.web;

import elements.api.UIElement;
import core.utils.ResolvableEnum;
import core.driver.DriverContext;
import core.driver.Waiter;
import core.engine.selenium.SeleniumEngine;
import core.resolvers.locator.api.LocatorRequest;
import core.resolvers.locator.api.LocatorResolvers;
import elements.locator.LocatorDescriptor;
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
 *
 * <p>Enums passed here must implement <b>both</b> {@link ResolvableEnum} (for label)
 * and an {@link UIElement} sub-interface (for locator resolution).</p>
 */
public class KeyValuePairHandler {
    private KeyValuePairHandler() {
        // prevent instantiation
    }

    /**
     * Resolves the UI value for a given element.
     *
     * @param element enum constant implementing both ResolvableEnum and UIElement
     * @return trimmed text value
     */
    public static <T extends Enum<T> & ResolvableEnum & UIElement> String getValue(T element) {
        String label = element.getLabel();
        try {
            String file = element.getExternalFileName();
            String key = element.getPrimaryLocator();
            LocatorDescriptor d = LocatorResolvers.strict().resolveDescriptor(LocatorRequest.of(file, key, element.getArgs()));
            By by = SeleniumEngine.toBy(d);
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
    public static <E extends Enum<E> & ResolvableEnum & UIElement> Map<String, String> collectAll(Class<E> enumClass) {
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
    public static <E extends Enum<E> & ResolvableEnum & UIElement> void verifyValues(Class<E> enumClass, Map<String, String> expected) {
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
    public static <T extends Enum<T> & ResolvableEnum & UIElement> List<String> getValues(List<T> elements) {
        return elements.stream().map(KeyValuePairHandler::getValue).collect(Collectors.toList());
    }
}
