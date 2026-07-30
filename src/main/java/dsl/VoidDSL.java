package dsl;

import elements.locator.LocatorDescriptor;
import core.resolvers.locator.api.LocatorRequest;
import core.resolvers.locator.api.LocatorResolvers;

import elements.meta.EnumClassRegistry;
import elements.api.UIElement;
import core.actions.ActionCapability;
import elements.api.capability.*;
import core.utils.ResolvableEnum;
import core.actions.hooks.ActionHandler;
import core.interactions.Interactions;
import core.utils.EnumResolver;

import java.util.List;

import static elements.meta.EnumClassRegistry.*;
import static core.utils.EnumResolver.stringToEnum;
import static core.logging.CustomLogger.*;

/**
 * InteractionsDSL Ã¢â‚¬â€ DSL Layer
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
 *   DSL  Ã¢â€ â€™  Engine  Ã¢â€ â€™  WebDriver
 * </pre>
 * <p>The DSL never touches {@code WebDriver} directly. If the execution
 * engine is later swapped (Playwright, mock, replay), only the engine
 * needs to change Ã¢â‚¬â€ the DSL stays untouched.</p>
 *
 * <h3>Layer responsibilities</h3>
 * <pre>
 *  Ã¢â€Å’Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€Â
 *  Ã¢â€â€š  dsl layer  (this package)                               Ã¢â€â€š
 *  Ã¢â€â€š  - VoidDSL             Ã¢â€ â€™ context-driven DSL              Ã¢â€â€š
 *  Ã¢â€Å“Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€Â¤
 *  Ã¢â€â€š  framework layer   (core / elements)                     Ã¢â€â€š
 *  Ã¢â€â€š  - Interactions         Ã¢â€ â€™ raw UI actions (engine)        Ã¢â€â€š
 *  Ã¢â€â€š  - VOID                 Ã¢â€ â€™ framework faÃƒÂ§ade               Ã¢â€â€š
 *  Ã¢â€â€š  - elements.api.*       Ã¢â€ â€™ element contracts              Ã¢â€â€š
 *  Ã¢â€â€š  - core.*               Ã¢â€ â€™ driver / logging / resolvers   Ã¢â€â€š
 *  Ã¢â€â€Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€Ëœ
 * </pre>
 *
 * @param engine The execution engine Ã¢â‚¬â€ all UI work is delegated here.
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

    // ========================= Selectable (Context) =========================

    @SuppressWarnings("unused")
    public void selectFromDropdownByContext(String keySuffix, String unresolvedEnumName) {
        selectFromDropdownByContext(null, keySuffix, unresolvedEnumName);
    }

    @SuppressWarnings("unused")
    public void selectFromDropdownByContext(String keyPrefix, String keySuffix, String unresolvedEnumName) {
        String resolvedContextLabel = resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);

        if (!(resolved instanceof Selectable Selectable)) {
            throw new IllegalArgumentException("Enum for context '" + resolvedContextLabel + "' is not a Selectable.");
        }
        engine.selectFromDropdown(Selectable);
    }

    @SuppressWarnings("unused")
    public void selectFromDropdownByContext(String keyPrefix, String keySuffix, int dropdownIndex, String unresolvedEnumName) {
        String resolvedContextLabel = resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);

        ActionCapability cap = ((UIElement) resolved).capability();
        if (cap == ActionCapability.MULTI_SELECTABLE) {
            engine.selectFromDropdown(dropdownIndex, (MultiSelectable) resolved);
            return;
        }
        if (cap == ActionCapability.SELECTABLE) {
            warn.log("Context '" + resolvedContextLabel + "' resolved to a singleton Selectable; index "
                    + dropdownIndex + " will be ignored.");
            engine.selectFromDropdown((Selectable) resolved);
            return;
        }

        throw new IllegalArgumentException(
                "Enum for context '" + resolvedContextLabel + "' must implement MultiSelectable or Selectable. " +
                        "Got: " + resolved.getClass().getSimpleName()
        );
    }

    // ========================= Trigger Selectable by context =========================

    public void triggerDropdownByContext(String keyPrefix, String keySuffix, Integer dropdownIndex) {
        String resolvedContextLabel = resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);

        Class<?> enumClass = CONTEXT_MAP.get(resolvedContextLabel);
        Enum<?> first = getFirstEnumConstant(enumClass, resolvedContextLabel);

        ActionCapability cap = ((UIElement) first).capability();
        if (cap == ActionCapability.MULTI_SELECTABLE) {
            engine.triggerDropdown((MultiSelectable) first, dropdownIndex);
        } else if (cap == ActionCapability.SELECTABLE) {
            engine.triggerDropdown((Selectable) first);
        } else {
            throw new IllegalArgumentException(
                    "Enum does not implement MultiSelectable or Selectable: " + enumClass.getSimpleName());
        }
    }

    @SuppressWarnings("unused")
    public void triggerDropdownByContext(String keyPrefix, String keySuffix) {
        triggerDropdownByContext(keyPrefix, keySuffix, null);
    }

    // ========================= Searchable (Context) =========================

    /**
     * Searches for an element by context and returns the result text.
     *
     * @deprecated This method previously returned a WebElement. Now returns the search result text.
     *             Use {@link #clickSearchableElementByContext} for search+click workflows.
     */
    @Deprecated(forRemoval = true)
    public String getSearchedElementByContext(String keyPrefix, String keySuffix, String unresolvedEnumName, String searchTerm) {
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
            if (!(resolved instanceof Searchable Searchable)) {
                throw new IllegalArgumentException("Resolved enum does not implement SearchableElement: " + unresolvedEnumName);
            }

            return engine.getSearchResultText(Searchable, searchTerm);

        } catch (Exception e) {
            error.failed("Failed to search using '" + unresolvedEnumName + "' in context: " + resolvedContextKey);
            throw new RuntimeException("Search failed for '" + unresolvedEnumName + "' in context: " + resolvedContextKey, e);
        }
    }

    @SuppressWarnings("unused")
    public void clickSearchableElementByContext(String keyPrefix, String keySuffix, String unresolvedEnumName, String searchTerm) {
        String resolvedContextKey = resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);

        try {
            ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextKey);
            if (!(resolved instanceof Searchable Searchable)) {
                throw new IllegalArgumentException("Resolved enum does not implement SearchableElement: " + unresolvedEnumName);
            }
            engine.searchAndSelect(Searchable, searchTerm);
        } catch (Exception e) {
            error.failed("Failed to search and click using '" + unresolvedEnumName + "' in context: " + resolvedContextKey);
            throw new RuntimeException("Search+click failed for '" + unresolvedEnumName + "' in context: " + resolvedContextKey, e);
        }
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
                UIElement el = (UIElement) resolved;
                LocatorDescriptor locator = LocatorResolvers.strict().resolveDescriptor(LocatorRequest.of(el.getExternalFileName(), el.getPrimaryLocator(), el.getArgs()));
                String displayText = resolved.getLabel();

                boolean visible = engine.isAnyDisplayed(locator);

                if (!visible) {
                    allVisible = false;
                    error.failed("UIElement not visible: " + displayText + " | " + locator);
                } else {
                    info.success("UIElement visible: " + displayText);
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

    // ========================= Checkable (Context) =========================

    public void setCheckboxByContext(String keyPrefix, String keySuffix, String unresolvedEnumName, boolean check) {
        String resolvedContextLabel = resolveKeyUsingPrefixAndSuffix(keyPrefix, keySuffix);
        ResolvableEnum resolved = resolveByContext(unresolvedEnumName, resolvedContextLabel);

        if (!(resolved instanceof Checkable Checkable)) {
            throw new IllegalArgumentException("Enum for context '" + resolvedContextLabel + "' is not a Checkable.");
        } else {
            engine.setCheckbox(Checkable, check);
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

