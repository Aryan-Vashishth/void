package core.engine.selenium;

import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.locator.LocatorStrategy;
import domain.automation.web.selenium.SeleniumEngine;

import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static org.testng.Assert.*;

/**
 * Unit tests for the two private static helper methods in {@link SeleniumEngine}:
 *
 * <ul>
 *   <li>{@code labelFor(LocatorDescriptor)} — picks a display label with priority:
 *       label > args[0] > value</li>
 *   <li>{@code clickLabel(String, LocatorDescriptor)} — composes the click log message:
 *       "Clicked: visibleText | label" or "Clicked: label" when text is blank</li>
 * </ul>
 *
 * Both methods are private so they are accessed via reflection. This approach is
 * intentional: the helpers encapsulate the label-selection logic that every interaction
 * method in SeleniumEngine depends on, and that logic must be tested in isolation
 * because integration tests (which require a live WebDriver) cannot cover edge cases
 * like null args, blank visible text, or label-vs-value fallback.
 */
public class SeleniumEngineLabelHelperTest {

    // ── Reflection handles ───────────────────────────────────────────────────

    /** Invoke {@code SeleniumEngine.labelFor(LocatorDescriptor)} via reflection. */
    private static String labelFor(LocatorDescriptor locator) throws Exception {
        Method m = SeleniumEngine.class.getDeclaredMethod("labelFor", LocatorDescriptor.class);
        m.setAccessible(true);
        return (String) m.invoke(null, locator);
    }

    /** Invoke {@code SeleniumEngine.clickLabel(String, LocatorDescriptor)} via reflection. */
    private static String clickLabel(String visibleText, LocatorDescriptor locator) throws Exception {
        Method m = SeleniumEngine.class.getDeclaredMethod(
                "clickLabel", String.class, LocatorDescriptor.class);
        m.setAccessible(true);
        return (String) m.invoke(null, visibleText, locator);
    }

    // ── labelFor: label takes priority over everything ───────────────────────

    @Test(description = "labelFor returns descriptor.label() when label is set — highest priority")
    public void labelFor_withLabel_returnsLabel() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor("//input", LocatorStrategy.XPATH, new Object[]{"arg0"})
                .withLabel("DemoLoginPage > Credentials > USERNAME_INPUT");

        assertEquals(labelFor(d), "DemoLoginPage > Credentials > USERNAME_INPUT");
    }

    @Test(description = "labelFor returns descriptor.label() even when args are also present")
    public void labelFor_labelAndArgs_labelWins() throws Exception {
        // Both label and args are set; label must win
        LocatorDescriptor d = new LocatorDescriptor(
                "//input", LocatorStrategy.XPATH,
                new Object[]{"first-arg"},
                null,
                "Page > Enum > FIELD");

        assertEquals(labelFor(d), "Page > Enum > FIELD");
    }

    // ── labelFor: args[0] fallback when label is null ────────────────────────

    @Test(description = "labelFor returns args[0] as string when label is null and args are present")
    public void labelFor_noLabel_withArgs_returnsFirstArg() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor(
                "//tr[%s]//td", LocatorStrategy.XPATH, new Object[]{"Laptop"});

        assertEquals(labelFor(d), "Laptop");
    }

    @Test(description = "labelFor returns String.valueOf(args[0]) — handles non-String arg types")
    public void labelFor_noLabel_intArg_returnsStringified() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor(
                "//tr[%s]", LocatorStrategy.XPATH, new Object[]{42});

        assertEquals(labelFor(d), "42");
    }

    @Test(description = "labelFor returns args[0] only — not args[1] or later elements")
    public void labelFor_noLabel_multipleArgs_returnsFirstOnly() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor(
                "//td[@r='%s'][@c='%s']", LocatorStrategy.XPATH, new Object[]{"row1", "col2"});

        assertEquals(labelFor(d), "row1");
    }

    // ── labelFor: value fallback when both label and args are absent ──────────

    @Test(description = "labelFor returns the locator value when label is null and args are empty")
    public void labelFor_noLabel_noArgs_returnsValue() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor("//button[@id='submit']", LocatorStrategy.XPATH);

        assertEquals(labelFor(d), "//button[@id='submit']");
    }

    @Test(description = "labelFor returns the locator value when label is null and args array is null")
    public void labelFor_noLabel_nullArgs_returnsValue() throws Exception {
        // Canonical constructor — pass null args explicitly
        LocatorDescriptor d = new LocatorDescriptor(
                "css=.submit-btn", LocatorStrategy.CSS, null, null, null);

        assertEquals(labelFor(d), "css=.submit-btn");
    }

    @Test(description = "labelFor returns value for a CSS locator (strategy-independent)")
    public void labelFor_noLabel_cssStrategy_returnsValue() throws Exception {
        LocatorDescriptor d = LocatorDescriptor.of(".nav-link", LocatorStrategy.CSS);

        assertEquals(labelFor(d), ".nav-link");
    }

    // ── clickLabel: composes click message ────────────────────────────────────

    @Test(description = "clickLabel includes visible text when text is non-blank")
    public void clickLabel_withText_includesTextAndLabel() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor("//button", LocatorStrategy.XPATH)
                .withLabel("DemoLoginPage > Buttons > LOGIN_BUTTON");

        String result = clickLabel("Login", d);

        assertEquals(result, "Clicked: Login | DemoLoginPage > Buttons > LOGIN_BUTTON");
    }

    @Test(description = "clickLabel omits visible text segment when text is blank")
    public void clickLabel_blankText_omitsTextSegment() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor("//button", LocatorStrategy.XPATH)
                .withLabel("Page > Group > BUTTON");

        String result = clickLabel("", d);

        assertEquals(result, "Clicked: Page > Group > BUTTON");
    }

    @Test(description = "clickLabel omits visible text segment when text is null")
    public void clickLabel_nullText_omitsTextSegment() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor("//button", LocatorStrategy.XPATH)
                .withLabel("Page > Group > BUTTON");

        String result = clickLabel(null, d);

        assertEquals(result, "Clicked: Page > Group > BUTTON");
    }

    @Test(description = "clickLabel omits visible text segment when text is only whitespace")
    public void clickLabel_whitespaceText_omitsTextSegment() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor("//button", LocatorStrategy.XPATH)
                .withLabel("Page > Group > BUTTON");

        String result = clickLabel("   ", d);

        assertEquals(result, "Clicked: Page > Group > BUTTON");
    }

    @Test(description = "clickLabel falls back to locator value when no label and no args are set")
    public void clickLabel_noLabel_noArgs_fallsBackToValue() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor("//div[@id='overlay']", LocatorStrategy.XPATH);

        String result = clickLabel("", d);

        assertEquals(result, "Clicked: //div[@id='overlay']");
    }

    @Test(description = "clickLabel falls back to args[0] when label is null but args are present")
    public void clickLabel_noLabel_withArgs_usesFirstArg() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor(
                "//tr[%s]//button", LocatorStrategy.XPATH, new Object[]{"Laptop"});

        // blank text → only label; label falls back to args[0]
        String result = clickLabel("", d);

        assertEquals(result, "Clicked: Laptop");
    }

    @Test(description = "clickLabel produces correct format for real-world login scenario")
    public void clickLabel_loginScenario_fullFormat() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor("//button[@id='login-btn']", LocatorStrategy.XPATH)
                .withLabel("DemoLoginPage > Buttons > LOGIN_BUTTON");

        // Element has visible text "Login" on the page
        String result = clickLabel("Login", d);

        assertEquals(result, "Clicked: Login | DemoLoginPage > Buttons > LOGIN_BUTTON");
    }

    @Test(description = "clickLabel with visible text and args-fallback label")
    public void clickLabel_withTextAndArgsFallback() throws Exception {
        LocatorDescriptor d = new LocatorDescriptor(
                "//button[text()='%s']", LocatorStrategy.XPATH, new Object[]{"Submit"});

        // label is null → falls back to args[0] = "Submit"
        String result = clickLabel("Submit", d);

        assertEquals(result, "Clicked: Submit | Submit");
    }
}
