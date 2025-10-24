package Elements.Interfaces;

import java.util.*;
import java.util.stream.Collectors;

public interface KeyValuePairElement extends BaseElement {
    /** Human-readable key/label (e.g., "AM Id"). */
    String getKey();

    /** Your implementation's "value" token (often a locator key like LINKED_DIALOG_VALUE_VIA_KEY_TEXT). */
    String getValue();

    // -------- Default helpers (auto-discover enum constants) --------

    /** Returns all keys for the enum that implements this interface (or a singleton for non-enums). */
    default List<String> getAllKeys() {
        return getConstants().stream()
                .map(KeyValuePairElement::getKey)
                .collect(Collectors.toList());
    }

    /** Returns all values for the enum that implements this interface (or a singleton for non-enums). */
    default List<String> getAllValues() {
        return getConstants().stream()
                .map(KeyValuePairElement::getValue)
                .collect(Collectors.toList());
    }

    /** Returns a LinkedHashMap (enum declaration order preserved) of key -> value. */
    default Map<String, String> getKeyValueMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (KeyValuePairElement e : getConstants()) {
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    /**
     * Returns all enum constants of the declaring enum that implements this interface.
     * If the implementer isn’t an enum, returns a singleton list with `this`.
     */
    @SuppressWarnings("unchecked")
    default List<KeyValuePairElement> getConstants() {
        if (this instanceof Enum<?>) {
            Class<?> declaring = ((Enum<?>) this).getDeclaringClass();
            Object[] constants = declaring.getEnumConstants();
            List<KeyValuePairElement> out = new ArrayList<>(constants.length);
            for (Object c : constants) out.add((KeyValuePairElement) c);
            return out;
        }
        return List.of(this);
    }

    // -------- Optional static helpers (when you only have the class) --------

    static <E extends Enum<E> & KeyValuePairElement> List<String> keysOf(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(KeyValuePairElement::getKey)
                .collect(Collectors.toList());
    }

    static <E extends Enum<E> & KeyValuePairElement> Map<String,String> mapOf(Class<E> enumClass) {
        LinkedHashMap<String,String> map = new LinkedHashMap<>();
        for (E e : enumClass.getEnumConstants()) map.put(e.getKey(), e.getValue());
        return map;
    }
}
