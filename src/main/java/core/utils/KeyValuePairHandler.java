package core.utils;

import Elements.Interfaces.KeyValuePairElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import core.driver.DriverContext;
import core.locators.PropertiesFileLocatorReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Static utility for extracting and verifying key→value pairs from UI.
 * Uses DriverContext + BaseUtils.wait so no instantiation needed.
 */
public class KeyValuePairHandler extends BaseUtils {

    private KeyValuePairHandler() {
        // prevent instantiation
    }

    /**
     * Resolves the UI value for a given element.
     *
     * @param element KeyValuePairElement enum constant
     * @return trimmed text value
     */
    public static String getValue(KeyValuePairElement element) {
        String label = element.getDisplayText();
        debug.log("Resolving value for key", "Label", label);

        try {
            By by = PropertiesFileLocatorReader.getLocator(
                    element.getPropertyFile(),
                    element.getValue(),
                    element.getArgs()
            );

            WebDriver driver = DriverContext.getDriver();
            WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
            String value = el.getText() == null ? "" : el.getText().trim();

            debug.resolved("Resolved key-value",
                    "Label", label,
                    "Value", value,
                    "By", by
            );
            return value;

        } catch (Exception e) {
            error.log("Failed to resolve value",
                    "Label", label,
                    "PropertyFile", element.getPropertyFile(),
                    "ValueKey", element.getValue(),
                    "Args", java.util.Arrays.toString(element.getArgs()),
                    "Error", e.getMessage()
            );
            throw new RuntimeException("Unable to resolve value for [" + label + "]", e);
        }
    }

    /**
     * Collect all key→value pairs defined by the enum class.
     */
    public static <E extends Enum<E> & KeyValuePairElement> Map<String, String> collectAll(Class<E> enumClass) {
        Map<String, String> map = new HashMap<>();
        for (E element : enumClass.getEnumConstants()) {
            map.put(element.getDisplayText(), getValue(element));
        }
        debug.table(map, "Collected Key→Value");
        return map;
    }

    /**
     * Verify UI values against expected map (case-insensitive equality).
     */
    public static <E extends Enum<E> & KeyValuePairElement> void verifyValues(Class<E> enumClass, Map<String, String> expected) {
        Map<String, String> actual = collectAll(enumClass);

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String key = entry.getKey();
            String expectedValue = entry.getValue();
            String actualValue = actual.get(key);

            if (actualValue == null || !expectedValue.equalsIgnoreCase(actualValue)) {
                error.log("Mismatch detected",
                        "Key", key,
                        "Expected", expectedValue,
                        "Actual", actualValue
                );
                throw new AssertionError("Value mismatch for [" + key + "]");
            } else {
                debug.success("Verified key-value " + "Key: " + key + "Value: " + actualValue);
            }
        }
        debug.complete("All key-values verified OK");
    }

    /**
     * Get values for a list of elements.
     */
    public static List<String> getValues(List<? extends KeyValuePairElement> elements) {
        return elements.stream().map(KeyValuePairHandler::getValue).collect(Collectors.toList());
    }
}
