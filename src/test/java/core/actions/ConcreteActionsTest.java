package core.actions;

import core.engine.EngineConfig;
import core.engine.LocatorDescriptor;
import core.engine.LocatorStrategy;
import core.engine.UIEngine;
import elements.api.Element;
import elements.api.capability.Checkable;
import elements.api.capability.Clickable;
import elements.api.capability.Hoverable;
import elements.api.capability.ReadOnly;
import elements.api.capability.SearchField;
import core.actions.ReadTextAction;
import elements.api.capability.SearchableDropdown;
import elements.api.capability.Selectable;
import elements.api.capability.Typeable;
import elements.api.capability.Uploadable;
import elements.meta.ElementRole;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Verifies Phase 14 — concrete action subclasses.
 *
 * <p>Covers: capability, immutability (safely() returns a new instance), execute()
 * calling the correct UIEngine method, parameter forwarding, and call sequence for
 * composite actions.</p>
 */
public class ConcreteActionsTest {

    // ════════════════════════════════════════════════════════════════════
    // Shared engine stub
    // ════════════════════════════════════════════════════════════════════

    static class RecordingEngine implements UIEngine {
        final List<String> ops = new ArrayList<>();
        boolean checkboxState = false;
        String lastTypedText;
        String lastAppendedText;
        String lastSentKey;
        String lastSelectedText;
        String lastSelectedValue;
        String lastUploadPath;

        @Override public void click(LocatorDescriptor d)                                { ops.add("click"); }
        @Override public void hover(LocatorDescriptor d)                                { ops.add("hover"); }
        @Override public void type(LocatorDescriptor d, String t)                       { ops.add("type");        lastTypedText    = t; }
        @Override public void clear(LocatorDescriptor d)                                { ops.add("clear"); }
        @Override public void appendType(LocatorDescriptor d, String t)                 { ops.add("appendType");  lastAppendedText = t; }
        @Override public void sendKey(LocatorDescriptor d, String k)                    { ops.add("sendKey");     lastSentKey      = k; }
        @Override public void selectByVisibleText(LocatorDescriptor d, String t)        { ops.add("selectByText"); lastSelectedText = t; }
        @Override public void selectByValue(LocatorDescriptor d, String v)              { ops.add("selectByValue"); lastSelectedValue = v; }
        @Override public void uploadFile(LocatorDescriptor d, String p)                 { ops.add("uploadFile");  lastUploadPath   = p; }
        @Override public void waitForOverlay(Duration t)                                { ops.add("waitForOverlay"); }
        @Override public void waitForVisible(LocatorDescriptor d, Duration t)           { ops.add("waitForVisible"); }
        @Override public boolean getCheckboxState(LocatorDescriptor d)                  { return checkboxState; }
        @Override public String getText(LocatorDescriptor d)                            { ops.add("getText"); return ""; }

        @Override public LocatorDescriptor resolve(Element e, ElementRole r, Object... a) { return stub(); }
        @Override public LocatorDescriptor resolve(String f, String k, Object... a)       { return stub(); }
        private static LocatorDescriptor stub() { return new LocatorDescriptor("//stub", LocatorStrategy.XPATH); }

        @Override public void initialize(EngineConfig c) {}
        @Override public void shutdown() {}
        @Override public void navigateTo(String u) {}
        @Override public String getCurrentUrl()                                         { return ""; }
        @Override public String getTitle()                                              { return ""; }
        @Override public void refresh() {}
        @Override public String getAttribute(LocatorDescriptor d, String a)             { return null; }
        @Override public boolean isVisible(LocatorDescriptor d)                         { return true; }
        @Override public boolean isEnabled(LocatorDescriptor d)                         { return true; }
        @Override public boolean isSelected(LocatorDescriptor d)                        { return false; }
        @Override public int getElementCount(LocatorDescriptor d)                       { return 1; }
        @Override public String getTextWithAttributeFallback(LocatorDescriptor d, String e, String... a) { return ""; }
        @Override public void waitForClickable(LocatorDescriptor d, Duration t) {}
        @Override public void waitForAbsence(LocatorDescriptor d, Duration t) {}
        @Override public void waitForPresence(LocatorDescriptor d, Duration t) {}
        @Override public Object executeScript(String s, Object... a)                    { return null; }
        @Override public void scrollTo(LocatorDescriptor d) {}
        @Override public byte[] takeScreenshot()                                        { return new byte[0]; }
        @Override public void highlight(LocatorDescriptor d, String c) {}
        @Override public Object getNativeDriver()                                        { return null; }
        @Override public String getEngineName()                                         { return "test"; }
    }

    // ════════════════════════════════════════════════════════════════════
    // Capability stubs
    // ════════════════════════════════════════════════════════════════════

    private static Clickable stubClickable() {
        return new Clickable() {
            @Override public String getTriggerLocator()  { return "trigger"; }
            @Override public String getExternalFileName(){ return "stub.json"; }
            @Override public Object[] getArgs()          { return new Object[0]; }
        };
    }

    private static Checkable stubCheckable() {
        return new Checkable() {
            @Override public String getTriggerLocator()  { return "trigger"; }
            @Override public String getExternalFileName(){ return "stub.json"; }
            @Override public Object[] getArgs()          { return new Object[0]; }
        };
    }

    private static Hoverable stubHoverable() {
        return new Hoverable() {
            @Override public String getTextLocator()           { return "text"; }
            @Override public String getToolTipContentLocator() { return "tip"; }
            @Override public String getEndsWith()              { return ""; }
            @Override public String getExternalFileName()      { return "stub.json"; }
            @Override public Object[] getArgs()                { return new Object[0]; }
        };
    }

    private static Typeable stubTypeable() {
        return new Typeable() {
            @Override public String getInputLocator()    { return "input"; }
            @Override public String getExternalFileName(){ return "stub.json"; }
            @Override public Object[] getArgs()          { return new Object[0]; }
        };
    }

    private static Selectable stubSelectable() {
        return new Selectable() {
            @Override public String getTriggerLocator()  { return "trigger"; }
            @Override public String getListLocator()     { return "list"; }
            @Override public String getExternalFileName(){ return "stub.json"; }
            @Override public Object[] getArgs()          { return new Object[0]; }
        };
    }

    private static Uploadable stubUploadable() {
        return new Uploadable() {
            @Override public String getInputLocator()    { return "input"; }
            @Override public String getExternalFileName(){ return "stub.json"; }
            @Override public Object[] getArgs()          { return new Object[0]; }
        };
    }

    private static SearchField stubSearchField() {
        return new SearchField() {
            @Override public String getSearchInputLocator()  { return "si"; }
            @Override public String getSearchButtonLocator() { return "sb"; }
            @Override public String getExternalFileName()    { return "stub.json"; }
            @Override public Object[] getArgs()              { return new Object[0]; }
        };
    }

    private static SearchableDropdown stubSearchableDropdown() {
        return new SearchableDropdown() {
            @Override public String getSearchInputLocator()  { return "si"; }
            @Override public String getSearchButtonLocator() { return "sb"; }
            @Override public String getTriggerLocator()      { return "trigger"; }
            @Override public String getSearchResultLocator() { return "sr"; }
            @Override public String getExternalFileName()    { return "stub.json"; }
            @Override public Object[] getArgs()              { return new Object[0]; }
        };
    }

    private static ReadOnly stubReadOnly() {
        return new ReadOnly() {
            @Override public String getTextLocator()      { return "//span[@class='label']"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    // ════════════════════════════════════════════════════════════════════
    // ClickAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void clickAction_capability_isClickable() {
        assertEquals(new ClickAction(stubClickable()).capability(), ActionCapability.CLICKABLE);
    }

    @Test
    public void clickAction_safely_returnsNewInstance() {
        ClickAction action = new ClickAction(stubClickable());
        assertNotSame(action.safely(), action);
    }

    @Test
    public void clickAction_perform_callsEngineClick() {
        RecordingEngine engine = new RecordingEngine();
        new ClickAction(stubClickable()).perform(engine);
        assertEquals(engine.ops, List.of("click"));
    }

    // ════════════════════════════════════════════════════════════════════
    // ToggleAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void toggleAction_capability_isCheckable() {
        assertEquals(new ToggleAction(stubCheckable()).capability(), ActionCapability.CHECKABLE);
    }

    @Test
    public void toggleAction_perform_callsEngineClick() {
        RecordingEngine engine = new RecordingEngine();
        new ToggleAction(stubCheckable()).perform(engine);
        assertEquals(engine.ops, List.of("click"));
    }

    // ════════════════════════════════════════════════════════════════════
    // CheckAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void checkAction_capability_isCheckable() {
        assertEquals(new CheckAction(stubCheckable(), true).capability(), ActionCapability.CHECKABLE);
    }

    @Test
    public void checkAction_perform_clicksWhenStatesDiffer() {
        RecordingEngine engine = new RecordingEngine();
        engine.checkboxState = false;
        new CheckAction(stubCheckable(), true).perform(engine);
        assertEquals(engine.ops, List.of("click"));
    }

    @Test
    public void checkAction_perform_skipsClickWhenStatesMatch() {
        RecordingEngine engine = new RecordingEngine();
        engine.checkboxState = false;
        new CheckAction(stubCheckable(), false).perform(engine);
        assertFalse(engine.ops.contains("click"), "should not click when state already matches");
    }

    // ════════════════════════════════════════════════════════════════════
    // HoverAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void hoverAction_capability_isHoverable() {
        assertEquals(new HoverAction(stubHoverable()).capability(), ActionCapability.HOVERABLE);
    }

    @Test
    public void hoverAction_perform_callsEngineHover() {
        RecordingEngine engine = new RecordingEngine();
        new HoverAction(stubHoverable()).perform(engine);
        assertEquals(engine.ops, List.of("hover"));
    }

    // ════════════════════════════════════════════════════════════════════
    // TypeAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void typeAction_capability_isTypeable() {
        assertEquals(new TypeAction(stubTypeable(), "x").capability(), ActionCapability.TYPEABLE);
    }

    @Test
    public void typeAction_perform_callsEngineType_withText() {
        RecordingEngine engine = new RecordingEngine();
        new TypeAction(stubTypeable(), "hello").perform(engine);
        assertEquals(engine.ops, List.of("type"));
        assertEquals(engine.lastTypedText, "hello");
    }

    // ════════════════════════════════════════════════════════════════════
    // ClearAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void clearAction_capability_isTypeable() {
        assertEquals(new ClearAction(stubTypeable()).capability(), ActionCapability.TYPEABLE);
    }

    @Test
    public void clearAction_perform_callsEngineClear() {
        RecordingEngine engine = new RecordingEngine();
        new ClearAction(stubTypeable()).perform(engine);
        assertEquals(engine.ops, List.of("clear"));
    }

    // ════════════════════════════════════════════════════════════════════
    // AppendTypeAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void appendTypeAction_capability_isTypeable() {
        assertEquals(new AppendTypeAction(stubTypeable(), "x").capability(), ActionCapability.TYPEABLE);
    }

    @Test
    public void appendTypeAction_perform_callsEngineAppendType_withText() {
        RecordingEngine engine = new RecordingEngine();
        new AppendTypeAction(stubTypeable(), "world").perform(engine);
        assertEquals(engine.ops, List.of("appendType"));
        assertEquals(engine.lastAppendedText, "world");
    }

    // ════════════════════════════════════════════════════════════════════
    // TypeAndPressAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void typeAndPressAction_capability_isTypeable() {
        assertEquals(new TypeAndPressAction(stubTypeable(), "x", "ENTER").capability(), ActionCapability.TYPEABLE);
    }

    @Test
    public void typeAndPressAction_perform_callsTypeAndSendKey_inOrder() {
        RecordingEngine engine = new RecordingEngine();
        new TypeAndPressAction(stubTypeable(), "query", "TAB").perform(engine);
        assertEquals(engine.ops, List.of("type", "sendKey"));
        assertEquals(engine.lastTypedText, "query");
        assertEquals(engine.lastSentKey, "TAB");
    }

    // ════════════════════════════════════════════════════════════════════
    // OpenAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void openAction_capability_isSelectable() {
        assertEquals(new OpenAction(stubSelectable()).capability(), ActionCapability.SELECTABLE);
    }

    @Test
    public void openAction_perform_callsEngineClick() {
        RecordingEngine engine = new RecordingEngine();
        new OpenAction(stubSelectable()).perform(engine);
        assertEquals(engine.ops, List.of("click"));
    }

    // ════════════════════════════════════════════════════════════════════
    // SelectAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void selectAction_capability_isSelectable() {
        assertEquals(new SelectAction(stubSelectable()).capability(), ActionCapability.SELECTABLE);
    }

    @Test
    public void selectAction_perform_callsClickWaitOverlayClick_inOrder() {
        RecordingEngine engine = new RecordingEngine();
        new SelectAction(stubSelectable()).perform(engine);
        assertEquals(engine.ops, List.of("click", "waitForOverlay", "click"));
    }

    // ════════════════════════════════════════════════════════════════════
    // SelectByTextAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void selectByTextAction_capability_isSelectable() {
        assertEquals(new SelectByTextAction(stubSelectable(), "option").capability(), ActionCapability.SELECTABLE);
    }

    @Test
    public void selectByTextAction_perform_callsSelectByVisibleText_withText() {
        RecordingEngine engine = new RecordingEngine();
        new SelectByTextAction(stubSelectable(), "Option A").perform(engine);
        assertEquals(engine.ops, List.of("selectByText"));
        assertEquals(engine.lastSelectedText, "Option A");
    }

    // ════════════════════════════════════════════════════════════════════
    // SelectByValueAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void selectByValueAction_capability_isSelectable() {
        assertEquals(new SelectByValueAction(stubSelectable(), "val").capability(), ActionCapability.SELECTABLE);
    }

    @Test
    public void selectByValueAction_perform_callsSelectByValue_withValue() {
        RecordingEngine engine = new RecordingEngine();
        new SelectByValueAction(stubSelectable(), "v1").perform(engine);
        assertEquals(engine.ops, List.of("selectByValue"));
        assertEquals(engine.lastSelectedValue, "v1");
    }

    // ════════════════════════════════════════════════════════════════════
    // UploadAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void uploadAction_capability_isUploadable() {
        assertEquals(new UploadAction(stubUploadable(), "/tmp/f.csv").capability(), ActionCapability.UPLOADABLE);
    }

    @Test
    public void uploadAction_perform_callsUploadFile_withPath() {
        RecordingEngine engine = new RecordingEngine();
        new UploadAction(stubUploadable(), "/files/report.pdf").perform(engine);
        assertEquals(engine.ops, List.of("uploadFile"));
        assertEquals(engine.lastUploadPath, "/files/report.pdf");
    }

    // ════════════════════════════════════════════════════════════════════
    // TypeSearchAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void typeSearchAction_capability_isSearchField() {
        assertEquals(new TypeSearchAction(stubSearchField(), "q").capability(), ActionCapability.SEARCH_FIELD);
    }

    @Test
    public void typeSearchAction_perform_callsEngineType_withSearchTerm() {
        RecordingEngine engine = new RecordingEngine();
        new TypeSearchAction(stubSearchField(), "invoice").perform(engine);
        assertEquals(engine.ops, List.of("type"));
        assertEquals(engine.lastTypedText, "invoice");
    }

    // ════════════════════════════════════════════════════════════════════
    // SubmitSearchAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void submitSearchAction_capability_isSearchField() {
        assertEquals(new SubmitSearchAction(stubSearchField()).capability(), ActionCapability.SEARCH_FIELD);
    }

    @Test
    public void submitSearchAction_perform_callsEngineClick() {
        RecordingEngine engine = new RecordingEngine();
        new SubmitSearchAction(stubSearchField()).perform(engine);
        assertEquals(engine.ops, List.of("click"));
    }

    // ════════════════════════════════════════════════════════════════════
    // SearchAndSelectAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void searchAndSelectAction_capability_isSearchableDropdown() {
        assertEquals(new SearchAndSelectAction(stubSearchableDropdown(), "q").capability(),
                ActionCapability.SEARCHABLE_DROPDOWN);
    }

    @Test
    public void searchAndSelectAction_perform_callsCompositeSequence() {
        RecordingEngine engine = new RecordingEngine();
        new SearchAndSelectAction(stubSearchableDropdown(), "Paris").perform(engine);
        assertEquals(engine.ops, List.of("click", "type", "waitForVisible", "click"));
        assertEquals(engine.lastTypedText, "Paris");
    }

    // ════════════════════════════════════════════════════════════════════
    // ReadTextAction
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void readTextAction_capability_isReadOnly() {
        // READ_ONLY is the only capability that covers non-interactive text elements
        assertEquals(new ReadTextAction(stubReadOnly()).capability(), ActionCapability.READ_ONLY);
    }

    @Test
    public void readTextAction_perform_callsEngineGetText() {
        // execute() must delegate to UIEngine.getText(), not click/type/etc.
        RecordingEngine engine = new RecordingEngine();
        new ReadTextAction(stubReadOnly()).perform(engine);
        assertEquals(engine.ops, List.of("getText"));
    }

    @Test
    public void readTextAction_safely_returnsNewInstance() {
        ReadTextAction action = new ReadTextAction(stubReadOnly());
        assertNotSame(action.safely(), action);
    }

    @Test
    public void readTextAction_defaultSafeProfile_isDefaultSafe() {
        // ReadTextAction inherits DEFAULT_SAFE — waits for visibility, no click or type hooks
        assertSame(new ReadTextAction(stubReadOnly()).defaultSafeProfile(),
                ActionProfiles.DEFAULT_SAFE);
    }

    @Test
    public void readTextAction_defaultReliableProfile_isDefaultReliable() {
        // Inherits DEFAULT_RELIABLE — adds Angular + spinner waits
        assertSame(new ReadTextAction(stubReadOnly()).defaultReliableProfile(),
                ActionProfiles.DEFAULT_RELIABLE);
    }

    @Test
    public void readOnly_readText_emitsReadTextAction() {
        // ReadOnly.readText() factory method must produce a ReadTextAction, not a generic action
        Action action = stubReadOnly().readText();
        assertTrue(action instanceof ReadTextAction,
                "readText() must return a ReadTextAction; got " + action.getClass().getSimpleName());
    }

    @Test
    public void readOnly_readText_capabilityIsReadOnly() {
        // Emitted action must carry READ_ONLY capability for metadata/logging
        assertEquals(stubReadOnly().readText().capability(), ActionCapability.READ_ONLY);
    }

    // ════════════════════════════════════════════════════════════════════
    // Immutability — safely() returns a new instance for all action types
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void allActions_safely_returnsNewInstance() {
        List<Action> actions = List.of(
                new ClickAction(stubClickable()),
                new ToggleAction(stubCheckable()),
                new CheckAction(stubCheckable(), true),
                new HoverAction(stubHoverable()),
                new TypeAction(stubTypeable(), "x"),
                new ClearAction(stubTypeable()),
                new AppendTypeAction(stubTypeable(), "x"),
                new TypeAndPressAction(stubTypeable(), "x", "ENTER"),
                new OpenAction(stubSelectable()),
                new SelectAction(stubSelectable()),
                new SelectByTextAction(stubSelectable(), "opt"),
                new SelectByValueAction(stubSelectable(), "v"),
                new UploadAction(stubUploadable(), "/f"),
                new TypeSearchAction(stubSearchField(), "q"),
                new SubmitSearchAction(stubSearchField()),
                new SearchAndSelectAction(stubSearchableDropdown(), "q"),
                new ReadTextAction(stubReadOnly())           // must also return new instance
        );
        for (Action action : actions) {
            assertNotSame(action.safely(), action,
                    action.getClass().getSimpleName() + ".safely() must return a new instance");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Profile dispatch via defaultSafeProfile()
    // ════════════════════════════════════════════════════════════════════

    @Test
    public void clickAction_defaultSafeProfile_isClickableSafe() {
        assertSame(new ClickAction(stubClickable()).defaultSafeProfile(),
                ActionProfiles.CLICKABLE_SAFE);
    }

    @Test
    public void typeAction_defaultSafeProfile_isTypeableSafe() {
        assertSame(new TypeAction(stubTypeable(), "x").defaultSafeProfile(),
                ActionProfiles.TYPEABLE_SAFE);
    }

    @Test
    public void selectAction_defaultSafeProfile_isSelectableSafe() {
        assertSame(new SelectAction(stubSelectable()).defaultSafeProfile(),
                ActionProfiles.SELECTABLE_SAFE);
    }

    @Test
    public void hoverAction_defaultSafeProfile_isDefaultSafe() {
        assertSame(new HoverAction(stubHoverable()).defaultSafeProfile(),
                ActionProfiles.DEFAULT_SAFE);
    }

    @Test
    public void uploadAction_defaultSafeProfile_isDefaultSafe() {
        assertSame(new UploadAction(stubUploadable(), "/f").defaultSafeProfile(),
                ActionProfiles.DEFAULT_SAFE);
    }
}
