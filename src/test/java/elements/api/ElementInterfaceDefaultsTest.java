package elements.api;

import elements.api.capability.MultiSelectable;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link UIElement} default methods.
 */
public class ElementInterfaceDefaultsTest {

    /** Minimal enum for testing {@code getPrimaryLocator()} and {@code getArgs()} defaults. */
    private enum StubElement implements UIElement {
        USERNAME_INPUT,
        LOGIN_BUTTON,
        SAVE_AS_DRAFT,
        CRM_ID,
        H1_HEADING;

        @Override public String getExternalFileName() { return null; }
    }

    /** Enum that overrides {@code getArgs()} to verify explicit overrides still take precedence. */
    private enum DynamicElement implements UIElement {
        PRODUCT_ROW;

        @Override public String getExternalFileName() { return null; }
        @Override public Object[] getArgs()           { return new Object[]{"Laptop"}; }
    }

    private static UIElement elementWithArgs(Object... args) {
        return new UIElement() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return "//x"; }
            @Override public Object[] getArgs()           { return args; }
        };
    }

    // ---------- UIElement.getPrimaryLocator ----------

    @Test
    public void getPrimaryLocator_nestedEnum_returnsNamespacedKey() {
        // StubElement is nested inside ElementInterfaceDefaultsTest
        assertEquals(StubElement.USERNAME_INPUT.getPrimaryLocator(),
                "ElementInterfaceDefaultsTest.StubElement.USERNAME_INPUT");
    }

    @Test
    public void getPrimaryLocator_nestedEnum_multiTokenConstant_returnsNamespacedKey() {
        assertEquals(StubElement.SAVE_AS_DRAFT.getPrimaryLocator(),
                "ElementInterfaceDefaultsTest.StubElement.SAVE_AS_DRAFT");
    }

    @Test
    public void getPrimaryLocator_explicitOverride_returnsOverriddenValue() {
        UIElement e = new UIElement() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return "//custom/xpath"; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
        assertEquals(e.getPrimaryLocator(), "//custom/xpath");
    }

    private static MultiSelectable multiDropdown(Object... args) {
        return new MultiSelectable() {
            @Override public String getTriggerLocator()   { return "//tr[%s]//button"; }
            @Override public String getListLocator()      { return "//tr[%s]//ul"; }
            @Override public String getExternalFileName() { return null; }
            @Override public Object[] getArgs()           { return args; }
        };
    }

    // ---------- UIElement.getArgs ----------

    @Test
    public void getArgs_noOverride_returnsNoArgs() {
        assertSame(StubElement.USERNAME_INPUT.getArgs(), UIElement.NO_ARGS);
    }

    @Test
    public void getArgs_noOverride_returnsEmptyArray() {
        assertEquals(StubElement.LOGIN_BUTTON.getArgs().length, 0);
    }

    @Test
    public void getArgs_explicitOverride_returnsOwnArray() {
        assertEquals(DynamicElement.PRODUCT_ROW.getArgs(), new Object[]{"Laptop"});
    }

    // ---------- UIElement.getDisplayText ----------

    @Test
    public void getDisplayText_singleToken_capitalisesWord() {
        assertEquals(StubElement.USERNAME_INPUT.getDisplayText(), "Username Input");
    }

    @Test
    public void getDisplayText_twoTokens_capitalisesEachWord() {
        assertEquals(StubElement.LOGIN_BUTTON.getDisplayText(), "Login Button");
    }

    @Test
    public void getDisplayText_threeTokens_capitalisesEachWord() {
        assertEquals(StubElement.SAVE_AS_DRAFT.getDisplayText(), "Save As Draft");
    }

    @Test
    public void getDisplayText_acronymToken_lowercasedAfterFirstChar() {
        // "CRM" → "Crm", "ID" → "Id"
        assertEquals(StubElement.CRM_ID.getDisplayText(), "Crm Id");
    }

    @Test
    public void getDisplayText_tokenWithLeadingDigit_preservedAfterCapitalize() {
        // "H1" → charAt(0)='H' upper + "1".toLowerCase()="1" → "H1"
        assertEquals(StubElement.H1_HEADING.getDisplayText(), "H1 Heading");
    }

    @Test
    public void getDisplayText_noOverride_usesEnumName_ignoresArgs() {
        // DynamicElement has args but no getDisplayText() override:
        // the UIElement default derives from enum name, not args.
        assertEquals(DynamicElement.PRODUCT_ROW.getDisplayText(), "Product Row");
    }

    @Test
    public void getDisplayText_anonymousElement_returnsFallbackLabel() {
        // Anonymous class has empty getSimpleName(), split("_") returns [""], all tokens empty.
        // The guard must skip blank tokens and return the "element" fallback.
        UIElement anon = new UIElement() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return "//div"; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
        assertEquals(anon.getDisplayText(), "element");
    }

    @Test
    public void capability_default_returnsUnknown() {
        // The UIElement interface default capability() must return UNKNOWN for untyped elements.
        UIElement anon = new UIElement() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return "//x"; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
        assertEquals(anon.capability(), core.actions.ActionCapability.UNKNOWN);
    }

    // ---------- UIElement.effectiveArgs ----------

    @Test
    public void effectiveArgs_returnsOverridesWhenNonEmpty() {
        UIElement e = elementWithArgs("base");
        assertEquals(e.effectiveArgs("override"), new Object[]{"override"});
    }

    @Test
    public void effectiveArgs_returnsElementArgsWhenOverridesEmpty() {
        UIElement e = elementWithArgs("base");
        assertEquals(e.effectiveArgs(), new Object[]{"base"});
    }

    @Test
    public void effectiveArgs_returnsElementArgsWhenOverridesNull() {
        UIElement e = elementWithArgs("base");
        assertEquals(e.effectiveArgs((Object[]) null), new Object[]{"base"});
    }

    @Test
    public void effectiveArgs_supportsMultipleOverrides() {
        UIElement e = elementWithArgs("base");
        assertEquals(e.effectiveArgs("a", "b", "c"), new Object[]{"a", "b", "c"});
    }

    // ---------- MultiSelectable.argsForIndex ----------

    @Test
    public void argsForIndex_nullIndex_returnsOriginalArgs() {
        MultiSelectable m = multiDropdown("foo");
        assertEquals(m.argsForIndex(null), new Object[]{"foo"});
    }

    @Test
    public void argsForIndex_withIndex_prependsIndex() {
        MultiSelectable m = multiDropdown("foo");
        assertEquals(m.argsForIndex(3), new Object[]{3, "foo"});
    }

    @Test
    public void argsForIndex_withIndex_emptyArgs_returnsIndexOnly() {
        MultiSelectable m = multiDropdown();
        assertEquals(m.argsForIndex(7), new Object[]{7});
    }
}
