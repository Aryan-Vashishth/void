package core.actions;

import core.engine.Executor;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Extension fitness test for the open ActionCapability set (runtime-redesign I3.1).
 *
 * <p>Validates that a domain can define custom capabilities and use them in an Action
 * without editing any runtime-owned files.</p>
 */
public class ActionCapabilityExtensionTest {

    // Custom capability defined entirely outside runtime-owned files.
    private static final ActionCapability FORM_SUBMITTABLE = ActionCapability.of("FORM_SUBMITTABLE");

    @Test(description = "custom capability reports its name correctly")
    public void customCapability_name_returnsGivenName() {
        assertEquals(FORM_SUBMITTABLE.name(), "FORM_SUBMITTABLE");
    }

    @Test(description = "two capabilities with the same name are equal")
    public void customCapability_equality_nameBasedEquals() {
        assertEquals(FORM_SUBMITTABLE, ActionCapability.of("FORM_SUBMITTABLE"));
    }

    @Test(description = "two capabilities with the same name have the same hash code")
    public void customCapability_hashCode_nameBasedHashCode() {
        assertEquals(FORM_SUBMITTABLE.hashCode(), ActionCapability.of("FORM_SUBMITTABLE").hashCode());
    }

    @Test(description = "capabilities with different names are not equal")
    public void customCapability_differentNames_notEqual() {
        assertNotEquals(FORM_SUBMITTABLE, ActionCapability.of("OTHER"));
    }

    @Test(description = "built-in constants preserve their canonical names after migration to interface")
    public void builtInConstants_names_preserved() {
        assertEquals(ActionCapability.CLICKABLE.name(),            "CLICKABLE");
        assertEquals(ActionCapability.TYPEABLE.name(),             "TYPEABLE");
        assertEquals(ActionCapability.SELECTABLE.name(),           "SELECTABLE");
        assertEquals(ActionCapability.HOVERABLE.name(),            "HOVERABLE");
        assertEquals(ActionCapability.CHECKABLE.name(),            "CHECKABLE");
        assertEquals(ActionCapability.UPLOADABLE.name(),           "UPLOADABLE");
        assertEquals(ActionCapability.SEARCHABLE.name(),           "SEARCHABLE");
        assertEquals(ActionCapability.SEARCH_FIELD.name(),         "SEARCH_FIELD");
        assertEquals(ActionCapability.SEARCHABLE_DROPDOWN.name(),  "SEARCHABLE_DROPDOWN");
        assertEquals(ActionCapability.READ_ONLY.name(),            "READ_ONLY");
        assertEquals(ActionCapability.TABLE.name(),                "TABLE");
        assertEquals(ActionCapability.EDITABLE_TABLE.name(),       "EDITABLE_TABLE");
        assertEquals(ActionCapability.LISTABLE.name(),             "LISTABLE");
        assertEquals(ActionCapability.MULTI_SELECTABLE.name(),     "MULTI_SELECTABLE");
        assertEquals(ActionCapability.UNKNOWN.name(),              "UNKNOWN");
    }

    @Test(description = "built-in constant equals a freshly created capability of the same name")
    public void builtInConstant_equalsOfSameName() {
        assertEquals(ActionCapability.CLICKABLE, ActionCapability.of("CLICKABLE"));
        assertEquals(ActionCapability.UNKNOWN,   ActionCapability.of("UNKNOWN"));
    }

    @Test(description = "of() throws on null name")
    public void of_nullName_throws() {
        assertThrows(IllegalArgumentException.class, () -> ActionCapability.of(null));
    }

    @Test(description = "of() throws on blank name")
    public void of_blankName_throws() {
        assertThrows(IllegalArgumentException.class, () -> ActionCapability.of("   "));
    }

    @Test(description = "extension test: Action carrying a custom capability requires zero runtime edits")
    public void extensionTest_actionWithCustomCapability_noRuntimeEditsRequired() {
        // Define a custom action carrying the custom capability -- no runtime files touched.
        Action customAction = new Action() {
            @Override public void perform(Executor executor) {}
            @Override public ActionCapability capability() { return FORM_SUBMITTABLE; }
        };

        assertEquals(customAction.capability(), FORM_SUBMITTABLE);
        assertEquals(customAction.capability().name(), "FORM_SUBMITTABLE");
        assertNotEquals(customAction.capability(), ActionCapability.UNKNOWN);
    }
}
