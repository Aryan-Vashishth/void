package core.resolvers.locator.sync;

import elements.api.Element;
import elements.meta.ElementRole;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Derives the ordered set of expected locator keys for all enum constants in a page class.
 *
 * <p>Key format: {@code PageClass.EnumClass.CONSTANT_NAME.ROLE}, where ROLE is
 * {@link ElementRole#name()} — the same token {@code EnumLocatorScanner} uses in JSON.
 * PRIMARY and SECONDARY are meta-roles used internally and are excluded from templates.</p>
 */
final class LocatorTemplateGenerator {

    private static final Set<ElementRole> META_ROLES =
        EnumSet.of(ElementRole.PRIMARY, ElementRole.SECONDARY);

    /**
     * A single expected locator key together with the enum class it belongs to.
     *
     * @param enumSimpleName simple name of the inner enum class (used for section headers)
     * @param key            fully-qualified locator key
     */
    record LocatorKey(String enumSimpleName, String key) {}

    /**
     * Returns all expected keys in declaration order.
     * Constants that are not {@link Element} instances are skipped.
     */
    List<LocatorKey> generateKeys(Class<?> pageClass) {
        String pageSimple = pageClass.getSimpleName();
        List<LocatorKey> result = new ArrayList<>();

        for (Class<?> nested : pageClass.getDeclaredClasses()) {
            if (!nested.isEnum()) continue;
            String enumSimple = nested.getSimpleName();

            for (Object constant : nested.getEnumConstants()) {
                if (!(constant instanceof Element element)) continue;
                String constantName = ((Enum<?>) constant).name();

                Map<ElementRole, String> roles = element.getAllLocatorRoles();
                List<ElementRole> capabilityRoles = roles.keySet().stream()
                    .filter(r -> !META_ROLES.contains(r))
                    .toList();

                if (capabilityRoles.isEmpty()) {
                    // Plain Element without a capability interface — no role suffix
                    result.add(new LocatorKey(enumSimple,
                        pageSimple + "." + enumSimple + "." + constantName));
                } else {
                    for (ElementRole role : capabilityRoles) {
                        result.add(new LocatorKey(enumSimple,
                            pageSimple + "." + enumSimple + "." + constantName + "." + role.name()));
                    }
                }
            }
        }
        return result;
    }
}
