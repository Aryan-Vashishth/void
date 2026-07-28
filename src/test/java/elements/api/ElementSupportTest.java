package elements.api;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link ElementSupport}: nameOf, declaringClassOf, ordinalOf.
 *
 * <p>ElementSupport is package-private; this test lives in the same package
 * to access it directly without reflection wrappers.</p>
 */
public class ElementSupportTest {

    private enum Outer implements UIElement {
        FIRST, SECOND, THIRD;
        @Override public String getExternalFileName() { return null; }
    }

    interface Page {
        enum Nested implements UIElement {
            ALPHA, BETA;
            @Override public String getExternalFileName() { return null; }
        }
    }

    private static UIElement anonymousElement() {
        return new UIElement() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return "//x"; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    // ── nameOf ────────────────────────────────────────────────────────────

    @Test
    public void nameOf_enumElement_returnsEnumConstantName() {
        assertEquals(ElementSupport.nameOf(Outer.FIRST), "FIRST");
    }

    @Test
    public void nameOf_enumElement_returnsCorrectConstantByOrdinal() {
        assertEquals(ElementSupport.nameOf(Outer.THIRD), "THIRD");
    }

    @Test
    public void nameOf_anonymousElement_returnsSimpleName() {
        UIElement anon = anonymousElement();
        // Anonymous class simple name is empty string
        assertEquals(ElementSupport.nameOf(anon), anon.getClass().getSimpleName());
    }

    @Test
    public void nameOf_nestedEnumElement_returnsConstantName_notQualifiedName() {
        assertEquals(ElementSupport.nameOf(Page.Nested.ALPHA), "ALPHA");
    }

    // ── declaringClassOf ─────────────────────────────────────────────────

    @Test
    public void declaringClassOf_enumElement_returnsDeclaringClass() {
        assertSame(ElementSupport.declaringClassOf(Outer.FIRST), Outer.class);
    }

    @Test
    public void declaringClassOf_nestedEnumElement_returnsNestedEnumClass() {
        assertSame(ElementSupport.declaringClassOf(Page.Nested.BETA), Page.Nested.class);
    }

    @Test
    public void declaringClassOf_anonymousElement_returnsAnonymousClass() {
        UIElement anon = anonymousElement();
        assertSame(ElementSupport.declaringClassOf(anon), anon.getClass());
    }

    // ── ordinalOf ────────────────────────────────────────────────────────

    @Test
    public void ordinalOf_firstConstant_returnsZero() {
        assertEquals(ElementSupport.ordinalOf(Outer.FIRST), 0);
    }

    @Test
    public void ordinalOf_secondConstant_returnsOne() {
        assertEquals(ElementSupport.ordinalOf(Outer.SECOND), 1);
    }

    @Test
    public void ordinalOf_thirdConstant_returnsTwo() {
        assertEquals(ElementSupport.ordinalOf(Outer.THIRD), 2);
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void ordinalOf_nonEnumElement_throwsUnsupportedOperationException() {
        ElementSupport.ordinalOf(anonymousElement());
    }

    @Test
    public void ordinalOf_nonEnumElement_exceptionMessageContainsClassName() {
        UIElement anon = anonymousElement();
        try {
            ElementSupport.ordinalOf(anon);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Listable"),
                    "message must mention Listable override; got: " + e.getMessage());
        }
    }
}
