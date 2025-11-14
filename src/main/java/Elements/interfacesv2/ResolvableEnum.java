package Elements.interfacesv2;

/**
 * An interface to provide standardized name and label access for enum constants.
 * Intended for use with enums representing UI elements, etc.
 */
public interface ResolvableEnum {

    /**
     * Returns the enum constant's name (raw, as in the enum).
     */
    default String getName() {
        return ((Enum<?>) this).name();
    }

    /**
     * Returns a formatted, human-friendly label (e.g., "ABC_WAS_THE" → "Abc Was The").
     */
    default String getLabel() {
        String raw = ((Enum<?>) this).name();
        String[] words = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
