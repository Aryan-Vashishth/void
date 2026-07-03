package core.actions;

import core.interactions.hooks.After;
import core.interactions.hooks.Before;
import elements.api.capability.Checkable;
import elements.api.capability.Clickable;
import elements.api.capability.Hoverable;
import elements.api.capability.SearchField;
import elements.api.capability.SearchableDropdown;
import elements.api.capability.Selectable;
import elements.api.capability.Typeable;
import elements.meta.ElementRole;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;

/**
 * Verifies Phase 4 — Capability-Driven Hook Selection.
 *
 * <p>Covers: {@code safeProfile()} identity, hook content, capability resolution
 * through {@link ElementActions#of}, {@link Action#safely()} override wiring,
 * and backward compatibility with {@link Profiles#SAFE}.</p>
 */
public class ElementActionsSafeProfileTest {

    // ── safeProfile identity ──────────────────────────────────────────────

    @Test
    public void clickable_safeProfile_returnsClickableSafeProfileConstant() {
        assertSame(stubClickable().safeProfile(), Clickable.CLICKABLE_SAFE_PROFILE);
    }

    @Test
    public void typeable_safeProfile_returnsTypeableSafeProfileConstant() {
        assertSame(stubTypeable().safeProfile(), Typeable.TYPEABLE_SAFE_PROFILE);
    }

    @Test
    public void selectable_safeProfile_returnsSelectableSafeProfileConstant() {
        assertSame(stubSelectable().safeProfile(), Selectable.SELECTABLE_SAFE_PROFILE);
    }

    @Test
    public void searchField_safeProfile_returnsSearchFieldConstant_notTypeableOrClickable() {
        // Forced diamond override: SearchField extends both Typeable and Clickable.
        assertSame(stubSearchField().safeProfile(), SearchField.SEARCH_FIELD_SAFE_PROFILE);
    }

    @Test
    public void searchableDropdown_safeProfile_returnsSearchableDropdownConstant_notSelectableOrSearchable() {
        // Forced diamond override: SearchableDropdown extends Selectable and Searchable.
        assertSame(stubSearchableDropdown().safeProfile(), SearchableDropdown.SEARCHABLE_DROPDOWN_SAFE_PROFILE);
    }

    @Test
    public void checkable_safeProfile_inheritsClickableSafeProfile() {
        // Checkable extends Clickable without overriding safeProfile — inherits Clickable's constant.
        assertSame(stubCheckable().safeProfile(), Clickable.CLICKABLE_SAFE_PROFILE);
    }

    @Test
    public void hoverable_safeProfile_fallsBackToDefaultSafe() {
        // Hoverable has no safeProfile override — falls back to ActionCapabilityProvider default.
        assertSame(stubHoverable().safeProfile(), ActionProfiles.DEFAULT_SAFE);
    }

    // ── safeProfile hook content ──────────────────────────────────────────

    @Test
    public void clickableSafeProfile_hasWaitForClickableBefore_andAngularLoaderHighlightAfter() {
        ActionProfile p = Clickable.CLICKABLE_SAFE_PROFILE;
        assertEquals(p.before(), List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE));
        assertEquals(p.after(), List.of(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void typeableSafeProfile_hasClearFieldAndWaitVisibleBefore_andHighlightAfter() {
        ActionProfile p = Typeable.TYPEABLE_SAFE_PROFILE;
        assertEquals(p.before(), List.of(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE));
        assertEquals(p.after(), List.of(After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void selectableSafeProfile_hasThreeBeforeHooks_andHighlightAfter() {
        ActionProfile p = Selectable.SELECTABLE_SAFE_PROFILE;
        assertEquals(p.before(), List.of(
                Before.WAIT_FOR_ELEMENT_VISIBLE,
                Before.WAIT_FOR_ELEMENT_CLICKABLE,
                Before.WAIT_FOR_ANGULAR_LOADER));
        assertEquals(p.after(), List.of(After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void defaultSafeProfile_hasWaitForVisibleBefore_andNoAfterHooks() {
        assertEquals(ActionProfiles.DEFAULT_SAFE.before(), List.of(Before.WAIT_FOR_ELEMENT_VISIBLE));
        assertEquals(ActionProfiles.DEFAULT_SAFE.after(), List.of());
    }

    // ── ElementActions.of() capability resolution ─────────────────────────

    @Test
    public void elementActionsOf_clickable_resolvesClickableCapability() {
        Action action = ElementActions.of(stubClickable(), ElementRole.TRIGGER, (e, d) -> {});
        assertEquals(action.capability(), ActionCapability.CLICKABLE);
    }

    @Test
    public void elementActionsOf_typeable_resolvesTypeableCapability() {
        Action action = ElementActions.of(stubTypeable(), ElementRole.INPUT, (e, d) -> {});
        assertEquals(action.capability(), ActionCapability.TYPEABLE);
    }

    @Test
    public void elementActionsOf_checkable_resolvesCheckableCapability() {
        Action action = ElementActions.of(stubCheckable(), ElementRole.TRIGGER, (e, d) -> {});
        assertEquals(action.capability(), ActionCapability.CHECKABLE);
    }

    @Test
    public void elementActionsOf_hoverable_resolvesHoverableCapability_notUnknown() {
        // Before Phase 4, capabilityFor() had no branch for Hoverable and returned UNKNOWN.
        // After Phase 4, Hoverable implements ActionCapabilityProvider — returns HOVERABLE.
        Action action = ElementActions.of(stubHoverable(), ElementRole.TRIGGER, (e, d) -> {});
        assertEquals(action.capability(), ActionCapability.HOVERABLE);
    }

    // ── safely() override wiring ──────────────────────────────────────────

    @Test
    public void clickable_safely_returnsWrappedAction_andPreservesCapability() {
        Action action = ElementActions.of(stubClickable(), ElementRole.TRIGGER, (e, d) -> {});
        Action safe = action.safely();
        // Hooks were applied — safely() must return a HookChainAction, not the raw base.
        assertNotSame(safe, action);
        // HookChainAction delegates capability() to the wrapped ElementBoundAction.
        assertEquals(safe.capability(), ActionCapability.CLICKABLE);
    }

    @Test
    public void typeable_safely_returnsWrappedAction_andPreservesCapability() {
        Action action = ElementActions.of(stubTypeable(), ElementRole.INPUT, (e, d) -> {});
        Action safe = action.safely();
        assertNotSame(safe, action);
        assertEquals(safe.capability(), ActionCapability.TYPEABLE);
    }

    // ── Backward compatibility: Profiles.SAFE still matches safeProfile ───

    @Test
    public void clickable_profilesSafe_hooksMatchClickableSafeProfile() {
        // Profiles.SAFE dispatches on capability() == CLICKABLE.
        // The resulting hooks must be equal to CLICKABLE_SAFE_PROFILE — backward compat for
        // callers using .using(Profiles.SAFE) rather than .safely().
        Clickable element = stubClickable();
        Action action = ElementActions.of(element, ElementRole.TRIGGER, (e, d) -> {});
        assertEquals(Profiles.SAFE.before(action), element.safeProfile().before());
        assertEquals(Profiles.SAFE.after(action), element.safeProfile().after());
    }

    @Test
    public void typeable_profilesSafe_hooksMatchTypeableSafeProfile() {
        Typeable element = stubTypeable();
        Action action = ElementActions.of(element, ElementRole.INPUT, (e, d) -> {});
        assertEquals(Profiles.SAFE.before(action), element.safeProfile().before());
        assertEquals(Profiles.SAFE.after(action), element.safeProfile().after());
    }

    @Test
    public void selectable_profilesSafe_hooksMatchSelectableSafeProfile() {
        Selectable element = stubSelectable();
        Action action = ElementActions.of(element, ElementRole.TRIGGER, (e, d) -> {});
        assertEquals(Profiles.SAFE.before(action), element.safeProfile().before());
        assertEquals(Profiles.SAFE.after(action), element.safeProfile().after());
    }

    @Test
    public void lambdaAction_safely_usesProfilesSafe_capabilityIsUnknown() {
        // A raw lambda (not via ElementActions.of) is not an ElementBoundAction.
        // Action.safely() default calls Profiles.SAFE, not the capability-specific safeProfile.
        // Profiles.SAFE default branch matches DEFAULT_SAFE for UNKNOWN capability.
        Action lambda = engine -> {};
        assertEquals(lambda.capability(), ActionCapability.UNKNOWN);
        assertEquals(Profiles.SAFE.before(lambda), ActionProfiles.DEFAULT_SAFE.before());
    }

    // ── Stubs ─────────────────────────────────────────────────────────────

    private static Clickable stubClickable() {
        return new Clickable() {
            @Override public String getTriggerLocator() { return "click"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static Typeable stubTypeable() {
        return new Typeable() {
            @Override public String getInputLocator() { return "input"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static Selectable stubSelectable() {
        return new Selectable() {
            @Override public String getTriggerLocator() { return "trigger"; }
            @Override public String getListLocator() { return "list"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static Checkable stubCheckable() {
        return new Checkable() {
            @Override public String getTriggerLocator() { return "chk"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static Hoverable stubHoverable() {
        return new Hoverable() {
            @Override public String getTextLocator() { return "text"; }
            @Override public String getToolTipContentLocator() { return "tip"; }
            @Override public String getEndsWith() { return ""; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static SearchField stubSearchField() {
        return new SearchField() {
            @Override public String getSearchInputLocator() { return "si"; }
            @Override public String getSearchButtonLocator() { return "sb"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }

    private static SearchableDropdown stubSearchableDropdown() {
        return new SearchableDropdown() {
            @Override public String getSearchInputLocator() { return "si"; }
            @Override public String getSearchButtonLocator() { return "sb"; }
            @Override public String getTriggerLocator() { return "tr"; }
            @Override public String getSearchResultLocator() { return "sr"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs() { return new Object[0]; }
        };
    }
}
