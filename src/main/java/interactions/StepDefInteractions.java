package interactions;

import elements.meta.EnumClassRegistry;
import elements.api.*;
import interactions.hooks.ActionHandler;
import core.resolvers.locator.LocatorResolverV1;
import core.logging.CustomLogger;
import core.utils.EnumResolver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import core.utils.web.WaitUtils;

import java.util.List;

import static elements.meta.EnumClassRegistry.*;
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
    // Removed generic type parameter usage to avoid unused warning; still enforces Clickable via runtime checks.
    private void clickUsingCastedEnum(Class<?> rawClass, String unresolvedEnumName, ActionHandler after) {
        if (rawClass == null || !rawClass.isEnum()) {
            throw new IllegalArgumentException("Provided class is not an enum: " + rawClass);
        }
        if (!Clickable.class.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException("Enum does not implement Clickable: " + rawClass.getSimpleName());
        }
        Enum<?> resolvedEnum = resolveEnumConstant((Class<? extends Enum<?>>) rawClass, unresolvedEnumName);
        clickOn((Clickable) resolvedEnum, after);
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

    /**
     * Local enum constant resolver: supports raw name (normalized) and label if ResolvableEnum.
     */
    private Enum<?> resolveEnumConstant(Class<? extends Enum<?>> enumClass, String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input enum name cannot be null/blank.");
        }
        String normalized = EnumResolver.normalizeToEnumName(input);
        for (Enum<?> constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(normalized)) return constant;
            if (constant instanceof ResolvableEnum re && input.equalsIgnoreCase(re.getLabel())) return constant;
        }
        throw new IllegalArgumentException("No matching constant or label for '" + input + "' (normalized: " + normalized + ") in enum: " + enumClass.getSimpleName());
    }

    // ========================= Click helpers (Navigation, Context) =========================


    @SuppressWarnings("unused")
    public void clickOnFrom(String keySuffix, String unresolvedEnumName, ActionHandler after) {
        clickOnFrom(null, keySuffix, unresolvedEnumName, after);
    }

    @SuppressWarnings("unused")
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
            if (!Clickable.class.isAssignableFrom(rawEnumClass) ||
                    !ResolvableEnum.class.isAssignableFrom(rawEnumClass)) {
                throw new IllegalArgumentException("Enum must implement both Clickable and ResolvableEnum.");
            }

            clickUsingCastedEnum(rawEnumClass, unresolvedEnumName, after);

        } catch (Exception e) {
            log.error("Failed to click on '" + unresolvedEnumName + "' from context: " + resolvedContextKey + e);
            throw new RuntimeException("Failed to resolve and click on '" + unresolvedEnumName + "' from context: " + resolvedContextKey, e);
        }
    }

    // ========================= Dropdown (Context) =========================

    @SuppressWarnings("unused")
    public void selectFromDropdownByContext(String keySuffix, String unresolvedEnumName) {
        selectFromDropdownByContext(null, keySuffix, unresolvedEnumName);
    }

    @SuppressWarnings("unused")
    public void selectFromDropdownByContext(String keyPrefix, String keySuffix, String unresolvedEnumName) {
        String resolvedContextLabel = EnumClassRegistry.resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);

        if (!(resolved instanceof Dropdown dropdown)) {
            throw new IllegalArgumentException("Enum for context '" + resolvedContextLabel + "' is not a Dropdown.");
        }
        selectFromDropdown(dropdown);
    }

    @SuppressWarnings("unused")
    public void selectFromDropdownByContext(String keyPrefix, String keySuffix, int dropdownIndex, String unresolvedEnumName) {
        String resolvedContextLabel = EnumClassRegistry.resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);

        // Prefer a specific multi-instance interface if present; otherwise fallback to singleton dropdown.
        if (resolved instanceof MultipleIdenticalDropdowns multiDropdownOption) {
            selectFromDropdown(dropdownIndex, multiDropdownOption);
            return;
        }
        if (resolved instanceof Dropdown singleDropdownOption) {
            warn.log("Context '" + resolvedContextLabel + "' resolved to a singleton Dropdown; index "
                    + dropdownIndex + " will be ignored.");
            selectFromDropdown(singleDropdownOption);
            return;
        }

        throw new IllegalArgumentException(
                "Enum for context '" + resolvedContextLabel + "' must implement MultipleDropdown or Dropdown. " +
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

        if (first instanceof MultipleIdenticalDropdowns multiDropdown) {
            triggerDropdown(multiDropdown, dropdownIndex);
        } else if (first instanceof Dropdown singleDropdown) {
            triggerDropdown(singleDropdown);
        } else {
            throw new IllegalArgumentException(
                    "Enum does not implement Dropdown or MultipleDropdown: " + enumClass.getSimpleName());
        }
    }

    @SuppressWarnings("unused")
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
            if (!(firstEnum instanceof Searchable)) {
                throw new IllegalArgumentException("Enum for context '" + resolvedContextKey + "' is not a SearchableElement.");
            }

            ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextKey);
            if (!(resolved instanceof Searchable searchable)) {
                throw new IllegalArgumentException("Resolved enum does not implement SearchableElement: " + unresolvedEnumName);
            }

            return getSearchedElement(searchable, searchTerm);

        } catch (Exception e) {
            log.error("Failed to search using '" + unresolvedEnumName + "' in context: " + resolvedContextKey, e);
            throw new RuntimeException("Search failed for '" + unresolvedEnumName + "' in context: " + resolvedContextKey, e);
        }
    }

    @SuppressWarnings("unused")
    public void clickSearchableElementByContext(String keyPrefix, String keySuffix, String unresolvedEnumName, String searchTerm){
        // Resolve and click in a single pass; let exceptions propagate naturally for clear failure messages.
        WebElement element = getSearchedElementByContext(keyPrefix, keySuffix, unresolvedEnumName, searchTerm);
        clickOn(element);
    }

    // ========================= Visibility verification (Context) =========================

    @SuppressWarnings("unused")
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
                By locator = LocatorResolverV1.getLocator(resolved.getExternalFileName(), resolved.getPrimaryLocator(), resolved.getArgs());
                String displayText = resolved.getLabel();

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

        // Using v1 Checkbox interface (replacing v2 CheckboxElement)
        if (!(resolved instanceof Checkbox checkbox)) {
            throw new IllegalArgumentException("Enum for context '" + resolvedContextLabel + "' is not a Checkbox.");
        } else {
            setCheckbox(checkbox, check);
        }
    }

    @SuppressWarnings("unused")
    public void checkCheckboxByContext(String keyPrefix, String keySuffix, String unresolvedEnumName) {
        setCheckboxByContext(keyPrefix, keySuffix, unresolvedEnumName, true);
    }

    @SuppressWarnings("unused")
    public void uncheckCheckboxByContext(String keyPrefix, String keySuffix, String unresolvedEnumName) {
        setCheckboxByContext(keyPrefix, keySuffix, unresolvedEnumName, false);
    }

    // Local helper to set checkbox state using v1 locator resolution only.
    private void setCheckbox(Checkbox checkbox, boolean desiredState) {
        try {
            By locator = LocatorResolverV1.getLocator(checkbox);
            WebElement cb = driver.findElement(locator);
            // Determine current state (try aria-checked, then checked attribute, then isSelected).
            boolean current;
            try {
                String aria = cb.getAttribute("aria-checked");
                if (aria != null && !aria.isBlank()) {
                    current = aria.equalsIgnoreCase("true");
                } else {
                    String checkedAttr = cb.getAttribute("checked");
                    if (checkedAttr != null) {
                        current = true; // presence implies checked
                    } else {
                        current = cb.isSelected();
                    }
                }
            } catch (Exception ignored) {
                current = cb.isSelected();
            }
            if (current != desiredState) {
                clickOn(cb); // use existing click pipeline
                info.success("Checkbox toggled → " + checkbox.getDisplayText() + " to " + desiredState);
            } else {
                info.validation("Checkbox already in desired state → " + checkbox.getDisplayText() + " = " + desiredState);
            }
        } catch (Exception e) {
            error.failed("Failed to set checkbox: " + checkbox.getDisplayText() + " " + e.getMessage());
            throw new RuntimeException("Failed to set checkbox state for: " + checkbox.getDisplayText(), e);
        }
    }
}
