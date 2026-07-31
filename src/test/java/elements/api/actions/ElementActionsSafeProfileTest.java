package domain.automation.web.vocabulary.actions;

import core.actions.Action;
import core.actions.ActionCapability;
import core.actions.ActionProfile;
import core.actions.ActionProfiles;
import core.interactions.hooks.After;
import core.interactions.hooks.Before;
import domain.automation.web.vocabulary.capability.Checkable;
import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.Hoverable;
import domain.automation.web.vocabulary.capability.SearchField;
import domain.automation.web.vocabulary.capability.SearchableDropdown;
import domain.automation.web.vocabulary.capability.Selectable;
import domain.automation.web.vocabulary.capability.Typeable;
import domain.automation.web.vocabulary.actions.*;
import domain.automation.web.vocabulary.role.ElementRole;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;

/**
 * Verifies that each concrete ElementAction subclass returns the correct
 * safe and reliable profiles from {@link ElementAction#defaultSafeProfile()}
 * and {@link ElementAction#defaultReliableProfile()}.
 *
 * <p>Covers: profile hook content per concrete action, capability resolution
 * through {@link ElementActions#of}, {@link ElementAction#safely()} dispatch.</p>
 */
public class ElementActionsSafeProfileTest {

    // ── Profile hook content — verified through concrete action subclasses ─
    // Profile ownership lives in each concrete action class (OCP). Adding a
    // new action type does not require changing ActionProfiles or ElementAction.

    @Test
    public void clickAction_defaultSafeProfile_hasWaitForClickableAndAngularLoaderHooks() {
        ActionProfile p = new ClickAction(stubClickable()).defaultSafeProfile();
        assertEquals(p.before(), List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE));
        assertEquals(p.after(), List.of(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void typeAction_defaultSafeProfile_hasClearFieldAndWaitVisibleAndHighlight() {
        ActionProfile p = new TypeAction(stubTypeable(), "").defaultSafeProfile();
        assertEquals(p.before(), List.of(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE));
        assertEquals(p.after(), List.of(After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void selectAction_defaultSafeProfile_hasThreeBeforeHooksAndHighlight() {
        ActionProfile p = new SelectAction(stubSelectable()).defaultSafeProfile();
        assertEquals(p.before(), List.of(
                Before.WAIT_FOR_ELEMENT_VISIBLE,
                Before.WAIT_FOR_ELEMENT_CLICKABLE,
                Before.WAIT_FOR_ANGULAR_LOADER));
        assertEquals(p.after(), List.of(After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void toggleAction_defaultSafeProfile_sharesSameConstantAsClickAction() {
        // CHECKABLE shares CLICKABLE_SAFE — same interaction model.
        assertSame(new ToggleAction(stubCheckable()).defaultSafeProfile(),
                   new ClickAction(stubClickable()).defaultSafeProfile());
    }

    @Test
    public void typeSearchAction_defaultSafeProfile_sharesSameConstantAsTypeAction() {
        assertSame(new TypeSearchAction(stubSearchField(), "").defaultSafeProfile(),
                   new TypeAction(stubTypeable(), "").defaultSafeProfile());
    }

    @Test
    public void searchAndSelectAction_defaultSafeProfile_sharesSameConstantAsSelectAction() {
        assertSame(new SearchAndSelectAction(stubSearchableDropdown(), "").defaultSafeProfile(),
                   new SelectAction(stubSelectable()).defaultSafeProfile());
    }

    @Test
    public void hoverAction_defaultSafeProfile_isDefaultSafe() {
        // HoverAction has no override — inherits DEFAULT_SAFE from ElementAction base.
        assertSame(new HoverAction(stubHoverable()).defaultSafeProfile(),
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

    // ── Reliable profile hook content ─────────────────────────────────────

    @Test
    public void clickAction_defaultReliableProfile_hasAngularLoaderAndWaitClickable() {
        ActionProfile p = new ClickAction(stubClickable()).defaultReliableProfile();
        assertEquals(p.before(), List.of(Before.WAIT_FOR_ANGULAR_LOADER, Before.WAIT_FOR_ELEMENT_CLICKABLE));
        assertEquals(p.after(), List.of(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void typeAction_defaultReliableProfile_hasAngularLoaderWaitAndClearField() {
        ActionProfile p = new TypeAction(stubTypeable(), "").defaultReliableProfile();
        assertEquals(p.before(), List.of(Before.WAIT_FOR_ANGULAR_LOADER, Before.WAIT_FOR_ELEMENT_VISIBLE, Before.CLEAR_FIELD));
        assertEquals(p.after(), List.of(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void selectAction_defaultReliableProfile_hasThreeBeforeHooksAndFullAfter() {
        ActionProfile p = new SelectAction(stubSelectable()).defaultReliableProfile();
        assertEquals(p.before(), List.of(Before.WAIT_FOR_ANGULAR_LOADER, Before.WAIT_FOR_ELEMENT_VISIBLE, Before.WAIT_FOR_ELEMENT_CLICKABLE));
        assertEquals(p.after(), List.of(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void toggleAction_defaultReliableProfile_sharesSameConstantAsClickAction() {
        assertSame(new ToggleAction(stubCheckable()).defaultReliableProfile(),
                   new ClickAction(stubClickable()).defaultReliableProfile());
    }

    @Test
    public void hoverAction_defaultReliableProfile_isDefaultReliable() {
        // HoverAction has no override — inherits DEFAULT_RELIABLE from ElementAction base.
        ActionProfile p = new HoverAction(stubHoverable()).defaultReliableProfile();
        assertEquals(p.before(), List.of(Before.WAIT_FOR_ELEMENT_VISIBLE));
        assertEquals(p.after(), List.of(After.WAIT_FOR_ANGULAR_LOADER, After.WAIT_FOR_SPIN_SPINNER_LOADER, After.HIGHLIGHT_ELEMENT));
    }

    @Test(expectedExceptions = IllegalStateException.class,
          description = "lambda actions with UNKNOWN capability must not silently inherit browser-wait hooks (I3.2)")
    public void lambdaAction_UNKNOWN_safely_throwsIllegalState() {
        // Lambda actions (engine -> {}) don't extend ElementAction and carry UNKNOWN capability.
        // After I3.2: safely() refuses to select hooks for an unrecognised capability.
        // Remedy: declare a specific ActionCapability or call .raw() instead.
        Action lambda = engine -> {};
        assertEquals(lambda.capability(), ActionCapability.UNKNOWN);
        lambda.safely(); // must throw
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
