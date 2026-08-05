package domain.automation.web.vocabulary.capability;

import domain.automation.web.vocabulary.actions.ParameterizedClickAction;

/**
 * Capability interface for elements whose click locator contains substitution placeholders
 * (e.g., {@code //button[@data-test='add-to-cart-%1$s']}).
 *
 * <p>Overrides {@link Clickable#click()} with a covariant return type so that only
 * elements implementing this interface expose {@link ParameterizedClickAction#withArgs}.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   app.run(Flow.of(
 *       ADD_TO_CART_BUTTON.click().withArgs("sauce-labs-backpack")
 *   ));
 * </pre>
 *
 * <p>Static elements implementing only {@link Clickable} cannot call {@code withArgs} --
 * the constraint is enforced at compile time, not by documentation.</p>
 */
public interface ParameterizedClickable extends Clickable {

    @Override
    default ParameterizedClickAction click() {
        return new ParameterizedClickAction(this);
    }
}
