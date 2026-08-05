package tests.demo.pages.saucedemo;

import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;
import domain.automation.web.vocabulary.capability.Typeable;

public interface LoginPage {

    // ── LoginForm ── username, password fields + submit button ───────────────

    interface LoginForm {

        enum Credentials implements Typeable {
            USERNAME_FIELD,
            PASSWORD_FIELD;
        }

        enum Buttons implements Clickable {
            LOGIN_BUTTON;
        }
    }

    // ── ErrorMessage ── error banner text + dismiss button ───────────────────

    interface ErrorMessage {

        enum Labels implements ReadOnly {
            ERROR_BANNER;
        }

        enum Buttons implements Clickable {
            ERROR_DISMISS;
        }
    }

    // ── CredentialsPanel ── accepted usernames / passwords hint panel ─────────

    enum CredentialsPanel implements ReadOnly {
        ACCEPTED_CREDENTIALS;
    }
}
