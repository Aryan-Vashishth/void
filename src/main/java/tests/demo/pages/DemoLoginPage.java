package tests.demo.pages;

import domain.automation.web.vocabulary.element.LocatorFamily;
import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;
import domain.automation.web.vocabulary.capability.Typeable;

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
