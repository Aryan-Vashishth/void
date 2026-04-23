package elements.api;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for the {@link Element#effectiveArgs(Object...)} and
 * {@link MultipleIdenticalDropdowns#argsForIndex(Integer)} default methods
 * introduced in Phase 4 (Element-side OO consolidation).
 */
public class ElementInterfaceDefaultsTest {

    private static Element elementWithArgs(Object... args) {
        return new Element() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return "//x"; }
            @Override public Object[] getArgs()           { return args; }
        };
    }

    private static MultipleIdenticalDropdowns multiDropdown(Object... args) {
        return new MultipleIdenticalDropdowns() {
            @Override public String getTriggerLocator()   { return "//tr[%s]//button"; }
            @Override public String getListLocator()      { return "//tr[%s]//ul"; }
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return "//tr[%s]//button"; }
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

    // ---------- MultipleIdenticalDropdowns.argsForIndex ----------

    @Test
    public void argsForIndex_nullIndex_returnsOriginalArgs() {
        MultipleIdenticalDropdowns m = multiDropdown("foo");
        assertEquals(m.argsForIndex(null), new Object[]{"foo"});
    }

    @Test
    public void argsForIndex_withIndex_prependsIndex() {
        MultipleIdenticalDropdowns m = multiDropdown("foo");
        assertEquals(m.argsForIndex(3), new Object[]{3, "foo"});
    }

    @Test
    public void argsForIndex_withIndex_emptyArgs_returnsIndexOnly() {
        MultipleIdenticalDropdowns m = multiDropdown();
        assertEquals(m.argsForIndex(7), new Object[]{7});
    }
}

