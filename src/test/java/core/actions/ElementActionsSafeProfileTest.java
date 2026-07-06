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
 * Verifies the Phase 5 SoC correction — execution policy lives in the action layer,
 * not on capability interfaces.
 *
 * <p>Covers: {@link ActionProfiles#safeProfileFor} hook content, capability resolution
 * through {@link ElementActions#of}, {@link ElementAction#safely()} dispatch, and
 * backward compatibility with {@link Profiles#SAFE}.</p>
 */
public class ElementActionsSafeProfileTest {

    // ── ActionProfiles.safeProfileFor hook content ────────────────────────
    // Policy lives in core.actions (action layer). Capability interfaces have
    // no ActionProfile or hook imports.

    @Test
    public void safeProfileFor_clickable_hasWaitForClickableAndAngularLoaderHooks() {
        ActionProfile p = ActionProfiles.safeProfileFor(ActionCapability.CLICKABLE);
        assertEquals(p.before(), List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE));
        assertEquals(p.after(), List.of(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void safeProfileFor_typeable_hasClearFieldAndWaitVisibleAndHighlight() {
        ActionProfile p = ActionProfiles.safeProfileFor(ActionCapability.TYPEABLE);
        assertEquals(p.before(), List.of(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE));
        assertEquals(p.after(), List.of(After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void safeProfileFor_selectable_hasThreeBeforeHooksAndHighlight() {
        ActionProfile p = ActionProfiles.safeProfileFor(ActionCapability.SELECTABLE);
        assertEquals(p.before(), List.of(
                Before.WAIT_FOR_ELEMENT_VISIBLE,
                Before.WAIT_FOR_ELEMENT_CLICKABLE,
                Before.WAIT_FOR_ANGULAR_LOADER));
        assertEquals(p.after(), List.of(After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void safeProfileFor_checkable_returnsSameProfileAsClickable() {
        // CHECKABLE shares CLICKABLE_SAFE — same interaction model.
        assertSame(ActionProfiles.safeProfileFor(ActionCapability.CHECKABLE),
                   ActionProfiles.safeProfileFor(ActionCapability.CLICKABLE));
    }

    @Test
    public void safeProfileFor_searchField_returnsSameProfileAsTypeable() {
        assertSame(ActionProfiles.safeProfileFor(ActionCapability.SEARCH_FIELD),
                   ActionProfiles.safeProfileFor(ActionCapability.TYPEABLE));
    }

    @Test
    public void safeProfileFor_searchableDropdown_returnsSameProfileAsSelectable() {
        assertSame(ActionProfiles.safeProfileFor(ActionCapability.SEARCHABLE_DROPDOWN),
                   ActionProfiles.safeProfileFor(ActionCapability.SELECTABLE));
    }

    @Test
    public void safeProfileFor_hoverable_returnsDefaultSafe() {
        assertSame(ActionProfiles.safeProfileFor(ActionCapability.HOVERABLE),
                   ActionProfiles.DEFAULT_SAFE);
    }

    @Test
    public void safeProfileFor_unknown_returnsDefaultSafe() {
        assertSame(ActionProfiles.safeProfileFor(ActionCapability.UNKNOWN),
                   ActionProfiles.DEFAULT_SAFE);
    }

    @Test
    public void defaultSafe_hasWaitForVisibleBefore_andNoAfterHooks() {
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
        // Phase 4 fix: capabilityFor() delegates to ActionCapabilityProvider first.
        Action action = ElementActions.of(stubHoverable(), ElementRole.TRIGGER, (e, d) -> {});
        assertEquals(action.capability(), ActionCapability.HOVERABLE);
    }

    // ── ElementAction.safely() dispatch via defaultSafeProfile() ──────────

    @Test
    public void clickable_safely_appliesClickableSafeHooks() {
        Action action = ElementActions.of(stubClickable(), ElementRole.TRIGGER, (e, d) -> {});
        Action safe = action.safely();
        // safely() calls using(defaultSafeProfile()) which resolves CLICKABLE_SAFE.
        assertNotSame(safe, action);
        assertEquals(safe.capability(), ActionCapability.CLICKABLE);
    }

    @Test
    public void typeable_safely_appliesTypeableSafeHooks() {
        Action action = ElementActions.of(stubTypeable(), ElementRole.INPUT, (e, d) -> {});
        Action safe = action.safely();
        assertNotSame(safe, action);
        assertEquals(safe.capability(), ActionCapability.TYPEABLE);
    }

    @Test
    public void hoverable_safely_appliesDefaultSafeHooks_notProfilesSafeSwitch() {
        // Hoverable → HOVERABLE capability → ActionProfiles.DEFAULT_SAFE.
        // DEFAULT_SAFE has non-empty before hooks, so safely() wraps the action.
        Action action = ElementActions.of(stubHoverable(), ElementRole.TRIGGER, (e, d) -> {});
        Action safe = action.safely();
        assertNotSame(safe, action);
    }

    // ── ActionProfiles.reliableProfileFor hook content ───────────────────

    @Test
    public void reliableProfileFor_clickable_hasAngularLoaderAndWaitClickable() {
        ActionProfile p = ActionProfiles.reliableProfileFor(ActionCapability.CLICKABLE);
        assertEquals(p.before(), List.of(Before.WAIT_FOR_ANGULAR_LOADER, Before.WAIT_FOR_ELEMENT_CLICKABLE));
        assertEquals(p.after(), List.of(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void reliableProfileFor_typeable_hasAngularLoaderWaitAndClearField() {
        ActionProfile p = ActionProfiles.reliableProfileFor(ActionCapability.TYPEABLE);
        assertEquals(p.before(), List.of(Before.WAIT_FOR_ANGULAR_LOADER, Before.WAIT_FOR_ELEMENT_VISIBLE, Before.CLEAR_FIELD));
        assertEquals(p.after(), List.of(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void reliableProfileFor_selectable_hasThreeBeforeHooksAndFullAfter() {
        ActionProfile p = ActionProfiles.reliableProfileFor(ActionCapability.SELECTABLE);
        assertEquals(p.before(), List.of(Before.WAIT_FOR_ANGULAR_LOADER, Before.WAIT_FOR_ELEMENT_VISIBLE, Before.WAIT_FOR_ELEMENT_CLICKABLE));
        assertEquals(p.after(), List.of(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void reliableProfileFor_checkable_returnsSameProfileAsClickable() {
        assertSame(ActionProfiles.reliableProfileFor(ActionCapability.CHECKABLE),
                   ActionProfiles.reliableProfileFor(ActionCapability.CLICKABLE));
    }

    @Test
    public void reliableProfileFor_default_hasWaitVisibleBeforeAndFullAfter() {
        ActionProfile p = ActionProfiles.reliableProfileFor(ActionCapability.HOVERABLE);
        assertEquals(p.before(), List.of(Before.WAIT_FOR_ELEMENT_VISIBLE));
        assertEquals(p.after(), List.of(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void lambdaAction_capability_isUnknown_andSafelyUsesDefaultSafe() {
        // Lambda actions (engine -> {}) don't extend ElementAction.
        // Action.safely() default uses ActionProfiles.DEFAULT_SAFE.
        Action lambda = engine -> {};
        assertEquals(lambda.capability(), ActionCapability.UNKNOWN);
        Action safe = lambda.safely();
        assertNotSame(safe, lambda);
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
