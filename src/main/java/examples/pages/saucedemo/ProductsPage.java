package examples.pages.saucedemo;

import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.Dropdown;
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

    // ── SortDropdown ── sort options + active label ───────────────────────────

    interface SortDropdown {

        /**
         * Each constant IS a selectable sort option. The locator points to the {@code <select>}
         * container; {@link #getArgs()} provides the visible text for
         * {@link domain.automation.web.engine.UIEngine#selectByVisibleText}.
         */
        enum Options implements Dropdown {
            NAME_A_TO_Z("Name (A to Z)"),
            NAME_Z_TO_A("Name (Z to A)"),
            PRICE_LOW_TO_HIGH("Price (low to high)"),
            PRICE_HIGH_TO_LOW("Price (high to low)");

            private final String label;

            Options(String label) { this.label = label; }

            @Override
            public Object[] getArgs() { return new Object[]{label}; }
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
