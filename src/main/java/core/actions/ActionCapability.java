package core.actions;

/**
 * Capability token identifying the behavioral contract an action operates against.
 *
 * <p>Used for metadata purposes only -- logging, tracing, diagnostics, and profile
 * resolution. Never carries execution logic. Behavioral execution continues through
 * the capability interface directly.</p>
 *
 * <p>The set is intentionally open: any domain can define new capabilities via
 * {@link #of(String)} without editing runtime-owned files.</p>
 *
 * <p>{@code UNKNOWN} is the fallback for actions not bound to a specific capability
 * (e.g., raw lambda actions).</p>
 *
 * @see core.actions.Action#capability()
 */
public interface ActionCapability {

    /** Returns the canonical name of this capability, used in logging and diagnostics. */
    String name();

    /**
     * Creates a capability with the given name.
     *
     * <p>Equality is name-based: two capabilities with the same name are equal.
     * Prefer static constants over repeated {@code of()} calls to guarantee
     * consistent identity and avoid allocation.</p>
     *
     * @param name non-blank canonical name
     * @return an ActionCapability for that name
     * @throws IllegalArgumentException if name is null or blank
     */
    static ActionCapability of(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("ActionCapability name must not be blank");
        }
        return new NamedCapability(name);
    }

    // Built-in UI-domain capabilities -- identities preserved from the former enum.
    ActionCapability CLICKABLE           = of("CLICKABLE");
    ActionCapability TYPEABLE            = of("TYPEABLE");
    ActionCapability SELECTABLE          = of("SELECTABLE");
    ActionCapability HOVERABLE           = of("HOVERABLE");
    ActionCapability CHECKABLE           = of("CHECKABLE");
    ActionCapability UPLOADABLE          = of("UPLOADABLE");
    ActionCapability SEARCHABLE          = of("SEARCHABLE");
    ActionCapability SEARCH_FIELD        = of("SEARCH_FIELD");
    ActionCapability SEARCHABLE_DROPDOWN = of("SEARCHABLE_DROPDOWN");
    ActionCapability READ_ONLY           = of("READ_ONLY");
    ActionCapability TABLE               = of("TABLE");
    ActionCapability EDITABLE_TABLE      = of("EDITABLE_TABLE");
    ActionCapability LISTABLE            = of("LISTABLE");
    ActionCapability MULTI_SELECTABLE    = of("MULTI_SELECTABLE");
    ActionCapability UNKNOWN             = of("UNKNOWN");
}
