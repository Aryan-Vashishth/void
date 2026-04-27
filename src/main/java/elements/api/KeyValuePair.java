package elements.api;

import java.util.Map;

/**
 * Type-safe key/value abstraction that also participates in the element locator pipeline.
 * <p>
 * Typical use cases:
 * <ul>
 *   <li>Enum constants mapping an internal key to a user-facing label.</li>
 *   <li>Data-driven pairs exposed for logging or selection flows.</li>
 * </ul>
 * </p>
 * <p>
 * Locator semantics: by default the {@code primaryLocator} is the key's {@code toString()}, and the
 * {@code secondaryLocator} is the value's {@code toString()}. Override {@link #getPrimaryLocator()} /
 * {@link #getSecondaryLocator()} if the displayed key/value are not actual property keys.
 * </p>
 */
public interface KeyValuePair<K, V>  extends Element{
    /** @return logical key component (often the enum constant name or internal identifier). */
    K getKey();
    /** @return logical value component (often human readable label). */
    V getValue();

    /** @return immutable entry view (Java 9 Map.entry). */
    default Map.Entry<K, V> toEntry() { return Map.entry(getKey(), getValue()); }
    /** @return alias to {@link #toEntry()} for semantic clarity in streaming contexts. */
    default Map.Entry<K, V> keyValuePair() { return toEntry(); }

    // bridging helpers (Element defaults for KeyValuePair implementors)
    default String getExternalFileName() { return null; }
    default String getSecondaryLocator() { return getValue() == null ? null : getValue().toString(); }
    default Object[] getArgs() { return new Object[0]; }
}
