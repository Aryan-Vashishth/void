package elements.api.capability;

import domain.automation.web.vocabulary.capability.Listable;
import domain.automation.web.vocabulary.element.UIElement;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link Listable#getIndex()} default behaviour.
 *
 * <p>The contract: enum implementors return the enum ordinal; non-enum implementors
 * throw {@link UnsupportedOperationException} rather than silently returning 0.</p>
 */
public class ListableTest {

    private enum ListItems implements Listable {
        FIRST_ITEM,
        SECOND_ITEM,
        THIRD_ITEM;

        @Override public String getExternalFileName() { return null; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    }

    private static Listable nonEnumListable() {
        return new Listable() {
            @Override public String getExternalFileName() { return null; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    // ── enum implementors ────────────────────────────────────────────────

    @Test
    public void getIndex_firstEnumConstant_returnsZero() {
        assertEquals(ListItems.FIRST_ITEM.getIndex(), 0);
    }

    @Test
    public void getIndex_secondEnumConstant_returnsOne() {
        assertEquals(ListItems.SECOND_ITEM.getIndex(), 1);
    }

    @Test
    public void getIndex_thirdEnumConstant_returnsTwo() {
        assertEquals(ListItems.THIRD_ITEM.getIndex(), 2);
    }

    @Test
    public void getIndex_enumOrdinalMatchesJavaOrdinal() {
        for (ListItems item : ListItems.values()) {
            assertEquals(item.getIndex(), item.ordinal(),
                    "getIndex() must match enum ordinal for: " + item.name());
        }
    }

    // ── non-enum implementors ────────────────────────────────────────────

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void getIndex_nonEnumImplementor_throwsUnsupportedOperationException() {
        nonEnumListable().getIndex();
    }

    @Test
    public void getIndex_nonEnumImplementor_exceptionMessageMentionsListable() {
        try {
            nonEnumListable().getIndex();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Listable"),
                    "exception message must mention Listable; got: " + e.getMessage());
        }
    }

    @Test
    public void getIndex_nonEnumImplementor_exceptionMessageMentionsOverride() {
        try {
            nonEnumListable().getIndex();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().toLowerCase().contains("override"),
                    "exception message must suggest Override; got: " + e.getMessage());
        }
    }
}
