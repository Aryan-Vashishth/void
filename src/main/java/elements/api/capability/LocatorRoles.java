package elements.api.capability;

import elements.meta.ElementRole;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class LocatorRoles {

    record RoleEntry(ElementRole role, String key) {}

    static RoleEntry role(ElementRole role, String key) {
        return new RoleEntry(role, key);
    }

    static Map<ElementRole, String> roleMap(RoleEntry... roles) {
        Map<ElementRole, String> result = new LinkedHashMap<>();
        Set<String> seen = new LinkedHashSet<>();
        for (RoleEntry r : roles) {
            if (r.key() != null && !r.key().isBlank() && seen.add(r.key())) {
                result.put(r.role(), r.key());
            }
        }
        return result;
    }

    private LocatorRoles() {}
}
