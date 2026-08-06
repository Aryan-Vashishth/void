package examples.demo.pages.saucedemo;

import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;

public interface CheckoutCompletePage {

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

    // ── OrderConfirmation ── success screen content ───────────────────────────

    enum OrderConfirmation implements ReadOnly {
        PONY_EXPRESS_IMAGE,
        COMPLETE_HEADER,
        COMPLETE_TEXT;
    }

    // ── Actions ── post-order navigation buttons ──────────────────────────────

    enum Actions implements Clickable {
        BACK_HOME_BUTTON,
        GENERATE_PDF_BUTTON;
    }

    // ── Footer ── social links ────────────────────────────────────────────────

    enum Footer implements Clickable {
        TWITTER_LINK,
        FACEBOOK_LINK,
        LINKEDIN_LINK;
    }
}
