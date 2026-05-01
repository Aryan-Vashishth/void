package dsl;

import core.resolvers.locator.api.LocatorRequest;
import core.resolvers.locator.api.LocatorResolvers;

import elements.meta.EnumClassRegistry;
import elements.api.*;
import core.utils.ResolvableEnum;
import core.interactions.hooks.ActionHandler;
import core.interactions.Interactions;
import core.utils.EnumResolver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static elements.meta.EnumClassRegistry.*;
import static core.utils.EnumResolver.stringToEnum;
import static core.logging.CustomLogger.*;

/**
 * InteractionsDSL — DSL Layer
 * ----------------------------------------
 * Provides a high-level, context-driven <b>Domain-Specific Language</b> for
 * Cucumber / BDD step definitions. Translates plain-text BDD parameters
 * (e.g.&nbsp;{@code "tiles"}, {@code "admin_home"}) into strongly-typed
 * element enums via {@link EnumClassRegistry}, then <b>delegates</b> all
 * actual UI execution to a composed {@link Interactions} engine.
 *
 * <h3>Composition over inheritance</h3>
 * <p>Unlike the previous {@code StepDefInteractions} which extended
 * {@code Interactions}, this class <em>holds</em> an {@code Interactions}
 * reference. This enforces a clean separation:</p>
 * <pre>
 *   DSL  →  Engine  →  WebDriver
 * </pre>
 * <p>The DSL never touches {@code WebDriver} directly. If the execution
 * engine is later swapped (Playwright, mock, replay), only the engine
 * needs to change — the DSL stays untouched.</p>
 *
 * <h3>Layer responsibilities</h3>
 * <pre>
 *  ┌──────────────────────────────────────────────────────────┐
 *  │  dsl layer  (this package)                               │
 *  │  - VoidDSL             → context-driven DSL              │
 *  ├──────────────────────────────────────────────────────────┤
 *  │  framework layer   (core / elements)                     │
 *  │  - Interactions         → raw UI actions (engine)        │
 *  │  - VOID                 → framework façade               │
 *  │  - elements.api.*       → element contracts              │
 *  │  - core.*               → driver / logging / resolvers   │
 *  └──────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @param engine The execution engine — all UI work is delegated here.
 */
public record VoidDSL(Interactions engine) {

    /**
     * Creates a DSL layer backed by the given interaction engine.
     *
     * @param engine the {@link Interactions} instance that performs actual UI actions
     */
    public VoidDSL(Interactions engine) {
        this.engine = engine;
        initialize(this.getClass());
    }

    /**
     * Exposes the underlying engine for callers that need raw interaction
     * capabilities not wrapped by the DSL (escape hatch).
     *
     * @return the composed {@link Interactions} engine
     */
    @Override
    public Interactions engine() {
        return engine;
    }

    // ========================= Context Resolution =========================

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

    // ========================= Click helpers (Navigation, Context) =========================

    @SuppressWarnings("unchecked")
    private void clickUsingCastedEnum(Class<?> rawClass, String unresolvedEnumName, ActionHandler after) {
        if (rawClass == null || !rawClass.isEnum()) {
            throw new IllegalArgumentException("Provided class is not an enum: " + rawClass);
        }
        if (!Clickable.class.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException("Enum does not implement Clickable: " + rawClass.getSimpleName());
        }
        Enum<?> resolvedEnum = resolveEnumConstant((Class<? extends Enum<?>>) rawClass, unresolvedEnumName);
        engine.clickOn((Clickable) resolvedEnum, after);
    }

    @SuppressWarnings("unused")
    public void clickOnFrom(String keySuffix, String unresolvedEnumName, ActionHandler after) {
        clickOnFrom(null, keySuffix, unresolvedEnumName, after);
    }

    @SuppressWarnings("unused")
    public void clickOnFrom(String keyPrefix, String keySuffix, String unresolvedEnumName, ActionHandler after) {
        String resolvedContextKey = resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
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
            error.failed("Failed to click on '" + unresolvedEnumName + "' from context: " + resolvedContextKey + e);
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
        String resolvedContextLabel = resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);

        if (!(resolved instanceof Dropdown dropdown)) {
            throw new IllegalArgumentException("Enum for context '" + resolvedContextLabel + "' is not a Dropdown.");
        }
        engine.selectFromDropdown(dropdown);
    }

    @SuppressWarnings("unused")
    public void selectFromDropdownByContext(String keyPrefix, String keySuffix, int dropdownIndex, String unresolvedEnumName) {
        String resolvedContextLabel = resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);

        if (resolved instanceof MultipleIdenticalDropdowns multiDropdownOption) {
            engine.selectFromDropdown(dropdownIndex, multiDropdownOption);
            return;
        }
        if (resolved instanceof Dropdown singleDropdownOption) {
            warn.log("Context '" + resolvedContextLabel + "' resolved to a singleton Dropdown; index "
                    + dropdownIndex + " will be ignored.");
            engine.selectFromDropdown(singleDropdownOption);
            return;
        }

        throw new IllegalArgumentException(
                "Enum for context '" + resolvedContextLabel + "' must implement MultipleDropdown or Dropdown. " +
                        "Got: " + resolved.getClass().getSimpleName()
        );
    }

    // ========================= Trigger dropdown by context =========================

    public void triggerDropdownByContext(String keyPrefix, String keySuffix, Integer dropdownIndex) {
        String resolvedContextLabel = resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);

        Class<?> enumClass = CONTEXT_MAP.get(resolvedContextLabel);
        Enum<?> first = getFirstEnumConstant(enumClass, resolvedContextLabel);

        if (first instanceof MultipleIdenticalDropdowns multiDropdown) {
            engine.triggerDropdown(multiDropdown, dropdownIndex);
        } else if (first instanceof Dropdown singleDropdown) {
            engine.triggerDropdown(singleDropdown);
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
        String resolvedContextKey = resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);

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

            return engine.getSearchedElement(searchable, searchTerm);

        } catch (Exception e) {
            error.failed("Failed to search using '" + unresolvedEnumName + "' in context: " + resolvedContextKey);
            throw new RuntimeException("Search failed for '" + unresolvedEnumName + "' in context: " + resolvedContextKey, e);
        }
    }

    @SuppressWarnings("unused")
    public void clickSearchableElementByContext(String keyPrefix, String keySuffix, String unresolvedEnumName, String searchTerm) {
        WebElement element = getSearchedElementByContext(keyPrefix, keySuffix, unresolvedEnumName, searchTerm);
        engine.clickOn(element);
    }

    // ========================= Visibility verification (Context) =========================

    @SuppressWarnings("unused")
    public boolean verifyElementsAreVisible(String keySuffix, List<String> unresolvedEnumNames) {
        return verifyElementsAreVisible(null, keySuffix, unresolvedEnumNames);
    }

    public boolean verifyElementsAreVisible(String keyPrefix, String keySuffix, List<String> unresolvedEnumNames) {
        boolean allVisible = true;
        String resolvedContextLabel = resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        info.validation("Checking visibility of elements from context: '" + resolvedContextLabel + "'");

        for (String unresolvedEnumName : unresolvedEnumNames) {
            try {
                ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);
                Element el = (Element) resolved;
                By locator = LocatorResolvers.strict().resolve(LocatorRequest.of(el.getExternalFileName(), el.getPrimaryLocator(), el.getArgs()));
                String displayText = resolved.getLabel();

                boolean visible = engine.isAnyDisplayed(locator);

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
        String resolvedContextLabel = resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);

        if (!(resolved instanceof Checkbox checkbox)) {
            throw new IllegalArgumentException("Enum for context '" + resolvedContextLabel + "' is not a Checkbox.");
        } else {
            engine.setCheckbox(checkbox, check);
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
}

