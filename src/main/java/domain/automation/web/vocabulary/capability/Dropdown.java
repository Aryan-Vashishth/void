package domain.automation.web.vocabulary.capability;

import core.actions.ActionCapability;
import domain.automation.web.vocabulary.actions.DropdownSelectAction;
import domain.automation.web.vocabulary.element.UIElement;
import domain.automation.web.vocabulary.role.ElementRole;

/**
 * Capability interface for native {@code <select>} dropdown elements.
 *
 * <p>Models the pattern where <b>enum constants ARE the options</b>: each constant
 * carries its visible option label via {@link #getArgs()}{@code [0]}, and all constants
 * in the same enum share a single locator pointing to the {@code <select>} container.</p>
 *
 * <h3>Page-object declaration</h3>
 * <pre>
 *   enum Options implements Dropdown {
 *       NAME_A_TO_Z("Name (A to Z)"),
 *       NAME_Z_TO_A("Name (Z to A)");
 *
 *       private final String label;
 *       Options(String label) { this.label = label; }
 *
 *       {@literal @}Override public Object[] getArgs() { return new Object[]{label}; }
 *   }
 * </pre>
 *
 * <h3>Test usage</h3>
 * <pre>
 *   app.flow().run(Flow.of(ProductsPage.SortDropdown.Options.NAME_Z_TO_A.select()));
 * </pre>
 *
 * <h3>Locator convention</h3>
 * <p>All constants point to the same {@code TRIGGER} locator (the {@code <select>} container).
 * {@link DropdownSelectAction} resolves the TRIGGER role and calls
 * {@link domain.automation.web.engine.UIEngine#selectByVisibleText} with the option label.</p>
 *
 * <p>Primary locator role: {@link ElementRole#TRIGGER}</p>
 */
public interface Dropdown extends UIElement {

    /** @return fully-qualified role-suffixed locator key for the select container. */
    default String getTriggerLocator() {
        return locatorKeyForRole(ElementRole.TRIGGER);
    }

    @Override
    default String getDisplayText() {
        Object[] args = getArgs();
        return args.length > 0 ? args[0].toString() : UIElement.super.getDisplayText();
    }

    @Override
    default ActionCapability capability() {
        return ActionCapability.SELECTABLE;
    }

    /** Emits a {@link DropdownSelectAction} that selects this constant's option by visible text. */
    default DropdownSelectAction select() {
        return new DropdownSelectAction(this);
    }
}
