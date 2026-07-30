package core.engine;

import elements.locator.LocatorDescriptor;
import elements.locator.LocatorStrategy;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Unit tests for the {@code label} component and {@link LocatorDescriptor#withLabel(String)}
 * added to LocatorDescriptor as part of the structured element-path logging feature.
 *
 * Every test is a pure data test — no I/O, no logger, no driver required.
 */
public class LocatorDescriptorLabelTest {

    // ── label is null by default ───────────────────────────────────────────────

    @Test(description = "Two-arg convenience constructor leaves label null")
    public void twoArgConstructor_labelIsNull() {
        LocatorDescriptor d = new LocatorDescriptor("//div", LocatorStrategy.XPATH);
        assertNull(d.label(), "label should be null when not set");
    }

    @Test(description = "Three-arg convenience constructor leaves label null")
    public void threeArgConstructor_labelIsNull() {
        LocatorDescriptor d = new LocatorDescriptor("//div", LocatorStrategy.XPATH, new Object[]{"arg"});
        assertNull(d.label(), "label should be null when not explicitly provided");
    }

    @Test(description = "LocatorDescriptor.of(value) factory leaves label null")
    public void factoryOf_value_labelIsNull() {
        LocatorDescriptor d = LocatorDescriptor.of("//span");
        assertNull(d.label());
    }

    @Test(description = "LocatorDescriptor.of(value, strategy) factory leaves label null")
    public void factoryOf_valueStrategy_labelIsNull() {
        LocatorDescriptor d = LocatorDescriptor.of("btn", LocatorStrategy.ID);
        assertNull(d.label());
    }

    @Test(description = "LocatorDescriptor.of(value, strategy, args) factory leaves label null")
    public void factoryOf_valueStrategyArgs_labelIsNull() {
        LocatorDescriptor d = LocatorDescriptor.of("//tr[%s]", LocatorStrategy.XPATH, "1");
        assertNull(d.label());
    }

    // ── withLabel sets the label ───────────────────────────────────────────────

    @Test(description = "withLabel returns a new descriptor with the label set")
    public void withLabel_setsLabel() {
        LocatorDescriptor base = new LocatorDescriptor("//input[@id='u']", LocatorStrategy.XPATH);
        LocatorDescriptor labeled = base.withLabel("DemoLoginPage > Credentials > USERNAME_INPUT");

        assertEquals(labeled.label(), "DemoLoginPage > Credentials > USERNAME_INPUT");
    }

    @Test(description = "withLabel returns a different instance — LocatorDescriptor is immutable")
    public void withLabel_returnsNewInstance() {
        LocatorDescriptor base = new LocatorDescriptor("//input", LocatorStrategy.XPATH);
        LocatorDescriptor labeled = base.withLabel("SomePage > SomeEnum > SOME_FIELD");

        assertNotSame(base, labeled, "withLabel must return a new instance");
        assertNull(base.label(), "original must be unchanged");
    }

    @Test(description = "withLabel preserves value, strategy, and args from the original")
    public void withLabel_preservesAllOtherFields() {
        Object[] args = {"row1"};
        LocatorDescriptor base = new LocatorDescriptor("//tr[%s]", LocatorStrategy.XPATH, args);
        LocatorDescriptor labeled = base.withLabel("TablePage > Rows > DATA_ROW");

        assertEquals(labeled.value(), "//tr[%s]");
        assertEquals(labeled.strategy(), LocatorStrategy.XPATH);
        assertSame(labeled.args(), args, "args array reference should be the same object");
        assertEquals(labeled.label(), "TablePage > Rows > DATA_ROW");
    }

    @Test(description = "withLabel null clears a previously set label")
    public void withLabel_null_clearsLabel() {
        LocatorDescriptor labeled = new LocatorDescriptor("//div", LocatorStrategy.XPATH)
                .withLabel("Page > Enum > CONSTANT");
        LocatorDescriptor cleared = labeled.withLabel(null);

        assertNull(cleared.label(), "label should be null after calling withLabel(null)");
        assertEquals(labeled.label(), "Page > Enum > CONSTANT", "original must be unchanged");
    }

    @Test(description = "withLabel with empty string stores the empty string (not null)")
    public void withLabel_emptyString_isNotNull() {
        LocatorDescriptor d = new LocatorDescriptor("//div", LocatorStrategy.XPATH).withLabel("");
        assertNotNull(d.label());
        assertEquals(d.label(), "");
    }

    // ── withLabel composes with withParent ─────────────────────────────────────

    @Test(description = "withLabel preserves label when withParent is called afterward")
    public void withLabel_thenWithParent_preservesLabel() {
        LocatorDescriptor parent = new LocatorDescriptor("//table", LocatorStrategy.XPATH);
        LocatorDescriptor child = new LocatorDescriptor("//td", LocatorStrategy.XPATH)
                .withLabel("TablePage > Cells > DATA_CELL");

        // Adding a parent should not drop the label
        LocatorDescriptor scoped = child.withParent(parent);

        assertEquals(scoped.label(), "TablePage > Cells > DATA_CELL",
                "label must survive withParent call");
        assertSame(scoped.parent(), parent, "parent must be set");
        assertEquals(scoped.value(), "//td");
    }

    @Test(description = "withParent preserves label that was set before the parent was added")
    public void withParent_preservesExistingLabel() {
        LocatorDescriptor parent = LocatorDescriptor.of("//div[@class='modal']");
        LocatorDescriptor child = LocatorDescriptor.of("//button", LocatorStrategy.XPATH)
                .withLabel("ModalPage > Actions > CONFIRM_BUTTON");

        LocatorDescriptor scoped = child.withParent(parent);

        assertFalse(scoped.isScoped() == false, "should be scoped after withParent");
        assertTrue(scoped.isScoped());
        assertEquals(scoped.label(), "ModalPage > Actions > CONFIRM_BUTTON");
    }

    @Test(description = "withLabel on a scoped descriptor preserves the parent")
    public void scopedDescriptor_withLabel_preservesParent() {
        LocatorDescriptor parent = LocatorDescriptor.of("//div");
        LocatorDescriptor child = LocatorDescriptor.of("//span").withParent(parent);
        LocatorDescriptor labeled = child.withLabel("SomePage > Group > FIELD");

        assertSame(labeled.parent(), parent, "parent must survive withLabel");
        assertEquals(labeled.label(), "SomePage > Group > FIELD");
    }

    // ── isScoped is unaffected by label ───────────────────────────────────────

    @Test(description = "isScoped returns false for a labeled descriptor without a parent")
    public void withLabel_noParent_isScopedFalse() {
        LocatorDescriptor d = LocatorDescriptor.of("//div").withLabel("P > E > C");
        assertFalse(d.isScoped());
    }

    @Test(description = "isScoped returns true for a labeled descriptor that has a parent")
    public void withLabel_withParent_isScopedTrue() {
        LocatorDescriptor parent = LocatorDescriptor.of("//table");
        LocatorDescriptor d = LocatorDescriptor.of("//td").withLabel("P > E > C").withParent(parent);
        assertTrue(d.isScoped());
    }

    // ── canonical constructor ─────────────────────────────────────────────────

    @Test(description = "Canonical 5-arg constructor stores label exactly")
    public void canonicalConstructor_storesLabel() {
        String label = "LoginPage > Credentials > USERNAME_INPUT";
        LocatorDescriptor d = new LocatorDescriptor(
                "//input[@id='user']", LocatorStrategy.XPATH, new Object[0], null, label);

        assertEquals(d.label(), label);
        assertNull(d.parent());
        assertFalse(d.isScoped());
    }

    // ── chained withLabel calls ───────────────────────────────────────────────

    @Test(description = "Calling withLabel twice — last call wins")
    public void withLabel_calledTwice_lastWins() {
        LocatorDescriptor d = LocatorDescriptor.of("//div")
                .withLabel("First > Label > HERE")
                .withLabel("Second > Label > HERE");

        assertEquals(d.label(), "Second > Label > HERE");
    }
}
