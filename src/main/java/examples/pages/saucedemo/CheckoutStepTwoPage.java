package examples.demo.pages.saucedemo;

import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;

public interface CheckoutStepTwoPage {

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

    // ── OrderSummary ── review section with nested sub-groups ─────────────────

    interface OrderSummary {

        // Cart items being purchased (%s = item index)
        interface CartItem {

            enum Buttons implements Clickable {
                ITEM_TITLE_LINK;
            }

            enum Labels implements ReadOnly {
                QUANTITY,
                ITEM_PRICE;
            }
        }

        // Payment method line
        enum PaymentInfo implements ReadOnly {
            LABEL,
            VALUE;
        }

        // Shipping method line
        enum ShippingInfo implements ReadOnly {
            LABEL,
            VALUE;
        }

        // Price breakdown
        enum PriceTotal implements ReadOnly {
            SUBTOTAL_LABEL,
            TAX_LABEL,
            TOTAL_LABEL;
        }
    }

    // ── Actions ── page-level navigation buttons ──────────────────────────────

    enum Actions implements Clickable {
        CANCEL_BUTTON,
        FINISH_BUTTON;
    }

    // ── Footer ── social links ────────────────────────────────────────────────

    enum Footer implements Clickable {
        TWITTER_LINK,
        FACEBOOK_LINK,
        LINKEDIN_LINK;
    }
}
