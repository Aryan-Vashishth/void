package Elements;

/**
 * Enumerated semantic roles for locator keys across UI element interfaces.
 * <p>Used to provide ordered, type-safe fallback and categorization.</p>
 */
public enum ElementRole {
    /** Primary locator (first attempt). */
    PRIMARY,
    /** Secondary fallback locator. */
    SECONDARY,

    /** Clickable trigger element (button/icon). */
    TRIGGER,
    /** Text or value input field. */
    INPUT,
    /** Generic list container or options panel. */
    LIST,
    /** Static textual element. */
    TEXT,

    /** Search text input field. */
    SEARCH_INPUT,
    /** Search action button. */
    SEARCH_BUTTON,
    /** Search result list/panel. */
    SEARCH_RESULT,

    /** Tooltip content element (full text). */
    TOOLTIP_CONTENT,

    /** Table root element. */
    TABLE,
    /** Table row locator. */
    ROW,
    /** Table column locator. */
    COLUMN,
    /** Individual cell locator. */
    CELL,
    /** Header cell locator. */
    HEADER,
    /** Add row action button. */
    ADD_ROW_BUTTON,
    /** Remove row action button. */
    REMOVE_ROW_BUTTON,
    /** Footer input row container. */
    FOOTER_INPUT_ROW,

    /** Multi-instance dropdown trigger (distinct context). */
    MULTI_TRIGGER,
    /** Multi-instance dropdown list/options container. */
    MULTI_LIST
}
