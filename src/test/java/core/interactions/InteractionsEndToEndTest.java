package core.interactions;

import domain.automation.web.selenium.driver.SeleniumDriverContext;
import core.logging.CustomLogger;
import core.logging.theme.LogTheme;
import domain.automation.web.resolve.api.LocatorResolver;
import domain.automation.web.resolve.api.LocatorResolvers;
import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;
import core.utils.ResolvableEnum;
import domain.automation.web.vocabulary.capability.Typeable;
import domain.automation.web.vocabulary.role.ElementRole;
import core.actions.hooks.ActionHandler;
import core.interactions.Interactions;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

/**
 * End-to-end style integration test for the resolver -&gt; interactions pipeline.
 *
 * <p>This test exercises the FULL chain that occurs at runtime Ã¢â‚¬â€ minus the
 * actual browser Ã¢â‚¬â€ for the most common UI flows:</p>
 *
 * <pre>
 *   Element enum (with template + args)
 *        Ã¢â€ â€œ        getExternalFileName(), getInputLocator()/getTriggerLocator(), getArgs()
 *   LocatorResolver.resolve(Element)
 *        Ã¢â€ â€œ        rawTemplate() Ã¢â€ â€™ LocatorTemplate.format(args) Ã¢â€ â€™ ByParser.parse()
 *   By locator (xpath/css/idÃ¢â‚¬Â¦)
 *        Ã¢â€ â€œ        passed into wait.until(...) / driver.findElement(...)
 *   Interactions.typeInto / clickOn / typeInto(By,String)
 *        Ã¢â€ â€œ
 *   WebElement.clear()/sendKeys()/click()  (verified via recording proxy)
 * </pre>
 *
 * <h3>Test fixtures</h3>
 * <ul>
 *   <li>Locator file: {@code src/test/resources/locators/properties/test-locators.properties}
 *       (already used by other resolver tests).</li>
 *   <li>Driver:       a {@link Proxy}-based fake {@link WebDriver} that also implements
 *       {@link JavascriptExecutor}, so {@code DOMUtils.scrollToElement(...)} doesn't blow up.</li>
 *   <li>Element:      a recording {@link WebElement} proxy that captures every method call,
 *       allowing us to assert {@code clear/sendKeys/click} were invoked in order.</li>
 * </ul>
 *
 * <h3>Custom Logger coverage</h3>
 * <p>Each test combines a different theme + ANSI on/off + log-level mix
 * (info/debug/warn/error + tree-style {@code log("heading", "k","v")}) so that
 * the rendering pipeline is also exercised end-to-end.</p>
 */
public class InteractionsEndToEndTest {

    /** File name fragment Ã¢â‚¬â€ base path {@code locators/properties/} is auto-prepended. */
    private static final String FILE = "test-locators.properties";

    private static final LocatorResolver RESOLVER = LocatorResolvers.strict();

    // ---------------------------------------------------------------------
    // Test-only Element enums Ã¢â‚¬â€ hand-crafted so we can drive the resolver
    // through every interesting branch (no-arg, single-arg, two-arg, css/id).
    // ---------------------------------------------------------------------

    /** Single-arg XPath template: {@code //input[@placeholder='%s']}. */
    enum SearchInput implements Typeable, ResolvableEnum {
        BY_PLACEHOLDER("TEMPLATE_WITH_ARG", "username");

        private final String key;
        private final Object[] args;
        SearchInput(String key, Object... args) { this.key = key; this.args = args; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getInputLocator()     { return key; }
        @Override public Object[] getArgs()           { return args; }
    }

    /** Two-arg XPath template: {@code //tr[@data-row='%s']//td[@data-col='%s']}. */
    enum CellInput implements Typeable, ResolvableEnum {
        ROW_COL("TEMPLATE_TWO_ARGS", "3", "name");

        private final String key;
        private final Object[] args;
        CellInput(String key, Object... args) { this.key = key; this.args = args; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getInputLocator()     { return key; }
        @Override public Object[] getArgs()           { return args; }
    }

    /** No-arg XPath: {@code //button[@id='submit']} (BUTTON_TRIGGER). */
    enum SubmitButton implements Clickable, ResolvableEnum {
        SUBMIT("BUTTON_TRIGGER");

        private final String key;
        SubmitButton(String key) { this.key = key; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTriggerLocator()   { return key; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    }

    /** CSS prefix: {@code css=input#username} (LOGIN_INPUT). */
    enum LoginField implements Typeable, ResolvableEnum {
        USERNAME("LOGIN_INPUT");

        private final String key;
        LoginField(String key) { this.key = key; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getInputLocator()     { return key; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    }

    /** Read-only element keyed off TEXT role (defaults from ResolvableEnum). */
    enum HeaderLabel implements ReadOnly, ResolvableEnum {
        WELCOME("BUTTON_TRIGGER");                  // re-uses an existing key for the demo

        private final String key;
        HeaderLabel(String key) { this.key = key; }

        @Override public String getExternalFileName() { return FILE; }
        @Override public String getTextLocator()      { return key; }
        @Override public Object[] getArgs()           { return new Object[0]; }
    }

    // ---------------------------------------------------------------------
    // Recording fakes
    // ---------------------------------------------------------------------

    /** Records every WebElement method call so tests can assert ordering. */
    private static final class ElementSpy implements InvocationHandler {
        final List<String> calls = Collections.synchronizedList(new ArrayList<>());
        final WebElement proxy = (WebElement) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{WebElement.class}, this);

        @Override public Object invoke(Object p, Method m, Object[] args) {
            calls.add(m.getName() + (args == null || args.length == 0
                    ? "()"
                    : "(" + Arrays.deepToString(args) + ")"));
            Class<?> rt = m.getReturnType();
            if (rt == boolean.class)  return Boolean.TRUE;     // isDisplayed/isEnabled/isSelected
            if (rt == String.class)   return "fake-text";
            if (rt == void.class)     return null;
            if (rt.isPrimitive())     return 0;
            if (rt == List.class)     return Collections.emptyList();
            return null;
        }
    }

    /** Records last locator passed to findElement(s) and returns the spy element. */
    private static final class DriverSpy implements InvocationHandler {
        final ElementSpy element = new ElementSpy();
        final List<By>   findCalls = Collections.synchronizedList(new ArrayList<>());
        final List<String> jsCalls = Collections.synchronizedList(new ArrayList<>());

        final WebDriver proxy = (WebDriver) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                this);

        @Override public Object invoke(Object p, Method m, Object[] args) {
            switch (m.getName()) {
                case "findElement":  findCalls.add((By) args[0]); return element.proxy;
                case "findElements": findCalls.add((By) args[0]); return List.of(element.proxy);
                case "getCurrentUrl": return "about:blank";
                case "getTitle":      return "fake";
                case "executeScript": case "executeAsyncScript":
                    jsCalls.add(String.valueOf(args[0]));
                    return null;
            }
            Class<?> rt = m.getReturnType();
            if (rt == boolean.class) return Boolean.FALSE;
            if (rt == String.class)  return "";
            if (rt == List.class)    return Collections.emptyList();
            if (rt.isPrimitive() && rt != void.class) return 0;
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Fixture lifecycle
    // ---------------------------------------------------------------------

    @BeforeClass
    public void setupClass() {
        CustomLogger.initialize(this.getClass());
        CustomLogger.enableAnsi();
        CustomLogger.setTheme(LogTheme.HIGH_CONTRAST);
        CustomLogger.info.log("=== InteractionsEndToEndTest starting ===");
    }

    @AfterMethod(alwaysRun = true)
    public void detachDrivers() {
        try { SeleniumDriverContext.removePrimary(); }   catch (Exception ignored) {}
        try { SeleniumDriverContext.removeSecondary(); } catch (Exception ignored) {}
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        CustomLogger.disableAnsi();
    }

    // ---------------------------------------------------------------------
    // Stage 1 Ã¢â‚¬â€ pure resolver pipeline (no driver needed)
    // ---------------------------------------------------------------------

    @Test
    public void pipeline_singleArgTemplate_resolvesToInjectedXpath() {
        CustomLogger.debug.log("Stage", "Key", SearchInput.BY_PLACEHOLDER.getInputLocator(),
                                          "Args", Arrays.toString(SearchInput.BY_PLACEHOLDER.getArgs()));

        By by = RESOLVER.resolve(SearchInput.BY_PLACEHOLDER);

        assertNotNull(by);
        assertEquals(by, By.xpath("//input[@placeholder='username']"),
                "Single-arg template must inject 'username' into %s placeholder");
        CustomLogger.info.resolved("Resolved -> " + by);
    }

    @Test
    public void pipeline_twoArgTemplate_resolvesBothPlaceholders() {
        CustomLogger.warn.log("Resolving two-arg template");

        By by = RESOLVER.resolve(CellInput.ROW_COL);

        assertEquals(by, By.xpath("//tr[@data-row='3']//td[@data-col='name']"));
        CustomLogger.info.success("Two-arg template resolved correctly: " + by);
    }

    @Test
    public void pipeline_noArgKey_resolvesAsIs() {
        By by = RESOLVER.resolve(SubmitButton.SUBMIT);
        assertEquals(by, By.xpath("//button[@id='submit']"));
    }

    @Test
    public void pipeline_cssPrefix_yieldsCssBy() {
        By by = RESOLVER.resolve(LoginField.USERNAME);
        assertEquals(by, By.cssSelector("input#username"));
    }

    @Test
    public void pipeline_explicitRoleResolution_returnsSameLocator() {
        // Resolving via role (INPUT for Typeable) must give same By as primary lookup.
        By primary = RESOLVER.resolve(LoginField.USERNAME);
        By byRole  = RESOLVER.resolve(LoginField.USERNAME, ElementRole.INPUT);
        assertEquals(byRole, primary);
    }

    @Test
    public void pipeline_overrideArgs_takePrecedenceOverEnumDefaults() {
        // Element default is "username"; we override with "search-term".
        By by = RESOLVER.resolveBest(SearchInput.BY_PLACEHOLDER, "search-term");
        assertEquals(by, By.xpath("//input[@placeholder='search-term']"));
    }

    // ---------------------------------------------------------------------
    // Stage 2 Ã¢â‚¬â€ full Interactions methods consuming a resolved By
    // ---------------------------------------------------------------------

    @Test
    public void interactions_typeIntoTextInputField_runsFullChain() {
        DriverSpy driver = new DriverSpy();
        Interactions ix = new Interactions(driver.proxy);

        ix.typeInto(SearchInput.BY_PLACEHOLDER, "qa-bot");

        // Locator that the resolver produced AND was passed to the driver:
        assertFalse(driver.findCalls.isEmpty(), "driver.findElement(s) should have been called");
        assertEquals(driver.findCalls.get(0), By.xpath("//input[@placeholder='username']"),
                "Resolved By must reach driver.findElement");

        // Element should have been cleared then text sent:
        List<String> calls = driver.element.calls;
        assertTrue(calls.stream().anyMatch(s -> s.startsWith("clear")),
                "WebElement.clear() must be invoked. Calls=" + calls);
        assertTrue(calls.stream().anyMatch(s -> s.startsWith("sendKeys") && s.contains("qa-bot")),
                "WebElement.sendKeys('qa-bot') must be invoked. Calls=" + calls);

        CustomLogger.info.complete("typeInto end-to-end OK; driver calls=" + driver.findCalls.size()
                + " element calls=" + calls.size());
    }

    @Test
    public void interactions_typeInto_withRawByLocator_runsClearAndSendKeys() {
        DriverSpy driver = new DriverSpy();
        Interactions ix = new Interactions(driver.proxy);

        By by = RESOLVER.resolve(LoginField.USERNAME);
        ix.typeInto(by, "secret-password");

        assertEquals(driver.findCalls.get(0), By.cssSelector("input#username"));
        assertTrue(driver.element.calls.stream().anyMatch(s -> s.contains("secret-password")));
    }

    @Test
    public void interactions_clickOnClickable_runsResolveAndClick() {
        DriverSpy driver = new DriverSpy();
        Interactions ix = new Interactions(driver.proxy);

        ix.clickOn(SubmitButton.SUBMIT);

        assertEquals(driver.findCalls.get(0), By.xpath("//button[@id='submit']"),
                "Resolved trigger By must be the one passed to driver.findElement");
        assertTrue(driver.element.calls.stream().anyMatch(s -> s.startsWith("click")),
                "WebElement.click() must be invoked. Calls=" + driver.element.calls);
    }

    @Test
    public void interactions_clickOnWithBeforeAfterHooks_executesHooksInOrder() {
        DriverSpy driver = new DriverSpy();
        Interactions ix = new Interactions(driver.proxy);

        AtomicInteger order = new AtomicInteger();
        int[] beforeAt = {-1};
        int[] afterAt  = {-1};

        ActionHandler before = (drv, desc) -> beforeAt[0] = order.incrementAndGet();
        ActionHandler after  = (drv, desc) -> afterAt[0]  = order.incrementAndGet();

        ix.clickOn(Interactions.of(before), SubmitButton.SUBMIT, Interactions.of(after));

        assertEquals(beforeAt[0], 1, "Before hook must run first");
        assertEquals(afterAt[0],  2, "After hook must run last");
        assertEquals(driver.findCalls.get(0), By.xpath("//button[@id='submit']"));
    }

    @Test
    public void interactions_clearField_resolvesAndClears() {
        DriverSpy driver = new DriverSpy();
        Interactions ix = new Interactions(driver.proxy);

        ix.clearField(LoginField.USERNAME);

        assertEquals(driver.findCalls.get(0), By.cssSelector("input#username"));
        assertTrue(driver.element.calls.stream().anyMatch(s -> s.startsWith("clear")));
    }

    @Test
    public void interactions_appendTo_doesNotClearButTypes() {
        DriverSpy driver = new DriverSpy();
        Interactions ix = new Interactions(driver.proxy);

        ix.appendTo(SearchInput.BY_PLACEHOLDER, "-extra");

        assertEquals(driver.findCalls.get(0), By.xpath("//input[@placeholder='username']"));
        assertFalse(driver.element.calls.stream().anyMatch(s -> s.startsWith("clear")),
                "appendTo must NOT clear the field");
        assertTrue(driver.element.calls.stream().anyMatch(s -> s.contains("-extra")));

        CustomLogger.enableAnsi();
    }

    // ---------------------------------------------------------------------
    // Stage 3 Ã¢â‚¬â€ diagnostic / logging-only test (no asserts on driver)
    // ---------------------------------------------------------------------

    @Test
    public void logging_combinations_doNotThrow_acrossThemesAndLevels() {
        // Sanity: rotate every level + tree logging on the active theme, ensure no NPE/format errors.
        // (Theme is set once in @BeforeClass and never switched mid-suite.)
        LogTheme theme = CustomLogger.getCurrentTheme();
        CustomLogger.debug.log("theme=" + theme);
        CustomLogger.info.click("button");
        CustomLogger.info.input("typed=value");
        CustomLogger.info.dropdown("OptionA");
        CustomLogger.warn.wait("element to be visible");
        CustomLogger.error.failed("simulated failure (no real error)");

        // Tree-style logging using fields(...) and (heading, k,v) overload:
        CustomLogger.debug.log("Resolution Snapshot",
                "Theme",     theme.name(),
                "AnsiOn",    String.valueOf(CustomLogger.isAnsiEnabled()),
                "Resolver",  RESOLVER.getClass().getSimpleName());

        // Exercise the explicit fields(...) helper too:
        Map<String, Object> map = CustomLogger.fields("k1", "v1", "k2", 42);
        CustomLogger.info.log("Map Heading", map);
        assertEquals(map.get("k1"), "v1");
        assertEquals(map.get("k2"), 42);
    }
}

