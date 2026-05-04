package tests.demo.pages;

import elements.api.capability.Clickable;
import elements.api.capability.ReadOnly;
import elements.api.capability.Typeable;

/**
 * Element definitions for the-internet.herokuapp.com/login demo page.
 * Follows the Quick Start Guide pattern: capability interfaces + external locators.
 */
public interface DemoLoginPage {

    /** Shared JSON locator file for all enums in this interface. */
    String LOCATOR_FILE = "demo-login-elements.json";

    // --- Text fields use Typeable (role: INPUT) ---
    enum Credentials implements Typeable {
        USERNAME_INPUT("USERNAME_INPUT"),
        PASSWORD_INPUT("PASSWORD_INPUT");

        private final String key;
        Credentials(String k) { this.key = k; }

        @Override public String getInputLocator()     { return key; }
        @Override public String getExternalFileName() { return LOCATOR_FILE; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    }

    // --- Buttons use Clickable (role: TRIGGER) ---
    enum Button implements Clickable {
        LOGIN_BUTTON("LOGIN_BUTTON", "Login");

        private final String key;
        private final String label;
        Button(String k, String l) { this.key = k; this.label = l; }

        @Override public String getTriggerLocator()   { return key; }
        @Override public String getExternalFileName() { return LOCATOR_FILE; }
        @Override public Object[] getArgs()           { return new Object[]{label}; }
    }

    // --- Labels use ReadOnly (role: TEXT) ---
    enum Labels implements ReadOnly {
        SUCCESS_MESSAGE("SUCCESS_MESSAGE");

        private final String key;
        Labels(String k) { this.key = k; }

        @Override public String getTextLocator()      { return key; }
        @Override public String getExternalFileName() { return LOCATOR_FILE; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    }
}

