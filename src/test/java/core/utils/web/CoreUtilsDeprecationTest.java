package core.utils.web;

import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

/**
 * Regression guard: verifies that {@link DOMUtils}, {@link WaitUtils} (By-based fields),
 * and {@link TableHandler} carry the expected {@link Deprecated} annotations introduced
 * in the core-utils-engine-agnostic initiative (ADR-020).
 *
 * These tests catch accidental annotation removal during future refactoring.
 * No browser, no driver, no I/O -- pure reflection.
 */
public class CoreUtilsDeprecationTest {

    // ── DOMUtils ──────────────────────────────────────────────────────────────

    @Test
    public void domUtils_classIsDeprecated() {
        assertNotNull(DOMUtils.class.getAnnotation(Deprecated.class),
                "DOMUtils class must carry @Deprecated(forRemoval=true)");
    }

    @Test
    public void domUtils_allPublicMethodsAreDeprecated() {
        List<Method> publicMethods = Arrays.stream(DOMUtils.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .collect(Collectors.toList());

        assertFalse(publicMethods.isEmpty(), "DOMUtils must have at least one public method");

        List<String> undeprecated = publicMethods.stream()
                .filter(m -> m.getAnnotation(Deprecated.class) == null)
                .map(Method::getName)
                .collect(Collectors.toList());

        assertTrue(undeprecated.isEmpty(),
                "DOMUtils methods missing @Deprecated: " + undeprecated);
    }

    @Test
    public void domUtils_deprecatedWithForRemoval() {
        Deprecated annotation = DOMUtils.class.getAnnotation(Deprecated.class);
        assertNotNull(annotation);
        assertTrue(annotation.forRemoval(),
                "DOMUtils @Deprecated must have forRemoval=true");
    }

    // ── WaitUtils ─────────────────────────────────────────────────────────────

    @Test
    public void waitUtils_angularLoaderFieldIsDeprecated() throws NoSuchFieldException {
        Field field = WaitUtils.class.getDeclaredField("ANGULAR_LOADER");
        assertNotNull(field.getAnnotation(Deprecated.class),
                "ANGULAR_LOADER must carry @Deprecated(forRemoval=true)");
        assertTrue(field.getAnnotation(Deprecated.class).forRemoval());
    }

    @Test
    public void waitUtils_spinSpinnerLoaderFieldIsDeprecated() throws NoSuchFieldException {
        Field field = WaitUtils.class.getDeclaredField("SPIN_SPINNER_LOADER");
        assertNotNull(field.getAnnotation(Deprecated.class),
                "SPIN_SPINNER_LOADER must carry @Deprecated(forRemoval=true)");
        assertTrue(field.getAnnotation(Deprecated.class).forRemoval());
    }

    @Test
    public void waitUtils_byBasedPublicMethodsAreDeprecated() {
        List<Method> byBasedPublic = Arrays.stream(WaitUtils.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> Arrays.stream(m.getParameterTypes())
                        .anyMatch(p -> p.getName().equals("org.openqa.selenium.By")))
                .collect(Collectors.toList());

        assertFalse(byBasedPublic.isEmpty(),
                "Expected at least one public By-parameter method on WaitUtils");

        List<String> undeprecated = byBasedPublic.stream()
                .filter(m -> m.getAnnotation(Deprecated.class) == null)
                .map(m -> m.getName() + "(" +
                        Arrays.stream(m.getParameterTypes()).map(Class::getSimpleName)
                              .collect(Collectors.joining(", ")) + ")")
                .collect(Collectors.toList());

        assertTrue(undeprecated.isEmpty(),
                "WaitUtils By-parameter methods missing @Deprecated: " + undeprecated);
    }

    // ── TableHandler ──────────────────────────────────────────────────────────

    @Test
    public void tableHandler_classIsDeprecated() {
        assertNotNull(TableHandler.class.getAnnotation(Deprecated.class),
                "TableHandler class must carry @Deprecated(forRemoval=true)");
    }

    @Test
    public void tableHandler_allPublicMethodsAreDeprecated() {
        List<Method> publicMethods = Arrays.stream(TableHandler.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .collect(Collectors.toList());

        assertFalse(publicMethods.isEmpty(), "TableHandler must have at least one public method");

        List<String> undeprecated = publicMethods.stream()
                .filter(m -> m.getAnnotation(Deprecated.class) == null)
                .map(Method::getName)
                .collect(Collectors.toList());

        assertTrue(undeprecated.isEmpty(),
                "TableHandler methods missing @Deprecated: " + undeprecated);
    }

    @Test
    public void tableHandler_deprecatedWithForRemoval() {
        Deprecated annotation = TableHandler.class.getAnnotation(Deprecated.class);
        assertNotNull(annotation);
        assertTrue(annotation.forRemoval(),
                "TableHandler @Deprecated must have forRemoval=true");
    }
}
