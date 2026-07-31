package elements.api;

import domain.automation.web.vocabulary.actions.AppendTypeAction;
import domain.automation.web.vocabulary.actions.CheckAction;
import domain.automation.web.vocabulary.actions.ClickAction;
import domain.automation.web.vocabulary.actions.ClearAction;
import domain.automation.web.vocabulary.actions.HoverAction;
import domain.automation.web.vocabulary.actions.OpenAction;
import domain.automation.web.vocabulary.actions.SearchAndSelectAction;
import domain.automation.web.vocabulary.actions.SelectAction;
import domain.automation.web.vocabulary.actions.SelectByTextAction;
import domain.automation.web.vocabulary.actions.SelectByValueAction;
import domain.automation.web.vocabulary.actions.SubmitSearchAction;
import domain.automation.web.vocabulary.actions.ToggleAction;
import domain.automation.web.vocabulary.actions.TypeAction;
import domain.automation.web.vocabulary.actions.TypeAndPressAction;
import domain.automation.web.vocabulary.actions.TypeSearchAction;
import domain.automation.web.vocabulary.actions.UploadAction;
import domain.automation.web.vocabulary.capability.Checkable;
import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.Hoverable;
import domain.automation.web.vocabulary.capability.SearchField;
import domain.automation.web.vocabulary.capability.SearchableDropdown;
import domain.automation.web.vocabulary.capability.Selectable;
import domain.automation.web.vocabulary.capability.Typeable;
import domain.automation.web.vocabulary.capability.Uploadable;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

/**
 * Verifies Phase 15 — capability interfaces return concrete action types (covariant returns).
 *
 * <p>Each test confirms that the method returns the specific action subtype, not just
 * {@code Action}. This ensures that the contract is encoded in the type system and
 * remains verifiable at compile time.</p>
 */
public class CapabilityActionEmissionTest {

    // ── Stubs ─────────────────────────────────────────────────────────────

    private static Clickable stubClickable() {
        return new Clickable() {
            @Override public String getTriggerLocator()  { return "t"; }
            @Override public String getExternalFileName(){ return "s.json"; }
            @Override public Object[] getArgs()          { return new Object[0]; }
        };
    }

    private static Checkable stubCheckable() {
        return new Checkable() {
            @Override public String getTriggerLocator()  { return "t"; }
            @Override public String getExternalFileName(){ return "s.json"; }
            @Override public Object[] getArgs()          { return new Object[0]; }
        };
    }

    private static Hoverable stubHoverable() {
        return new Hoverable() {
            @Override public String getTextLocator()           { return "text"; }
            @Override public String getToolTipContentLocator() { return "tip"; }
            @Override public String getEndsWith()              { return ""; }
            @Override public String getExternalFileName()      { return "s.json"; }
            @Override public Object[] getArgs()                { return new Object[0]; }
        };
    }

    private static Typeable stubTypeable() {
        return new Typeable() {
            @Override public String getInputLocator()    { return "i"; }
            @Override public String getExternalFileName(){ return "s.json"; }
            @Override public Object[] getArgs()          { return new Object[0]; }
        };
    }

    private static Selectable stubSelectable() {
        return new Selectable() {
            @Override public String getTriggerLocator()  { return "t"; }
            @Override public String getListLocator()     { return "l"; }
            @Override public String getExternalFileName(){ return "s.json"; }
            @Override public Object[] getArgs()          { return new Object[0]; }
        };
    }

    private static Uploadable stubUploadable() {
        return new Uploadable() {
            @Override public String getInputLocator()    { return "i"; }
            @Override public String getExternalFileName(){ return "s.json"; }
            @Override public Object[] getArgs()          { return new Object[0]; }
        };
    }

    private static SearchField stubSearchField() {
        return new SearchField() {
            @Override public String getSearchInputLocator()  { return "si"; }
            @Override public String getSearchButtonLocator() { return "sb"; }
            @Override public String getExternalFileName()    { return "s.json"; }
            @Override public Object[] getArgs()              { return new Object[0]; }
        };
    }

    private static SearchableDropdown stubSearchableDropdown() {
        return new SearchableDropdown() {
            @Override public String getSearchInputLocator()  { return "si"; }
            @Override public String getSearchButtonLocator() { return "sb"; }
            @Override public String getTriggerLocator()      { return "t"; }
            @Override public String getSearchResultLocator() { return "sr"; }
            @Override public String getExternalFileName()    { return "s.json"; }
            @Override public Object[] getArgs()              { return new Object[0]; }
        };
    }

    // ── Clickable ─────────────────────────────────────────────────────────

    @Test
    public void click_returnsClickAction() {
        ClickAction action = stubClickable().click();
        assertNotNull(action);
    }

    // ── Checkable ─────────────────────────────────────────────────────────

    @Test
    public void toggle_returnsToggleAction() {
        ToggleAction action = stubCheckable().toggle();
        assertNotNull(action);
    }

    @Test
    public void set_returnsCheckAction() {
        CheckAction action = stubCheckable().set(true);
        assertNotNull(action);
    }

    // ── Hoverable ─────────────────────────────────────────────────────────

    @Test
    public void hover_returnsHoverAction() {
        HoverAction action = stubHoverable().hover();
        assertNotNull(action);
    }

    // ── Typeable ──────────────────────────────────────────────────────────

    @Test
    public void type_returnsTypeAction() {
        TypeAction action = stubTypeable().type("hello");
        assertNotNull(action);
    }

    @Test
    public void clear_returnsClearAction() {
        ClearAction action = stubTypeable().clear();
        assertNotNull(action);
    }

    @Test
    public void append_returnsAppendTypeAction() {
        AppendTypeAction action = stubTypeable().append("more");
        assertNotNull(action);
    }

    @Test
    public void typeAndPress_returnsTypeAndPressAction() {
        TypeAndPressAction action = stubTypeable().typeAndPress("q", "ENTER");
        assertNotNull(action);
    }

    // ── Selectable ────────────────────────────────────────────────────────

    @Test
    public void open_returnsOpenAction() {
        OpenAction action = stubSelectable().open();
        assertNotNull(action);
    }

    @Test
    public void select_returnsSelectAction() {
        SelectAction action = stubSelectable().select();
        assertNotNull(action);
    }

    @Test
    public void selectByText_returnsSelectByTextAction() {
        SelectByTextAction action = stubSelectable().selectByText("Option A");
        assertNotNull(action);
    }

    @Test
    public void selectByValue_returnsSelectByValueAction() {
        SelectByValueAction action = stubSelectable().selectByValue("v1");
        assertNotNull(action);
    }

    // ── Uploadable ────────────────────────────────────────────────────────

    @Test
    public void upload_returnsUploadAction() {
        UploadAction action = stubUploadable().upload("/tmp/file.csv");
        assertNotNull(action);
    }

    // ── SearchField ───────────────────────────────────────────────────────

    @Test
    public void typeSearch_returnsTypeSearchAction() {
        TypeSearchAction action = stubSearchField().typeSearch("invoice");
        assertNotNull(action);
    }

    @Test
    public void submitSearch_returnsSubmitSearchAction() {
        SubmitSearchAction action = stubSearchField().submitSearch();
        assertNotNull(action);
    }

    // ── SearchableDropdown ────────────────────────────────────────────────

    @Test
    public void searchAndSelect_returnsSearchAndSelectAction() {
        SearchAndSelectAction action = stubSearchableDropdown().searchAndSelect("Paris");
        assertNotNull(action);
    }

    // ── Polymorphism: concrete types are accepted where Action is expected ─

    @Test
    public void allEmittedActions_arePolymorphicallyAssignableToAction() {
        core.actions.Action a1 = stubClickable().click();
        core.actions.Action a2 = stubCheckable().toggle();
        core.actions.Action a3 = stubCheckable().set(false);
        core.actions.Action a4 = stubHoverable().hover();
        core.actions.Action a5 = stubTypeable().type("x");
        core.actions.Action a6 = stubTypeable().clear();
        core.actions.Action a7 = stubTypeable().append("x");
        core.actions.Action a8 = stubTypeable().typeAndPress("x", "TAB");
        core.actions.Action a9 = stubSelectable().open();
        core.actions.Action a10 = stubSelectable().select();
        core.actions.Action a11 = stubSelectable().selectByText("o");
        core.actions.Action a12 = stubSelectable().selectByValue("v");
        core.actions.Action a13 = stubUploadable().upload("/f");
        core.actions.Action a14 = stubSearchField().typeSearch("q");
        core.actions.Action a15 = stubSearchField().submitSearch();
        core.actions.Action a16 = stubSearchableDropdown().searchAndSelect("q");

        // Verify non-null — all 16 are live Action instances
        for (core.actions.Action a : new core.actions.Action[]{
                a1, a2, a3, a4, a5, a6, a7, a8,
                a9, a10, a11, a12, a13, a14, a15, a16}) {
            assertNotNull(a);
        }
    }
}
