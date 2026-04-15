package elements.api;

import elements.meta.ElementRole;

/**
 * Adds standardized name/label semantics to enums so they can be surfaced
 * as {@link KeyValuePair} instances, enabling consistent logging or UI selection flows.
 * <p>Default label formatting: transforms ENUM_CONSTANT to "Enum Constant".</p>
 */
public interface ResolvableEnum extends KeyValuePair<String,String> {

    /** @return raw enum constant name. */
    default String getName() {
        return ((Enum<?>) this).name();
    }

    /** @return human-friendly label (Title Case, underscores removed). */
    default String getLabel() {
        String raw = getName();
        String[] words = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) sb.append(w.substring(1).toLowerCase());
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    /** @return lowerCamel variant (ENUM_CONSTANT -> enumConstant). */
    default String toLowerCamel() {
        String raw = getName().toLowerCase();
        String[] parts = raw.split("_");
        if (parts.length == 0) return raw;
        StringBuilder b = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return b.toString();
    }

    @Override default String getKey(){ return getName(); }
    @Override default String getValue(){ return getLabel(); }
    default String getPrimaryLocator(){ return getKey(); }
    default String getExternalFileName(){ return null; }
    default Object[] getArgs(){ return new Object[0]; }
    default String getDisplayText(){ return getLabel(); }
    default java.util.Map<ElementRole,String> getAllLocatorRoles(){
        java.util.Map<ElementRole,String> roles = new java.util.LinkedHashMap<>();
        String primary = getPrimaryLocator();
        if(primary!=null && !primary.isBlank()) roles.put(ElementRole.TEXT, primary);
        return roles;
    }
}
