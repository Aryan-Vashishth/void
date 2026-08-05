package tests.demo.pages.saucedemo;

import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ParameterizedClickable;
import domain.automation.web.vocabulary.capability.ReadOnly;

public interface ProductsPage {

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

    // ── SortDropdown ── sort control + active label ───────────────────────────

    interface SortDropdown {

        enum Controls implements Clickable {
            SORT_SELECT;
        }

        enum Labels implements ReadOnly {
            ACTIVE_OPTION_LABEL;
        }
    }

    // ── ProductItem ── per-item actions and labels (%s = product slug / index)

    interface ProductItem {

        enum Buttons implements Clickable {
            ITEM_TITLE_LINK;
        }

        enum DynamicButtons implements ParameterizedClickable {
            ADD_TO_CART_BUTTON,   // XPath uses %1$s for product slug
            REMOVE_BUTTON;        // XPath uses %1$s for product slug
        }

        enum Labels implements ReadOnly {
            ITEM_NAME,      // all product name elements (for count)
            ITEM_NAME_AT,   // XPath uses %1$s for 1-based index
            ITEM_PRICE,     // all price elements (for count)
            ITEM_PRICE_AT;  // XPath uses %1$s for 1-based index
        }
    }

    // ── Footer ── social links ────────────────────────────────────────────────

    enum Footer implements Clickable {
        TWITTER_LINK,
        FACEBOOK_LINK,
        LINKEDIN_LINK;
    }
}
