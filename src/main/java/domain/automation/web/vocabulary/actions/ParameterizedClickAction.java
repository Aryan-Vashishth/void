package domain.automation.web.vocabulary.actions;

import core.target.Target;
import domain.automation.web.vocabulary.capability.ParameterizedClickable;

/**
 * Parameterized click action for elements whose locator contains substitution placeholders
 * (e.g., {@code //button[@data-test='add-to-cart-%1$s']}).
 *
 * <p>Emitted by {@link ParameterizedClickable#click()}. Callers supply the slug or index
 * via {@link #withArgs} before adding the action to a {@code Flow}:</p>
 * <pre>
 *   app.run(Flow.of(ADD_TO_CART_BUTTON.click().withArgs("sauce-labs-backpack")));
 * </pre>
 *
 * <p>Extends {@link ClickAction} -- inherits CLICKABLE_SAFE/RELIABLE profiles,
 * {@code execute()}, and {@code operationLabel()} unchanged. Only adds {@link #withArgs}
 * and the {@link #locatorArgs()} hook override.</p>
 *
 * <p>Engine-neutral: args are forwarded to {@link domain.automation.web.engine.UIEngine#resolve},
 * which every engine (Selenium, Playwright, etc.) implements.</p>
 */
public final class ParameterizedClickAction extends ClickAction {

    private Object[] storedArgs = Target.NO_ARGS;

    public ParameterizedClickAction(ParameterizedClickable element) {
        super(element);
    }

    /**
     * Supplies locator substitution args for this action.
     *
     * <p>A second call overwrites the first. Pass this action immediately into
     * {@code Flow.of()} -- there is no window to mutate it after execution.</p>
     *
     * @param args substitution args forwarded to {@code UIEngine.resolve()}; null is treated as no args
     * @return {@code this} for fluent chaining
     */
    public ParameterizedClickAction withArgs(Object... args) {
        this.storedArgs = (args != null) ? args : Target.NO_ARGS;
        return this;
    }

    @Override
    protected Object[] locatorArgs() {
        return storedArgs;
    }
}
