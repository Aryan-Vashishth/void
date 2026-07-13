package tests.demo.pages;

import elements.api.capability.Clickable;
import elements.api.capability.ReadOnly;
import elements.api.capability.Typeable;

/**
 * Element definitions for the-internet.herokuapp.com/login demo page.
 * Follows the Quick Start Guide pattern: capability interfaces + external locators.
 */
public interface DemoLoginPage {

    // --- Text fields ---
    enum Credentials implements Typeable {
        USERNAME_INPUT,
        PASSWORD_INPUT;
    }

    // --- Buttons ---
    enum Button implements Clickable {
        LOGIN_BUTTON("Login");

        private final String label;
        Button(String l) { this.label = l; }

        @Override public Object[] getArgs() { return new Object[]{label}; }
    }

    // --- Labels ---
    enum Labels implements ReadOnly {
        SUCCESS_MESSAGE;
    }
}
