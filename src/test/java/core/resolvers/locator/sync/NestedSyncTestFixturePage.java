package core.resolvers.locator.sync;

import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;
import domain.automation.web.vocabulary.capability.Typeable;

/**
 * Fixture with nested interfaces for LocatorSync unit tests.
 * Models a page with grouped element sections (like a real login page).
 */
interface NestedSyncTestFixturePage {

    interface LoginForm {
        enum Fields implements Typeable   { USERNAME_FIELD, PASSWORD_FIELD }
        enum Buttons implements Clickable { LOGIN_BUTTON }
    }

    interface ErrorBanner {
        enum Labels implements ReadOnly   { ERROR_MSG }
        enum Buttons implements Clickable { DISMISS_BUTTON }
    }

    enum PageActions implements Clickable { FORGOT_PASSWORD_LINK }
}
