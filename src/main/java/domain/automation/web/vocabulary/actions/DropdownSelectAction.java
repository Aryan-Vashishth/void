package domain.automation.web.vocabulary.actions;

import core.actions.ActionCapability;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.vocabulary.capability.Dropdown;
import domain.automation.web.vocabulary.role.ElementRole;

/**
 * Action that selects a native {@code <select>} option by its visible text.
 *
 * <p>Emitted by {@link Dropdown#select()}. The option label is taken from
 * {@link Dropdown#getArgs()}{@code [0]}, which each enum constant carries as its
 * constructor argument. The locator always resolves to the {@code TRIGGER} role
 * (the {@code <select>} container); all constants in the enum share the same locator.</p>
 *
 * <h3>Example</h3>
 * <pre>
 *   // In ProductsPage:
 *   enum Options implements Dropdown {
 *       NAME_Z_TO_A("Name (Z to A)");
 *       private final String label;
 *       Options(String label) { this.label = label; }
 *       {@literal @}Override public Object[] getArgs() { return new Object[]{label}; }
 *   }
 *
 *   // In test:
 *   app.flow().run(Flow.of(ProductsPage.SortDropdown.Options.NAME_Z_TO_A.select()));
 * </pre>
 */
public final class DropdownSelectAction extends SelectableElementAction {

    private final String optionText;

    public DropdownSelectAction(Dropdown element) {
        super(element, ElementRole.TRIGGER, ActionCapability.SELECTABLE);
        Object[] args = element.getArgs();
        this.optionText = (args != null && args.length > 0) ? args[0].toString() : "";
    }

    @Override
    public String operationLabel() {
        return "selectOption";
    }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.selectByVisibleText(descriptor, optionText);
    }
}
