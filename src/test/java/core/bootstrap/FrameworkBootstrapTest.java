package core.bootstrap;

import core.utils.ConfigPaths;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.net.URL;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link FrameworkBootstrap}.
 *
 * <p>All tests that call {@code init()} must reset bootstrap state before and after
 * to avoid ordering dependencies. {@code reset()} is package-private and called via
 * reflection.</p>
 */
public class FrameworkBootstrapTest {

    @BeforeMethod
    @AfterMethod
    public void resetBootstrap() throws Exception {
        Method reset = FrameworkBootstrap.class.getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(null);
    }

    @Test
    public void init_isIdempotent() {
        FrameworkBootstrap.init();
        FrameworkBootstrap.init(); // must not throw
        assertTrue(FrameworkBootstrap.isInitialized());
    }

    @Test
    public void init_setsInitializedFlag() {
        assertFalse(FrameworkBootstrap.isInitialized());
        FrameworkBootstrap.init();
        assertTrue(FrameworkBootstrap.isInitialized());
    }

    @Test
    public void getUtilsConfig_returnsEmptyBeforeInit() throws Exception {
        assertTrue(FrameworkBootstrap.getUtilsConfig().isEmpty(),
                "getUtilsConfig() must return empty Properties before init()");
    }

    @Test
    public void init_succeedsWithoutDriverPropertiesOnClasspath() throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        ClassLoader noDriverProps = new ClassLoader(original) {
            @Override
            public URL getResource(String name) {
                if (ConfigPaths.DRIVER_DEFAULT.equals(name)) return null;
                return super.getResource(name);
            }
        };

        Thread.currentThread().setContextClassLoader(noDriverProps);
        try {
            FrameworkBootstrap.init();
            assertTrue(FrameworkBootstrap.isInitialized(),
                    "init() must succeed even when driver.properties is absent -- "
                    + "web-domain config is validated at web session creation, not framework startup");
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
