package examples.demo.pages.saucedemo;

import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;
import domain.automation.web.vocabulary.capability.Typeable;

public interface CheckoutStepOnePage {

    // ── Header ── nav buttons + text labels ──────────────────────────────────

    interface Header {

        enum Buttons implements Clickable {
            MENU_BUTTON,
            CART_LINK;
        }

        enum Labels implements ReadOnly {
            PAGE_TITLE,
            CART_BADGE;
        }
    }

    // ── SideMenu ── all navigation links ─────────────────────────────────────

    enum SideMenu implements Clickable {
        ALL_ITEMS_LINK,
        ABOUT_LINK,
        LOGOUT_LINK,
        RESET_APP_STATE_LINK,
        CLOSE_MENU_BUTTON;
    }

    // ── CheckoutInfoForm ── customer info fields + form actions ───────────────

    interface CheckoutInfoForm {

        enum Fields implements Typeable {
            FIRST_NAME_INPUT,
            LAST_NAME_INPUT,
            POSTAL_CODE_INPUT;
        }

        enum Buttons implements Clickable {
            CANCEL_BUTTON,
            CONTINUE_BUTTON;
        }
    }

    // ── ErrorMessage ── validation error banner ───────────────────────────────

    enum ErrorMessage implements ReadOnly {
        ERROR_BANNER;
    }

    // ── Footer ── social links ────────────────────────────────────────────────

    enum Footer implements Clickable {
        TWITTER_LINK,
        FACEBOOK_LINK,
        LINKEDIN_LINK;
    }
}
