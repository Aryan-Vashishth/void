package interactions;

import Elements.EnumClassRegistry;
import Elements.Interfaces.*;
import core.locators.LocatorResolver; // ← centralized AUTO resolver (JSON→.properties)
import core.logging.CustomLogger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import core.utils.WaitUtils;

import java.util.List;

import static Elements.EnumClassRegistry.*;
import static core.utils.EnumResolver.stringToEnum;
import static core.logging.CustomLogger.*;

/**
 * StepDefInteractions provides high-level, reusable methods for step definitions
 * in Cucumber or similar BDD frameworks, using the context-driven element resolution
 * strategy. By leveraging EnumClassRegistry and standard element interfaces,
 * these methods allow for generic, maintainable, and easily-extended UI automation steps.
 */
public class StepDefInteractions extends Interactions {

    public StepDefInteractions(WebDriver driver) {
        super(driver);
        CustomLogger.initialize(this.getClass());
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<T> & ClickableElement & ResolvableEnum> void clickUsingCastedEnum(Class<?> rawClass, String unresolvedEnumName, ActionHandler after) {
        Class<T> typedClass = (Class<T>) rawClass;
        T element = stringToEnum(typedClass, unresolvedEnumName);
        clickOn(element, after);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T> & ResolvableEnum> T resolveByContext(String unresolvedEnumName, String resolvedKey) {
        Class<?> rawClass = CONTEXT_MAP.get(resolvedKey);
        if (rawClass == null) {
            throw new IllegalArgumentException("No enum registered for context: " + resolvedKey);
        }
        if (!rawClass.isEnum() || !ResolvableEnum.class.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException("Mapped class is not a valid enum with ResolvableEnum: " + resolvedKey);
        }
        return stringToEnum((Class<T>) rawClass, unresolvedEnumName);
    }

    // ========================= Click helpers (Navigation, Context) =========================


    public void clickOnFrom(String keySuffix, String unresolvedEnumName, ActionHandler after) {
        clickOnFrom(null, keySuffix, unresolvedEnumName, after);
    }

    public void clickOnFrom(String keyPrefix, String keySuffix, String unresolvedEnumName, ActionHandler after) {
        String resolvedContextKey = EnumClassRegistry.resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        try {
            Class<? extends Enum<?>> rawEnumClass = CONTEXT_MAP.get(resolvedContextKey);

            if (rawEnumClass == null) {
                throw new IllegalArgumentException("Unsupported context: " + resolvedContextKey);
            }
            if (!Enum.class.isAssignableFrom(rawEnumClass)) {
                throw new IllegalArgumentException("Class mapped from context is not an enum: " + rawEnumClass.getSimpleName());
            }
            if (!ClickableElement.class.isAssignableFrom(rawEnumClass) ||
                    !ResolvableEnum.class.isAssignableFrom(rawEnumClass)) {
                throw new IllegalArgumentException("Enum must implement both ClickableElement and ResolvableEnum.");
            }

            clickUsingCastedEnum(rawEnumClass, unresolvedEnumName, after);

        } catch (Exception e) {
            log.error("Failed to click on '" + unresolvedEnumName + "' from context: " + resolvedContextKey + e);
            throw new RuntimeException("Failed to resolve and click on '" + unresolvedEnumName + "' from context: " + resolvedContextKey, e);
        }
    }

    // ========================= Dropdown (Context) =========================

    @Override
    public void selectFromDropdownByContext(String keySuffix, String unresolvedEnumName) {
        selectFromDropdownByContext(null, keySuffix, unresolvedEnumName);
    }

    public void selectFromDropdownByContext(String keyPrefix, String keySuffix, String unresolvedEnumName) {
        String resolvedContextLabel = EnumClassRegistry.resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);

        if (!(resolved instanceof DropdownElement dropdown)) {
            throw new IllegalArgumentException("Enum for context '" + resolvedContextLabel + "' is not a DropdownElement.");
        }
        selectFromDropdown(dropdown);
    }

    public void selectFromDropdownByContext(String keyPrefix, String keySuffix, int dropdownIndex, String unresolvedEnumName) {
        String resolvedContextLabel = EnumClassRegistry.resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);

        // Prefer a specific multi-instance interface if present; otherwise fallback to singleton dropdown.
        if (resolved instanceof MultipleIdenticalDropdownElements multiDropdownOption) {
            selectFromDropdown(dropdownIndex, multiDropdownOption);
            return;
        }
        if (resolved instanceof DropdownElement singleDropdownOption) {
            warn.log("Context '" + resolvedContextLabel + "' resolved to a singleton DropdownElement; index "
                    + dropdownIndex + " will be ignored.");
            selectFromDropdown(singleDropdownOption);
            return;
        }

        throw new IllegalArgumentException(
                "Enum for context '" + resolvedContextLabel + "' must implement MultipleDropdownElement or DropdownElement. " +
                        "Got: " + resolved.getClass().getSimpleName()
        );
    }

    // ========================= Utility: first enum constant =========================

    public static Enum<?> getFirstEnumConstant(Class<?> enumClass, String resolvedContextLabel) {
        if (enumClass == null) {
            throw new IllegalArgumentException("No enum class found for context: " + resolvedContextLabel);
        }
        if (!enumClass.isEnum()) {
            throw new IllegalArgumentException("Context is not an enum: " + resolvedContextLabel);
        }
        Enum<?>[] constants = (Enum<?>[]) enumClass.getEnumConstants();
        if (constants.length == 0) {
            throw new IllegalArgumentException("No enum constants found in: " + enumClass.getSimpleName());
        }
        return constants[0];
    }

    // ========================= Trigger dropdown by context =========================

    public void triggerDropdownByContext(String keyPrefix, String keySuffix, Integer dropdownIndex) {
        String resolvedContextLabel = EnumClassRegistry.resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);

        Class<?> enumClass = EnumClassRegistry.CONTEXT_MAP.get(resolvedContextLabel);
        Enum<?> first = getFirstEnumConstant(enumClass, resolvedContextLabel);

        if (first instanceof MultipleIdenticalDropdownElements multiDropdown) {
            triggerDropdown(multiDropdown, dropdownIndex);
        } else if (first instanceof DropdownElement singleDropdown) {
            triggerDropdown(singleDropdown);
        } else {
            throw new IllegalArgumentException(
                    "Enum does not implement DropdownElement or MultipleDropdownElement: " + enumClass.getSimpleName());
        }
    }

    public void triggerDropdownByContext(String keyPrefix, String keySuffix) {
        triggerDropdownByContext(keyPrefix, keySuffix, null);
    }

    // ========================= Searchable (Context) =========================

    public WebElement getSearchedElementByContext(String keyPrefix, String keySuffix, String unresolvedEnumName, String searchTerm) {
        String resolvedContextKey = EnumClassRegistry.resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);

        try {
            Class<? extends Enum<?>> rawEnumClass = CONTEXT_MAP.get(resolvedContextKey);
            if (rawEnumClass == null) {
                throw new IllegalArgumentException("No enum registered for context: " + resolvedContextKey);
            }
            if (!rawEnumClass.isEnum() || !ResolvableEnum.class.isAssignableFrom(rawEnumClass)) {
                throw new IllegalArgumentException("Mapped class is not a valid enum with ResolvableEnum: " + resolvedContextKey);
            }

            Enum<?> firstEnum = getFirstEnumConstant(rawEnumClass, resolvedContextKey);
            if (!(firstEnum instanceof SearchableElementInput)) {
                throw new IllegalArgumentException("Enum for context '" + resolvedContextKey + "' is not a SearchableElement.");
            }

            ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextKey);
            if (!(resolved instanceof SearchableElementInput searchable)) {
                throw new IllegalArgumentException("Resolved enum does not implement SearchableElement: " + unresolvedEnumName);
            }

            return getSearchedElement(searchable, searchTerm);

        } catch (Exception e) {
            log.error("Failed to search using '" + unresolvedEnumName + "' in context: " + resolvedContextKey, e);
            throw new RuntimeException("Search failed for '" + unresolvedEnumName + "' in context: " + resolvedContextKey, e);
        }
    }

    public void clickSearchableElementByContext(String keyPrefix, String keySuffix, String unresolvedEnumName, String searchTerm){
        WebElement element;
        try {
            element = getSearchedElementByContext(keyPrefix, keySuffix, unresolvedEnumName, searchTerm);
        } catch (Exception e){
            element = getSearchedElementByContext(keyPrefix, keySuffix, unresolvedEnumName, searchTerm);
        }
        clickOn(element);
    }

    // ========================= Visibility verification (Context) =========================

    public boolean verifyElementsAreVisible(String keySuffix, List<String> unresolvedEnumNames) {
        return verifyElementsAreVisible(null, keySuffix, unresolvedEnumNames);
    }

    public boolean verifyElementsAreVisible(String keyPrefix, String keySuffix, List<String> unresolvedEnumNames) {
        boolean allVisible = true;
        String resolvedContextLabel = EnumClassRegistry.resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        info.validation("Checking visibility of elements from context: '" + resolvedContextLabel + "'");

        for (String unresolvedEnumName : unresolvedEnumNames) {
            try {
                ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);

                // Default visibility target = PRIMARY locator of the element.
                // For dropdowns, PRIMARY = trigger (not the overlay list).
                By locator;
                String displayText;

                if (resolved instanceof BaseElement base) {
                    locator = LocatorResolver.primary(base);
                    displayText = base.toString();
                } else {
                    warn.failed("Skipping unsupported element type for: " + unresolvedEnumName);
                    allVisible = false;
                    continue;
                }

                boolean visible = WaitUtils.waitForCondition(
                        "element '" + unresolvedEnumName + "' to be visible",
                        java.time.Duration.ofSeconds(5),
                        java.time.Duration.ofMillis(150),
                        () -> driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed)
                );

                if (!visible) {
                    allVisible = false;
                    error.failed("Element not visible: " + displayText + " | " + locator);
                } else {
                    info.success("Element visible: " + displayText);
                }

            } catch (Exception e) {
                allVisible = false;
                error.failed("Visibility check failed for: " + unresolvedEnumName + " in context: " + resolvedContextLabel);
                error.failed(e.getMessage());
            }
        }

        if (allVisible) {
            info.complete("All listed elements are verified visible for context: " + resolvedContextLabel);
        } else {
            error.failed("Some elements are not visible or failed to verify in context: " + resolvedContextLabel);
        }
        return allVisible;
    }

    // ========================= Checkbox (Context) =========================

    public void setCheckboxByContext(String keyPrefix, String keySuffix, String unresolvedEnumName, boolean check) {
        String resolvedContextLabel = EnumClassRegistry.resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);

        if (!(resolved instanceof CheckboxElement checkbox)) {
            throw new IllegalArgumentException("Enum for context '" + resolvedContextLabel + "' is not a CheckboxElement.");
        } else {
            setCheckbox(checkbox, check);
        }
    }

    public void checkCheckboxByContext(String keyPrefix, String keySuffix, String unresolvedEnumName) {
        setCheckboxByContext(keyPrefix, keySuffix, unresolvedEnumName, true);
    }

    public void uncheckCheckboxByContext(String keyPrefix, String keySuffix, String unresolvedEnumName) {
        setCheckboxByContext(keyPrefix, keySuffix, unresolvedEnumName, false);
    }
}
