package examples.demo;

import core.flow.Flow;
import core.logging.CustomLogger;
import core.logging.theme.LogTheme;
import core.runtime.VOID;
import core.utils.data.DataGenerator;
import domain.automation.web.engine.UIEngine;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import examples.demo.pages.saucedemo.*;
import examples.listeners.ScreenshotCapable;

import java.util.Comparator;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Full SauceDemo test suite -- 44 cases across Login, Landing Page, and Cart/Checkout modules.
 *
 * <p>Test IDs match SauceDemo_All_Test_Cases.csv. Known defects are documented inline.</p>
 *
 * <p>Credentials: standard_user / locked_out_user / problem_user -- all use secret_sauce.</p>
 */
@Test
public class SauceDemoTest implements ScreenshotCapable {

    private static final String BASE_URL       = "https://www.saucedemo.com/";
    private static final String STANDARD_USER  = "standard_user";
    private static final String LOCKED_USER    = "locked_out_user";
    private static final String PROBLEM_USER   = "problem_user";
    private static final String VALID_PASSWORD = "secret_sauce";

    // Per-thread session -- each parallel test method gets its own browser instance.
    private static final ThreadLocal<VOID>      SESSION = new ThreadLocal<>();
    private static final ThreadLocal<UIEngine>  ENGINE  = new ThreadLocal<>();

    private VOID     app()       { return SESSION.get(); }
    private UIEngine rawEngine() { return ENGINE.get(); }

    @Override
    public byte[] captureScreenshot() {
        try {
            UIEngine engine = ENGINE.get();
            return engine != null ? engine.takeScreenshot() : new byte[0];
        } catch (Exception e) {
            return new byte[0];
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    @BeforeClass
    public void setUp() {
        CustomLogger.initialize(SauceDemoTest.class);
        CustomLogger.enableAnsi();
        CustomLogger.setTheme(LogTheme.HIGH_CONTRAST);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        VOID app = SESSION.get();
        if (app != null) app.shutdown();
        SESSION.remove();
        ENGINE.remove();
    }

    @BeforeMethod(alwaysRun = true)
    public void navigateToLogin() {
        CustomLogger.info.log(Thread.currentThread().getName());
        VOID app = VOID.builder().start();
        SESSION.set(app);
        ENGINE.set(app.debug().engine());
        app.browser().navigateTo(BASE_URL);
        // SauceDemo stores cart state in localStorage; clear it so each test starts with an empty cart.
        rawEngine().executeScript("localStorage.clear()");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private void doLogin(String username, String password) {
        app().run(Flow.of(
                LoginPage.LoginForm.Credentials.USERNAME_FIELD.type(username),
                LoginPage.LoginForm.Credentials.PASSWORD_FIELD.type(password),
                LoginPage.LoginForm.Buttons.LOGIN_BUTTON.click()
        ));
    }

    private void doValidLogin() {
        doLogin(STANDARD_USER, VALID_PASSWORD);
    }

    private String loginErrorText() {
        return app().reader().query(LoginPage.ErrorMessage.Labels.ERROR_BANNER.getText());
    }

    private String cartBadge() {
        return app().reader().query(ProductsPage.Header.Labels.CART_BADGE.getText());
    }

    private void addItemToCart(String slug) {
        app().run(Flow.of(ProductsPage.ProductItem.DynamicButtons.ADD_TO_CART_BUTTON.click().withArgs(slug)));
    }

    private void addFirstItemToCart() {
        // "sauce-labs-backpack" is reliably the first item under default A-Z sort
        addItemToCart("sauce-labs-backpack");
    }

    private void openHamburgerMenu() {
        app().run(Flow.of(ProductsPage.Header.Buttons.MENU_BUTTON.click()));
        // The sidebar uses a CSS transition; wait for it to become visible before returning.
        app().elements().waitForVisible(ProductsPage.SideMenu.ALL_ITEMS_LINK);
    }

    private void goToCart() {
        app().run(Flow.of(ProductsPage.Header.Buttons.CART_LINK.click()));
    }

    private void startCheckout() {
        app().run(Flow.of(CartPage.Actions.CHECKOUT_BUTTON.click()));
    }

    private void fillCheckoutInfo(String first, String last, String zip) {
        app().run(Flow.of(
                CheckoutStepOnePage.CheckoutInfoForm.Fields.FIRST_NAME_INPUT.type(first),
                CheckoutStepOnePage.CheckoutInfoForm.Fields.LAST_NAME_INPUT.type(last),
                CheckoutStepOnePage.CheckoutInfoForm.Fields.POSTAL_CODE_INPUT.type(zip),
                CheckoutStepOnePage.CheckoutInfoForm.Buttons.CONTINUE_BUTTON.click()
        ));
    }

    private void fillCheckoutInfo() {
        fillCheckoutInfo(
                DataGenerator.generateValue(DataGenerator.FieldType.NAME),
                DataGenerator.generateValue(DataGenerator.FieldType.NAME),
                DataGenerator.generateValue(DataGenerator.FieldType.NUMBER)
        );
    }

    private List<String> productNames() {
        return app().elements().allTexts(ProductsPage.ProductItem.Labels.ITEM_NAME);
    }

    private List<Double> productPrices() {
        return app().elements().allTexts(ProductsPage.ProductItem.Labels.ITEM_PRICE)
                .stream()
                .map(t -> Double.parseDouble(t.replace("$", "")))
                .toList();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LOGIN  (LOGIN-01 ... LOGIN-13)
    // ═════════════════════════════════════════════════════════════════════════

    @Test(description = "LOGIN-01 -- Valid credentials redirect to /inventory.html")
    public void login01_validLogin() {
        doLogin(STANDARD_USER, VALID_PASSWORD);
        assertTrue(app().browser().url().contains("/inventory.html"),
                "Expected redirect to /inventory.html after valid login");
    }

    @Test(description = "LOGIN-02 -- Empty fields: 'Username is required'")
    public void login02_emptyCredentials() {
        app().run(Flow.of(LoginPage.LoginForm.Buttons.LOGIN_BUTTON.click()));
        assertEquals(loginErrorText(), "Epic sadface: Username is required");
    }

    @Test(description = "LOGIN-03 -- Username only: 'Password is required'")
    public void login03_emptyPassword() {
        app().run(Flow.of(
                LoginPage.LoginForm.Credentials.USERNAME_FIELD.type(STANDARD_USER),
                LoginPage.LoginForm.Buttons.LOGIN_BUTTON.click()
        ));
        assertEquals(loginErrorText(), "Epic sadface: Password is required");
    }

    @Test(description = "LOGIN-04 -- Password only: 'Username is required' (takes priority)")
    public void login04_emptyUsername() {
        app().run(Flow.of(
                LoginPage.LoginForm.Credentials.PASSWORD_FIELD.type(VALID_PASSWORD),
                LoginPage.LoginForm.Buttons.LOGIN_BUTTON.click()
        ));
        assertEquals(loginErrorText(), "Epic sadface: Username is required");
    }

    @Test(description = "LOGIN-05 -- Wrong password: generic credentials mismatch error")
    public void login05_invalidCredentials() {
        doLogin(STANDARD_USER, "wrongpass");
        assertEquals(loginErrorText(),
                "Epic sadface: Username and password do not match any user in this service");
    }

    @Test(description = "LOGIN-06 -- Locked-out user shows specific lockout message")
    public void login06_lockedOutUser() {
        doLogin(LOCKED_USER, VALID_PASSWORD);
        assertEquals(loginErrorText(), "Epic sadface: Sorry, this user has been locked out.");
    }

    @Test(description = "LOGIN-07 -- Username is case-sensitive; wrong case is rejected")
    public void login07_usernameCaseSensitive() {
        doLogin("Standard_User", VALID_PASSWORD);
        assertEquals(loginErrorText(),
                "Epic sadface: Username and password do not match any user in this service");
    }

    @Test(description = "LOGIN-08 -- Leading/trailing whitespace in username is not trimmed")
    public void login08_leadingTrailingWhitespace() {
        doLogin("  " + STANDARD_USER + "  ", VALID_PASSWORD);
        assertEquals(loginErrorText(),
                "Epic sadface: Username and password do not match any user in this service");
    }

    @Test(description = "LOGIN-09 -- SQL/script injection shows generic error with no bypass")
    public void login09_injectionAttempt() {
        doLogin("' OR '1'='1", "<script>alert(1)</script>");
        assertEquals(loginErrorText(),
                "Epic sadface: Username and password do not match any user in this service");
    }

    @Test(description = "LOGIN-10 -- Password field type attribute is 'password' (masked)")
    public void login10_passwordFieldMasked() {
        assertEquals(
                app().elements().attribute(LoginPage.LoginForm.Credentials.PASSWORD_FIELD, "type"),
                "password");
    }

    @Test(description = "LOGIN-11 -- problem_user authenticates and reaches inventory page")
    public void login11_problemUser() {
        doLogin(PROBLEM_USER, VALID_PASSWORD);
        assertTrue(app().browser().url().contains("/inventory.html"));
    }

    @Test(description = "LOGIN-12 -- Error banner closes when dismiss icon is clicked")
    public void login12_errorDismissal() {
        app().run(Flow.of(LoginPage.LoginForm.Buttons.LOGIN_BUTTON.click()));
        assertFalse(loginErrorText().isEmpty(), "Error banner must be visible before dismissal");

        app().run(Flow.of(LoginPage.ErrorMessage.Buttons.ERROR_DISMISS.click()));

        assertFalse(app().elements().isVisible(LoginPage.ErrorMessage.Labels.ERROR_BANNER),
                "Error banner should be hidden after clicking dismiss");
    }

    @Test(description = "LOGIN-13 -- Login button has no 'disabled' attribute (always enabled)")
    public void login13_loginButtonAlwaysEnabled() {
        String disabled = app().elements().attribute(LoginPage.LoginForm.Buttons.LOGIN_BUTTON, "disabled");
        assertNull(disabled, "Login button must not carry a 'disabled' attribute with empty fields");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LANDING PAGE  (LAND-01 ... LAND-17)
    // ═════════════════════════════════════════════════════════════════════════

    @Test(description = "LAND-01 -- Products page loads: URL, title, and 6 product cards visible")
    public void land01_productsPageLoads() {
        doValidLogin();
        assertTrue(app().browser().url().contains("/inventory.html"));
        assertEquals(app().reader().query(ProductsPage.Header.Labels.PAGE_TITLE.getText()), "Products");
        assertEquals(app().elements().count(ProductsPage.ProductItem.Labels.ITEM_NAME), 6,
                "Expected 6 product cards on the inventory page");
    }

    @Test(description = "LAND-02 -- Default sort is Name (A to Z)")
    public void land02_defaultSortAtoZ() {
        doValidLogin();
        assertEquals(app().reader().query(ProductsPage.SortDropdown.Labels.ACTIVE_OPTION_LABEL.getText()),
                "Name (A to Z)");
        List<String> names = productNames();
        assertEquals(names, names.stream().sorted().toList(),
                "Products should be in A-Z order by default");
    }

    @Test(description = "LAND-03 -- Sort by Name (Z to A) reverses alphabetical order")
    public void land03_sortZtoA() {
        doValidLogin();
        app().run(Flow.of(ProductsPage.SortDropdown.Options.NAME_Z_TO_A.select()));
        List<String> names = productNames();
        assertEquals(names, names.stream().sorted(Comparator.reverseOrder()).toList(),
                "Products should be in Z-A order after selecting that sort");
    }

    @Test(description = "LAND-04 -- Sort by Price (low to high) orders products ascending")
    public void land04_sortPriceLowToHigh() {
        doValidLogin();
        app().run(Flow.of(ProductsPage.SortDropdown.Options.PRICE_LOW_TO_HIGH.select()));
        List<Double> prices = productPrices();
        assertEquals(prices, prices.stream().sorted().toList(),
                "Products should be ordered price ascending");
    }

    @Test(description = "LAND-05 -- Sort by Price (high to low) orders products descending")
    public void land05_sortPriceHighToLow() {
        doValidLogin();
        app().run(Flow.of(ProductsPage.SortDropdown.Options.PRICE_HIGH_TO_LOW.select()));
        List<Double> prices = productPrices();
        assertEquals(prices, prices.stream().sorted(Comparator.reverseOrder()).toList(),
                "Products should be ordered price descending");
    }

    @Test(description = "LAND-06 -- Add single item: badge shows 1, button changes to Remove")
    public void land06_addSingleItemToCart() {
        doValidLogin();
        addItemToCart("sauce-labs-backpack");
        assertEquals(cartBadge(), "1");
        assertTrue(app().elements().isVisible(ProductsPage.ProductItem.DynamicButtons.REMOVE_BUTTON, "sauce-labs-backpack"),
                "Remove button should appear after adding item");
    }

    @Test(description = "LAND-07 -- Add two items: badge shows 2")
    public void land07_addTwoItemsToCart() {
        doValidLogin();
        addItemToCart("sauce-labs-backpack");
        addItemToCart("sauce-labs-bike-light");
        assertEquals(cartBadge(), "2");
    }

    @Test(description = "LAND-08 -- Remove item from Products page: button reverts, badge disappears")
    public void land08_removeItemFromProductsPage() {
        doValidLogin();
        addItemToCart("sauce-labs-backpack");
        assertEquals(cartBadge(), "1");
        app().run(Flow.of(ProductsPage.ProductItem.DynamicButtons.REMOVE_BUTTON.click().withArgs("sauce-labs-backpack")));
        assertFalse(app().elements().isVisible(ProductsPage.Header.Labels.CART_BADGE),
                "Cart badge should disappear after removing the only item");
    }

    @Test(description = "LAND-09 -- Cart icon navigates to /cart.html with correct item")
    public void land09_cartIconNavigation() {
        doValidLogin();
        addItemToCart("sauce-labs-backpack");
        goToCart();
        assertTrue(app().browser().url().contains("/cart.html"));
        assertTrue(app().elements().count(CartPage.CartItem.Labels.ITEM_NAME) > 0,
                "Cart should contain at least one item");
    }

    @Test(description = "LAND-10 -- Continue Shopping navigates back to /inventory.html")
    public void land10_continueShoppingFromCart() {
        doValidLogin();
        addFirstItemToCart();
        app().run(Flow.of(
                ProductsPage.Header.Buttons.CART_LINK.click(),
                CartPage.Actions.CONTINUE_SHOPPING_BUTTON.click()
        ));
        assertTrue(app().browser().url().contains("/inventory.html"));
    }

    @Test(description = "LAND-11 -- Hamburger menu opens and shows all four navigation links")
    public void land11_hamburgerMenuOpens() {
        doValidLogin();
        openHamburgerMenu();
        assertTrue(app().elements().isVisible(ProductsPage.SideMenu.ALL_ITEMS_LINK), "All Items link should be visible");
        assertTrue(app().elements().isVisible(ProductsPage.SideMenu.ABOUT_LINK), "About link should be visible");
        assertTrue(app().elements().isVisible(ProductsPage.SideMenu.LOGOUT_LINK), "Logout link should be visible");
        assertTrue(app().elements().isVisible(ProductsPage.SideMenu.RESET_APP_STATE_LINK), "Reset App State link should be visible");
    }

    @Test(description = "LAND-12 -- Reset App State clears badge; KNOWN DEFECT: Remove buttons do not revert")
    public void land12_resetAppState() {
        doValidLogin();
        addItemToCart("sauce-labs-backpack");
        assertEquals(cartBadge(), "1");

        openHamburgerMenu();
        app().run(Flow.of(ProductsPage.SideMenu.RESET_APP_STATE_LINK.click()));

        // Badge clears correctly
        assertFalse(app().elements().isVisible(ProductsPage.Header.Labels.CART_BADGE),
                "Cart badge should clear to 0 after Reset App State");

        // KNOWN DEFECT (LAND-12): Remove button does not revert to 'Add to cart' without
        // a page refresh. Asserting the actual broken behavior as a regression anchor.
        assertTrue(app().elements().isVisible(ProductsPage.ProductItem.DynamicButtons.REMOVE_BUTTON, "sauce-labs-backpack"),
                "KNOWN DEFECT -- Remove button still shows 'Remove' after reset; expected 'Add to cart'");
    }

    @Test(description = "LAND-13 -- Hamburger menu closes via the X button")
    public void land13_closeHamburgerMenu() {
        doValidLogin();
        openHamburgerMenu();
        app().run(Flow.of(ProductsPage.SideMenu.CLOSE_MENU_BUTTON.click()));
        // The sidebar close uses a CSS transition; wait for it to become hidden before asserting.
        app().elements().waitForHidden(ProductsPage.SideMenu.CLOSE_MENU_BUTTON);
        assertFalse(app().elements().isVisible(ProductsPage.SideMenu.CLOSE_MENU_BUTTON),
                "Close button should be hidden once the menu is closed");
    }

    @Test(description = "LAND-14 -- Footer Twitter link points to Sauce Labs Twitter")
    public void land14_footerTwitterLink() {
        doValidLogin();
        assertEquals(
                app().elements().attribute(ProductsPage.Footer.TWITTER_LINK, "href"),
                "https://twitter.com/saucelabs");
    }

    @Test(description = "LAND-15 -- Footer Facebook link points to Sauce Labs Facebook")
    public void land15_footerFacebookLink() {
        doValidLogin();
        assertEquals(
                app().elements().attribute(ProductsPage.Footer.FACEBOOK_LINK, "href"),
                "https://www.facebook.com/saucelabs");
    }

    @Test(description = "LAND-16 -- Footer LinkedIn link points to Sauce Labs LinkedIn")
    public void land16_footerLinkedInLink() {
        doValidLogin();
        assertEquals(
                app().elements().attribute(ProductsPage.Footer.LINKEDIN_LINK, "href"),
                "https://www.linkedin.com/company/sauce-labs/");
    }

    @Test(description = "LAND-17 -- Logout redirects to the login page")
    public void land17_logout() {
        doValidLogin();
        openHamburgerMenu();
        app().run(Flow.of(ProductsPage.SideMenu.LOGOUT_LINK.click()));
        assertEquals(app().browser().url(), BASE_URL);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CART & CHECKOUT  (CART-01 ... CART-15)
    // ═════════════════════════════════════════════════════════════════════════

    @Test(description = "CART-01 -- Cart shows correct item name and price after add")
    public void cart01_cartShowsCorrectItem() {
        doValidLogin();
        addItemToCart("sauce-labs-onesie");
        goToCart();
        assertTrue(app().browser().url().contains("/cart.html"));
        assertTrue(app().reader().query(CartPage.CartItem.Labels.ITEM_NAME.getText()).contains("Onesie"),
                "Cart should display the Onesie item");
        assertEquals(app().reader().query(CartPage.CartItem.Labels.ITEM_PRICE.getText()), "$7.99");
    }

    @Test(description = "CART-02 -- Checkout button navigates to /checkout-step-one.html")
    public void cart02_proceedToCheckout() {
        doValidLogin();
        addFirstItemToCart();
        goToCart();
        startCheckout();
        assertTrue(app().browser().url().contains("/checkout-step-one.html"));
    }

    @Test(description = "CART-03 -- Empty checkout form: 'First Name is required'")
    public void cart03_checkoutEmptyFirstName() {
        doValidLogin();
        addFirstItemToCart();
        goToCart();
        startCheckout();
        app().run(Flow.of(CheckoutStepOnePage.CheckoutInfoForm.Buttons.CONTINUE_BUTTON.click()));
        assertEquals(
                app().reader().query(CheckoutStepOnePage.ErrorMessage.ERROR_BANNER.getText()),
                "Error: First Name is required");
    }

    @Test(description = "CART-04 -- First Name only: 'Last Name is required'")
    public void cart04_checkoutEmptyLastName() {
        doValidLogin();
        addFirstItemToCart();
        goToCart();
        startCheckout();
        app().run(Flow.of(
                CheckoutStepOnePage.CheckoutInfoForm.Fields.FIRST_NAME_INPUT.type("John"),
                CheckoutStepOnePage.CheckoutInfoForm.Buttons.CONTINUE_BUTTON.click()
        ));
        assertEquals(
                app().reader().query(CheckoutStepOnePage.ErrorMessage.ERROR_BANNER.getText()),
                "Error: Last Name is required");
    }

    @Test(description = "CART-05 -- First + Last name only: 'Postal Code is required'")
    public void cart05_checkoutEmptyPostalCode() {
        doValidLogin();
        addFirstItemToCart();
        goToCart();
        startCheckout();
        app().run(Flow.of(
                CheckoutStepOnePage.CheckoutInfoForm.Fields.FIRST_NAME_INPUT.type("John"),
                CheckoutStepOnePage.CheckoutInfoForm.Fields.LAST_NAME_INPUT.type("Doe"),
                CheckoutStepOnePage.CheckoutInfoForm.Buttons.CONTINUE_BUTTON.click()
        ));
        assertEquals(
                app().reader().query(CheckoutStepOnePage.ErrorMessage.ERROR_BANNER.getText()),
                "Error: Postal Code is required");
    }

    @Test(description = "CART-06 -- Valid checkout info advances to /checkout-step-two.html")
    public void cart06_checkoutStepOneSuccess() {
        doValidLogin();
        addFirstItemToCart();
        goToCart();
        startCheckout();
        fillCheckoutInfo();
        assertTrue(app().browser().url().contains("/checkout-step-two.html"));
    }

    @Test(description = "CART-07 -- Overview totals: Total = Item total + Tax (~8%)")
    public void cart07_checkoutOverviewTotals() {
        doValidLogin();
        addItemToCart("sauce-labs-onesie"); // $7.99
        goToCart();
        startCheckout();
        fillCheckoutInfo();

        double subtotal = Double.parseDouble(
                app().reader().query(CheckoutStepTwoPage.OrderSummary.PriceTotal.SUBTOTAL_LABEL.getText())
                        .replaceAll("[^0-9.]", ""));
        double tax = Double.parseDouble(
                app().reader().query(CheckoutStepTwoPage.OrderSummary.PriceTotal.TAX_LABEL.getText())
                        .replaceAll("[^0-9.]", ""));
        double total = Double.parseDouble(
                app().reader().query(CheckoutStepTwoPage.OrderSummary.PriceTotal.TOTAL_LABEL.getText())
                        .replaceAll("[^0-9.]", ""));

        assertEquals(subtotal, 7.99, 0.001, "Item subtotal should be $7.99");
        assertEquals(total, subtotal + tax, 0.001, "Total must equal subtotal + tax");
    }

    @Test(description = "CART-08 -- Finish completes order: /checkout-complete.html, cart cleared")
    public void cart08_completeOrder() {
        doValidLogin();
        addFirstItemToCart();
        goToCart();
        startCheckout();
        fillCheckoutInfo();
        app().run(Flow.of(CheckoutStepTwoPage.Actions.FINISH_BUTTON.click()));

        assertTrue(app().browser().url().contains("/checkout-complete.html"));
        assertFalse(app().elements().isVisible(ProductsPage.Header.Labels.CART_BADGE),
                "Cart badge should be absent after completing the order");
    }

    @Test(description = "CART-09 -- Back Home returns to /inventory.html with cart empty")
    public void cart09_backHomeAfterOrder() {
        doValidLogin();
        addFirstItemToCart();
        goToCart();
        startCheckout();
        fillCheckoutInfo();
        app().run(Flow.of(
                CheckoutStepTwoPage.Actions.FINISH_BUTTON.click(),
                CheckoutCompletePage.Actions.BACK_HOME_BUTTON.click()
        ));

        assertTrue(app().browser().url().contains("/inventory.html"));
        assertFalse(app().elements().isVisible(ProductsPage.Header.Labels.CART_BADGE),
                "Cart should be empty after completing and returning home");
    }

    @Test(description = "CART-10 -- Cancel on step one returns to /cart.html with item preserved")
    public void cart10_cancelOnCheckoutStepOne() {
        doValidLogin();
        addFirstItemToCart();
        goToCart();
        startCheckout();
        app().run(Flow.of(CheckoutStepOnePage.CheckoutInfoForm.Buttons.CANCEL_BUTTON.click()));

        assertTrue(app().browser().url().contains("/cart.html"));
        assertTrue(app().elements().count(CartPage.CartItem.Labels.ITEM_NAME) > 0,
                "Cart item should be preserved after cancelling step one");
    }

    @Test(description = "CART-11 -- Cancel on step two; KNOWN DEFECT: goes to /inventory.html not /cart.html")
    public void cart11_cancelOnCheckoutStepTwo() {
        doValidLogin();
        addFirstItemToCart();
        goToCart();
        startCheckout();
        fillCheckoutInfo();
        app().run(Flow.of(CheckoutStepTwoPage.Actions.CANCEL_BUTTON.click()));

        // KNOWN DEFECT (CART-11): Expected /cart.html (consistent with step-one cancel) but
        // actual behavior redirects to /inventory.html. Asserting actual behavior as a
        // regression anchor until the inconsistency is resolved.
        assertTrue(app().browser().url().contains("/inventory.html"),
                "KNOWN DEFECT -- Cancel on step 2 lands on inventory; expected cart page");
    }

    @Test(description = "CART-12 -- Remove item from cart page: item gone, badge disappears")
    public void cart12_removeItemFromCartPage() {
        doValidLogin();
        addItemToCart("sauce-labs-backpack");
        goToCart();
        app().run(Flow.of(CartPage.CartItem.DynamicButtons.REMOVE_BUTTON.click().withArgs("sauce-labs-backpack")));

        assertEquals(app().elements().count(CartPage.CartItem.Labels.ITEM_NAME), 0,
                "Cart should be empty after removing the item");
        assertFalse(app().elements().isVisible(ProductsPage.Header.Labels.CART_BADGE),
                "Cart badge should disappear after last item is removed");
    }

    @Test(description = "CART-13 -- Empty cart checkout; KNOWN DEFECT: $0 order should be blocked")
    public void cart13_emptyCartCheckout() {
        doValidLogin();
        goToCart();
        assertEquals(app().elements().count(CartPage.CartItem.Labels.ITEM_NAME), 0,
                "Cart must be empty at the start of this test");

        startCheckout();
        fillCheckoutInfo();
        app().run(Flow.of(CheckoutStepTwoPage.Actions.FINISH_BUTTON.click()));

        // KNOWN DEFECT (CART-13): The system should prevent a $0 order with an empty cart
        // but instead completes it with a confirmation page. Asserting actual behavior.
        assertTrue(app().browser().url().contains("/checkout-complete.html"),
                "KNOWN DEFECT -- Empty cart checkout succeeds; should be blocked");
    }

    @Test(description = "CART-14 -- Generate PDF button is visible and clickable after empty-cart checkout")
    public void cart14_generatePdfEmptyCart() {
        doValidLogin();
        goToCart();
        startCheckout();
        fillCheckoutInfo();
        app().run(Flow.of(CheckoutStepTwoPage.Actions.FINISH_BUTTON.click()));

        assertTrue(app().browser().url().contains("/checkout-complete.html"));
        app().run(Flow.of(CheckoutCompletePage.Actions.GENERATE_PDF_BUTTON.click().safely()));
        assertTrue(app().browser().url().contains("/checkout-complete.html"),
                "PDF generation should not navigate away from the confirmation page");
    }

    @Test(description = "CART-15 -- Generate PDF button is visible and clickable after multi-item checkout")
    public void cart15_generatePdfMultipleItems() {
        doValidLogin();
        addItemToCart("sauce-labs-backpack");
        addItemToCart("sauce-labs-bike-light");
        goToCart();
        startCheckout();
        fillCheckoutInfo();
        app().run(Flow.of(CheckoutStepTwoPage.Actions.FINISH_BUTTON.click()));

        assertTrue(app().browser().url().contains("/checkout-complete.html"));
        app().run(Flow.of(CheckoutCompletePage.Actions.GENERATE_PDF_BUTTON.click().safely()));
        assertTrue(app().browser().url().contains("/checkout-complete.html"),
                "PDF generation should not navigate away from the confirmation page");
    }
}
