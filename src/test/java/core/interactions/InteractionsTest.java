package core.interactions;
import core.driver.DriverContext;
import core.logging.CustomLogger;
import interactions.Interactions;
import interactions.hooks.ActionHandler;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.testng.Assert.*;
/**
 * Unit tests for {@link Interactions}.
 *
 * <p>The framework does not bundle Mockito, so these tests use a lightweight
 * {@link java.lang.reflect.Proxy}-based fake {@link WebDriver}. Coverage is
 * deliberately scoped to the parts of {@code Interactions} that can be exercised
 * without a real browser:</p>
 * <ul>
 *   <li>Constructor wiring (driver + WebDriverWait fields, DriverContext registration).</li>
 *   <li>The {@link Interactions#of(ActionHandler...)} hook-collection helper.</li>
 * </ul>
 *
 * <p>End-to-end behaviour for click / type / dropdown / search flows is intentionally
 * left to integration suites that bring up a real driver.</p>
 */
public class InteractionsTest {
    // ---------------------------------------------------------------------
    // Test fixtures
    // ---------------------------------------------------------------------
    /**
     * Builds a no-op {@link WebDriver} via {@link Proxy}. Every interface method
     * returns a sensible default; the tests below never invoke any of them.
     */
    private static WebDriver newFakeDriver() {
        InvocationHandler h = (proxy, method, args) -> defaultFor(method.getReturnType());
        return (WebDriver) Proxy.newProxyInstance(
                InteractionsTest.class.getClassLoader(),
                new Class<?>[] { WebDriver.class },
                h);
    }
    private static Object defaultFor(Class<?> t) {
        if (!t.isPrimitive())     return null;
        if (t == boolean.class)   return Boolean.FALSE;
        if (t == void.class)      return null;
        if (t == long.class)      return 0L;
        if (t == double.class)    return 0d;
        if (t == float.class)     return 0f;
        return 0;
    }

    @BeforeClass
    public void setup(){
        CustomLogger.initialize(this.getClass());
        CustomLogger.enableAnsi();
    }

    @AfterMethod(alwaysRun = true)
    public void detachDrivers() {
        // Constructor calls DriverContext.setPrimaryDriver - clean up so tests
        // remain order-independent.
        try { DriverContext.removePrimary(); }   catch (Exception ignored) {}
        try { DriverContext.removeSecondary(); } catch (Exception ignored) {}
    }
    // ---------------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------------
    @Test
    public void constructor_assignsDriverField() throws Exception {
        WebDriver fake = newFakeDriver();
        Interactions ix = new Interactions(fake);
        Field driverField = Interactions.class.getDeclaredField("driver");
        driverField.setAccessible(true);
        assertSame(driverField.get(ix), fake, "driver field should be the supplied WebDriver");
    }
    @Test
    public void constructor_initialisesWebDriverWaitWithTenSecondTimeout() throws Exception {
        Interactions ix = new Interactions(newFakeDriver());
        Field waitField = Interactions.class.getDeclaredField("wait");
        waitField.setAccessible(true);
        Object wait = waitField.get(ix);
        assertNotNull(wait, "wait field must be initialised");
        assertEquals(wait.getClass().getSimpleName(), "WebDriverWait");
        // Verify timeout by reading the private `timeout` field declared on
        // FluentWait (WebDriverWait''s parent). FluentWait does not expose a
        // public getter in the Selenium version this project pins to.
        Field timeoutField = wait.getClass().getSuperclass().getDeclaredField("timeout");
        timeoutField.setAccessible(true);
        assertEquals(timeoutField.get(wait), Duration.ofSeconds(10));
    }
    @Test
    public void constructor_registersDriverInDriverContext() {
        WebDriver fake = newFakeDriver();
        new Interactions(fake);
        assertSame(DriverContext.getDriver(), fake,
                "Interactions constructor must register driver as PRIMARY in DriverContext");
    }
    // ---------------------------------------------------------------------
    // of(ActionHandler...)
    // ---------------------------------------------------------------------
    @Test
    public void of_returnsNullForNullVarargs() {
        assertNull(Interactions.of((ActionHandler[]) null),
                "of(null) should return null so callers can short-circuit hook execution");
    }
    @Test
    public void of_returnsNullForEmptyVarargs() {
        assertNull(Interactions.of(),
                "of() with no handlers should return null (treated as 'no hooks')");
    }
    @Test
    public void of_returnsImmutableListWithSuppliedHandlers() {
        AtomicInteger counter = new AtomicInteger();
        ActionHandler a = drv -> counter.incrementAndGet();
        ActionHandler b = drv -> counter.addAndGet(10);
        List<ActionHandler> hooks = Interactions.of(a, b);
        assertNotNull(hooks);
        assertEquals(hooks.size(), 2);
        assertSame(hooks.get(0), a);
        assertSame(hooks.get(1), b);
        // Returned list is created via List.of(...) and must be immutable.
        assertThrows(UnsupportedOperationException.class, () -> hooks.add(a));
        // Handlers should still be invocable and produce the expected side effects.
        hooks.forEach(h -> h.execute(null));
        assertEquals(counter.get(), 11, "Both handlers should have run exactly once");
    }
}