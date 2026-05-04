package elements.api;

import elements.api.capability.MultiSelectable;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for the {@link Element#effectiveArgs(Object...)} and
 * {@link MultiSelectable#argsForIndex(Integer)} default methods.
 */
public class ElementInterfaceDefaultsTest {

    private static Element elementWithArgs(Object... args) {
        return new Element() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return "//x"; }
            @Override public Object[] getArgs()           { return args; }
        };
    }

    private static MultiSelectable multiDropdown(Object... args) {
        return new MultiSelectable() {
            @Override public String getTriggerLocator()   { return "//tr[%s]//button"; }
            @Override public String getListLocator()      { return "//tr[%s]//ul"; }
            @Override public String getExternalFileName() { return null; }
            @Override public Object[] getArgs()           { return args; }
        };
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
