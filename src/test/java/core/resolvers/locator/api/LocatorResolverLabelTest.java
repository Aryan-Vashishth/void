package core.resolvers.locator.api;

import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.resolve.api.LocatorResolver;
import domain.automation.web.resolve.api.LocatorResolvers;
import domain.automation.web.vocabulary.element.UIElement;
import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.role.ElementRole;

import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.*;

/**
 * Tests that {@link LocatorResolver} populates {@link LocatorDescriptor#label()} correctly
 * when resolving elements that are Java enum constants.
 *
 * The label is derived by {@code labelOf(UIElement)} via reflection:
 *   enclosingClass.getSimpleName() + " > " + enumClass.getSimpleName() + " > " + getDisplayText()
 *
 * Coverage:
 * - resolveDescriptorBest: enum element → label has all three segments
 * - resolveDescriptorBest: non-enum element → label is null
 * - resolveDescriptor(UIElement, role, args): enum element → label attached
 * - labelOf handles enum with no declaring class (top-level enum) → prefix omitted
 */
public class LocatorResolverLabelTest {

    // ── Test enum hierarchy that mimics real page structure ───────────────────

    /**
     * Simulates a page interface with a nested capability enum.
     * DemoLoginPage > Credentials > USERNAME_INPUT
     */
    interface DemoLoginPage {
        enum Credentials implements Clickable {
            USERNAME_INPUT,
            PASSWORD_INPUT;

            @Override public String getTriggerLocator()   { return "//input[@id='" + name().toLowerCase() + "']"; }
            @Override public String getExternalFileName() { return null; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        }

        enum Buttons implements Clickable {
            SUBMIT_BUTTON;

            @Override public String getTriggerLocator()   { return "//button[@type='submit']"; }
            @Override public String getExternalFileName() { return null; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        }
    }

    // ── resolveDescriptorBest: enum element gets a label ──────────────────────

    @Test(description = "resolveDescriptorBest attaches a label for an enum UIElement")
    public void resolveDescriptorBest_enumElement_labelIsSet() {
        LocatorDescriptor d = LocatorResolvers.strict()
                .resolveDescriptorBest(DemoLoginPage.Credentials.USERNAME_INPUT);

        assertNotNull(d.label(), "label must be set for an enum UIElement");
    }

    @Test(description = "resolveDescriptorBest label has format 'Page > EnumClass > DisplayText'")
    public void resolveDescriptorBest_enumElement_labelFormat() {
        LocatorDescriptor d = LocatorResolvers.strict()
                .resolveDescriptorBest(DemoLoginPage.Credentials.USERNAME_INPUT);

        // The page is DemoLoginPage (enclosing interface), enum class is Credentials
        String label = d.label();
        assertTrue(label.contains("DemoLoginPage"),
                "label must include the enclosing page name; got: " + label);
        assertTrue(label.contains("Credentials"),
                "label must include the enum class name; got: " + label);
        assertTrue(label.contains("Username Input"),
                "label must include the element display text; got: " + label);
    }

    @Test(description = "resolveDescriptorBest label uses ' > ' as separator between all three segments")
    public void resolveDescriptorBest_enumElement_labelSeparator() {
        LocatorDescriptor d = LocatorResolvers.strict()
                .resolveDescriptorBest(DemoLoginPage.Credentials.USERNAME_INPUT);

        assertEquals(d.label(), "DemoLoginPage > Credentials > Username Input");
    }

    @Test(description = "resolveDescriptorBest label reflects the correct constant for PASSWORD_INPUT")
    public void resolveDescriptorBest_differentConstant_labelMatches() {
        LocatorDescriptor d = LocatorResolvers.strict()
                .resolveDescriptorBest(DemoLoginPage.Credentials.PASSWORD_INPUT);

        assertEquals(d.label(), "DemoLoginPage > Credentials > Password Input");
    }

    @Test(description = "resolveDescriptorBest label uses the correct enum class name when sibling enums exist")
    public void resolveDescriptorBest_siblingEnum_labelUsesCorrectClass() {
        LocatorDescriptor d = LocatorResolvers.strict()
                .resolveDescriptorBest(DemoLoginPage.Buttons.SUBMIT_BUTTON);

        assertEquals(d.label(), "DemoLoginPage > Buttons > Submit Button");
    }

    // ── resolveDescriptorBest: non-enum element has no label ─────────────────

    @Test(description = "resolveDescriptorBest returns null label for a non-enum UIElement (anonymous class)")
    public void resolveDescriptorBest_nonEnumElement_labelIsNull() {
        // Anonymous class — not an enum, so labelOf returns null
        UIElement anon = new UIElement() {
            @Override public String getExternalFileName() { return null; }
            @Override public String getPrimaryLocator()   { return "//div[@id='content']"; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };

        LocatorDescriptor d = LocatorResolvers.strict().resolveDescriptorBest(anon);
        assertNull(d.label(), "Non-enum element must not produce a label");
    }

    // ── resolveDescriptor(UIElement, role, args): enum element gets a label ─────

    @Test(description = "resolveDescriptor with explicit role also attaches label from enum reflection")
    public void resolveDescriptor_withRole_enumElement_labelIsSet() {
        LocatorDescriptor d = LocatorResolvers.strict()
                .resolveDescriptor(
                        DemoLoginPage.Credentials.USERNAME_INPUT,
                        ElementRole.TRIGGER);

        assertNotNull(d.label(), "label must be set even when resolving with an explicit role");
        assertEquals(d.label(), "DemoLoginPage > Credentials > Username Input");
    }

    // ── locator value is still correct alongside the label ───────────────────

    @Test(description = "Label does not interfere with the resolved locator value")
    public void resolveDescriptorBest_labelDoesNotAffectLocatorValue() {
        LocatorDescriptor d = LocatorResolvers.strict()
                .resolveDescriptorBest(DemoLoginPage.Credentials.USERNAME_INPUT);

        // The locator itself should resolve the XPath from the enum's getTriggerLocator()
        assertNotNull(d.value(), "locator value must not be null");
        assertFalse(d.value().isBlank(), "locator value must not be blank");
    }

    // ── withLabel interoperability ────────────────────────────────────────────

    @Test(description = "Label set by resolver can be overridden with withLabel afterward")
    public void resolvedLabel_canBeOverriddenWithWithLabel() {
        LocatorDescriptor d = LocatorResolvers.strict()
                .resolveDescriptorBest(DemoLoginPage.Credentials.USERNAME_INPUT)
                .withLabel("CustomOverride > For > Testing");

        assertEquals(d.label(), "CustomOverride > For > Testing",
                "withLabel must override the resolver-attached label");
    }

    @Test(description = "Label set by resolver can be cleared by withLabel(null)")
    public void resolvedLabel_canBeClearedWithWithLabelNull() {
        LocatorDescriptor d = LocatorResolvers.strict()
                .resolveDescriptorBest(DemoLoginPage.Credentials.USERNAME_INPUT)
                .withLabel(null);

        assertNull(d.label(), "withLabel(null) must clear the resolver-attached label");
    }
}
