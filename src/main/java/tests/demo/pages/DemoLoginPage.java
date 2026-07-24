package tests.demo.pages;

import elements.api.LocatorFamily;
import elements.api.capability.Clickable;
import elements.api.capability.ReadOnly;
import elements.api.capability.Typeable;

/**
 * UIElement definitions for the-internet.herokuapp.com/login demo page.
 * Follows the Quick Start Guide pattern: capability interfaces + external locators.
 */
public interface DemoLoginPage {

    // --- Text fields ---
    enum Credentials implements Typeable, LocatorFamily {
        USERNAME,
        PASSWORD;

        @Override
        public Object[] getArgs() {
            return new Object[]{name().toLowerCase()};
        }
    }

    // --- Buttons ---
    enum Button implements Clickable {
        LOGIN_BUTTON;
    }

    // --- Labels ---
    enum Labels implements ReadOnly {
        SUCCESS_MESSAGE;
    }
}
