package elements.api;

import elements.api.capability.MultiSelectable;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link Element} default methods.
 */
public class ElementInterfaceDefaultsTest {

    /** Minimal enum for testing {@code getPrimaryLocator()} and {@code getArgs()} defaults. */
    private enum StubElement implements Element {
        USERNAME_INPUT,
        LOGIN_BUTTON,
        SAVE_AS_DRAFT;

        @Override public String getExternalFileName() { return null; }
    }

    /** Enum that overrides {@code getArgs()} to verify explicit overrides still take precedence. */
    private enum DynamicElement implements Element {
        PRODUCT_ROW;

        @Override public String getExternalFileName() { return null; }
        @Override public Object[] getArgs()           { return new Object[]{"Laptop"}; }
    }

    private static Element elementWithArgs(Object... args) {
        return new Element() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return "//x"; }
            @Override public Object[] getArgs()           { return args; }
        };
    }

    // ---------- Element.getPrimaryLocator ----------

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
        Element e = new Element() {
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

    // ---------- Element.getArgs ----------

    @Test
    public void getArgs_noOverride_returnsNoArgs() {
        assertSame(StubElement.USERNAME_INPUT.getArgs(), Element.NO_ARGS);
    }

    @Test
    public void getArgs_noOverride_returnsEmptyArray() {
        assertEquals(StubElement.LOGIN_BUTTON.getArgs().length, 0);
    }

    @Test
    public void getArgs_explicitOverride_returnsOwnArray() {
        assertEquals(DynamicElement.PRODUCT_ROW.getArgs(), new Object[]{"Laptop"});
    }

    // ---------- Element.effectiveArgs ----------

    @Test
    public void effectiveArgs_returnsOverridesWhenNonEmpty() {
        Element e = elementWithArgs("base");
        assertEquals(e.effectiveArgs("override"), new Object[]{"override"});
    }

    @Test
    public void effectiveArgs_returnsElementArgsWhenOverridesEmpty() {
        Element e = elementWithArgs("base");
        assertEquals(e.effectiveArgs(), new Object[]{"base"});
    }

    @Test
    public void effectiveArgs_returnsElementArgsWhenOverridesNull() {
        Element e = elementWithArgs("base");
        assertEquals(e.effectiveArgs((Object[]) null), new Object[]{"base"});
    }

    @Test
    public void effectiveArgs_supportsMultipleOverrides() {
        Element e = elementWithArgs("base");
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
