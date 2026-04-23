// file: core/resolvers/locator/LocatorResolverV1.java
package core.resolvers.locator;

import core.resolvers.locator.api.LocatorRequest;
import core.resolvers.locator.api.LocatorResolver;
import core.resolvers.locator.api.LocatorResolvers;
import core.resolvers.locator.template.LocatorTemplate;
import elements.meta.ElementRole;
import elements.api.Element;
import org.openqa.selenium.By;

/**
 * Legacy static façade — preserved for backward compatibility.
 *
 * <p>All orchestration now delegates to {@link LocatorResolvers#strict()}.</p>
 *
 * @deprecated since the Phase&nbsp;3 OO refactor. New code should use {@link LocatorResolvers#strict()}
 *             directly. This class will be removed after callers have migrated.
 */
@Deprecated(forRemoval = true, since = "Phase 3 OO refactor")
public final class LocatorResolverV1 {

    private LocatorResolverV1() { /* Static utility — prevent instantiation */ }

    private static LocatorResolver R() { return LocatorResolvers.strict(); }

    // ===== public API =====
    public static By getLocator(String fileName, String key, Object... args) {
        return R().resolve(LocatorRequest.of(fileName, key, args));
    }

    public static By getLocator(Element e) {
        return R().resolve(e);
    }

    public static By getLocator(Element e, ElementRole role, Object... overrideArgs) {
        return R().resolve(e, role, overrideArgs);
    }

    public static By getBestAvailable(Element e, Object... overrideArgs) {
        return R().resolveBest(e, overrideArgs);
    }

    public static String getRawLocator(String fileName, String key) {
        return R().rawTemplate(LocatorRequest.of(fileName, key));
    }

    /** STRICT-policy template formatter ({@code %s}/{@code %n$s}; throws on too few args). */
    public static String resolveLocatorTemplate(String template, Object... args) {
        return LocatorTemplate.strict(template).format(args);
    }
}
