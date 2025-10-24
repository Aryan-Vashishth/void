package core.locators;

import Elements.Interfaces.BaseElement;
import org.openqa.selenium.By;

/** Centralized AUTO resolver: try JSON, then .properties. */
public final class LocatorResolver {

    private LocatorResolver() {}

    public static By primary(BaseElement el) {
        By by = tryJsonPrimary(el);
        return (by != null) ? by : PropertiesFileLocatorReader.resolvePrimary(el);
    }

    public static By secondary(BaseElement el) {
        By by = tryJsonSecondary(el);
        return (by != null) ? by : PropertiesFileLocatorReader.resolveSecondary(el);
    }

    public static By key(BaseElement el, String key, Object... args) {
        By by = tryJsonKey(el, key, args);
        return (by != null) ? by : PropertiesFileLocatorReader.resolveKey(el, key, args);
    }

    private static By tryJsonPrimary(BaseElement el) {
        try { return JsonLocatorReader.resolvePrimary(el); } catch (Exception ignore) { return null; }
    }

    private static By tryJsonSecondary(BaseElement el) {
        try { return JsonLocatorReader.resolveSecondary(el); } catch (Exception ignore) { return null; }
    }

    private static By tryJsonKey(BaseElement el, String key, Object... args) {
        try { return JsonLocatorReader.resolveKey(el, key, args); } catch (Exception ignore) { return null; }
    }
}
