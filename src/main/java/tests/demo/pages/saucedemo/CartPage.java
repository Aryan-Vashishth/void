package tests.demo.pages.saucedemo;

import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;

public interface CartPage {

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

    // ── CartItem ── per-item actions and labels (%s = item index or slug) ─────

    interface CartItem {

        enum Buttons implements Clickable {
            ITEM_TITLE_LINK,
            REMOVE_BUTTON;
        }

        enum Labels implements ReadOnly {
            ITEM_NAME,
            QUANTITY,
            ITEM_PRICE;
        }
    }

    // ── Actions ── page-level navigation buttons ──────────────────────────────

    enum Actions implements Clickable {
        CONTINUE_SHOPPING_BUTTON,
        CHECKOUT_BUTTON;
    }

    // ── Footer ── social links ────────────────────────────────────────────────

    enum Footer implements Clickable {
        TWITTER_LINK,
        FACEBOOK_LINK,
        LINKEDIN_LINK;
    }
}
