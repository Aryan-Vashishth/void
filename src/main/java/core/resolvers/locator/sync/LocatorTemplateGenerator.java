package core.resolvers.locator.sync;

import domain.automation.web.vocabulary.element.UIElement;
import domain.automation.web.vocabulary.role.ElementRole;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Derives the ordered set of expected locator keys for all enum constants in a page class.
 *
 * <p>Key format: {@code EnclosingClass.EnumClass.CONSTANT_NAME.ROLE}, where ROLE is
 * {@link ElementRole#name()} -- the same token {@code EnumLocatorScanner} uses in JSON.
 * For direct enum children of the page class the prefix is {@code PageName.EnumName}.
 * For enums nested inside inner interfaces the prefix is {@code InterfaceName.EnumName},
 * matching what {@link UIElement#qualifiedLocatorKey} produces at runtime.
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
     * Constants that are not {@link UIElement} instances are skipped.
     * Enums inside nested interfaces or nested classes are included recursively.
     */
    List<LocatorKey> generateKeys(Class<?> pageClass) {
        List<LocatorKey> result = new ArrayList<>();
        collectKeys(pageClass, result);
        return result;
    }

    private void collectKeys(Class<?> scope, List<LocatorKey> result) {
        for (Class<?> nested : scope.getDeclaredClasses()) {
            if (nested.isEnum()) {
                collectEnumKeys(nested, result);
            } else if (!nested.isSynthetic()) {
                collectKeys(nested, result);
            }
        }
    }

    private void collectEnumKeys(Class<?> enumClass, List<LocatorKey> result) {
        String enclosingSimple = enumClass.getEnclosingClass().getSimpleName();
        String enumSimple = enumClass.getSimpleName();

        for (Object constant : enumClass.getEnumConstants()) {
            if (!(constant instanceof UIElement element)) continue;

            // Family-locator pattern: one shared template key covers all constants.
            String familyKey = element.templateFamilyKey();
            if (familyKey != null) {
                if (result.stream().noneMatch(k -> k.key().equals(familyKey))) {
                    result.add(new LocatorKey(enumSimple, familyKey));
                }
                continue;
            }

            String constantName = ((Enum<?>) constant).name();

            Map<ElementRole, String> roles = element.getAllLocatorRoles();
            List<ElementRole> capabilityRoles = roles.keySet().stream()
                .filter(r -> !META_ROLES.contains(r))
                .toList();

            if (capabilityRoles.isEmpty()) {
                result.add(new LocatorKey(enumSimple,
                    enclosingSimple + "." + enumSimple + "." + constantName));
            } else {
                for (ElementRole role : capabilityRoles) {
                    result.add(new LocatorKey(enumSimple,
                        enclosingSimple + "." + enumSimple + "." + constantName + "." + role.name()));
                }
            }
        }
    }
}
