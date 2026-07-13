package core.resolvers.locator.sync;

import elements.meta.ElementRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects keys in an existing properties file that no longer correspond to a known enum constant.
 *
 * <p>Expected key format: {@code PageClass.EnumClass.CONSTANT_NAME.ROLE}
 * (4 or more dot-separated segments, last segment is a valid {@link ElementRole} name).
 * Keys with fewer segments or an unrecognised role are reported as malformed rather than orphan.</p>
 *
 * <p>Orphan keys are reported as warnings; the file is never modified by this class.</p>
 */
final class OrphanKeyDetector {

    record OrphanWarning(String key, int lineNumber, String reason) {}

    private static final Set<String> VALID_ROLES;
    static {
        Set<String> roles = new LinkedHashSet<>();
        for (ElementRole r : ElementRole.values()) roles.add(r.name());
        VALID_ROLES = Collections.unmodifiableSet(roles);
    }

    List<OrphanWarning> detect(Class<?> pageClass, LineTrackingPropertiesReader reader) {
        List<OrphanWarning> warnings = new ArrayList<>();
        Map<String, Set<String>> constantIndex = buildConstantIndex(pageClass);

        for (Map.Entry<String, Integer> entry : reader.allLineNumbers().entrySet()) {
            String key  = entry.getKey();
            int    line = entry.getValue();

            String[] parts = key.split("\\.");
            if (parts.length < 3) {
                warnings.add(new OrphanWarning(key, line, "malformed key — fewer than 3 segments"));
                continue;
            }

            String constantName;
            String enumSimple;

            // With role suffix (>= 4 parts, last is a valid ElementRole)
            if (parts.length >= 4 && VALID_ROLES.contains(parts[parts.length - 1])) {
                constantName = parts[parts.length - 2];
                enumSimple   = parts[parts.length - 3];
            } else {
                // Old format or plain Element key without role suffix
                constantName = parts[parts.length - 1];
                enumSimple   = parts[parts.length - 2];
            }

            Set<String> constants = constantIndex.get(enumSimple);
            if (constants == null) {
                warnings.add(new OrphanWarning(key, line,
                    "enum class '" + enumSimple + "' not found in " + pageClass.getSimpleName()));
                continue;
            }
            if (!constants.contains(constantName)) {
                warnings.add(new OrphanWarning(key, line,
                    "constant '" + constantName + "' not found in " + enumSimple));
            }
        }
        return warnings;
    }

    private Map<String, Set<String>> buildConstantIndex(Class<?> pageClass) {
        Map<String, Set<String>> index = new LinkedHashMap<>();
        for (Class<?> nested : pageClass.getDeclaredClasses()) {
            if (!nested.isEnum()) continue;
            Set<String> names = new LinkedHashSet<>();
            for (Object c : nested.getEnumConstants()) names.add(((Enum<?>) c).name());
            index.put(nested.getSimpleName(), names);
        }
        return index;
    }
}
