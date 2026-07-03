package core.actions;

/**
 * Canonical identifiers for all supported UI element capabilities.
 *
 * <p>Used for metadata purposes only — logging, tracing, diagnostics, metrics, and
 * serialization. Behavioral execution continues through the capability interface directly.</p>
 *
 * <p>{@code UNKNOWN} is the fallback for actions not bound to a specific element capability
 * (e.g., raw lambda actions).</p>
 *
 * @see ActionCapabilityProvider
 */
public enum ActionCapability {
    CLICKABLE,
    TYPEABLE,
    SELECTABLE,
    HOVERABLE,
    CHECKABLE,
    UPLOADABLE,
    SEARCHABLE,
    SEARCH_FIELD,
    SEARCHABLE_DROPDOWN,
    READ_ONLY,
    TABLE,
    EDITABLE_TABLE,
    LISTABLE,
    MULTI_SELECTABLE,
    UNKNOWN
}

