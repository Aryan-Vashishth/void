package elements.api;

import javax.annotation.Nullable;

/**
 * Extension of {@link LocatorFamily} for enum groups where some constants have semantic
 * values that cannot be automatically derived from the constant name — acronyms, symbols,
 * punctuation, or domain-specific labels.
 *
 * <h3>Pattern</h3>
 * <p>Constants with predictable names get automatic word-transform derivation (same as
 * {@link LocatorFamily}). Constants that require a custom value declare it via a
 * constructor argument stored in a field. {@link #getSemanticValue()} exposes that field
 * and {@link #getArgs()} routes accordingly.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public interface VendorPage {
 *
 *     enum Filters implements Clickable, AdvancedLocatorFamily {
 *
 *         COUNTRY,                                  // auto: "Country"
 *         PROGRAM_NAME,                             // auto: "Program Name"
 *         HQ_STATE_PROVINCE("HQ State/Province"),   // explicit: slash in value
 *         SAVE_AND_CONTINUE("Save & Continue"),     // explicit: ampersand
 *         CRM("CRM");                               // explicit: all-caps acronym
 *
 *         private final String semanticValue;
 *         Filters()          { this.semanticValue = null; }
 *         Filters(String v)  { this.semanticValue = v;    }
 *
 *         @Override public String getPrimaryLocator() { return AdvancedLocatorFamily.super.getPrimaryLocator(); }
 *         @Override public String getTriggerLocator() { return getPrimaryLocator(); }
 *         @Override public String getSemanticValue()  { return semanticValue; }
 *     }
 * }
 * }</pre>
 *
 * <h3>Key format</h3>
 * <p>Unchanged from {@link LocatorFamily}: {@code PageName.EnumName} — no constant suffix.</p>
 *
 * <h3>Argument resolution</h3>
 * <ul>
 *   <li>If {@link #getSemanticValue()} returns non-null → use it as the runtime arg.</li>
 *   <li>If {@link #getSemanticValue()} returns null → fall back to
 *       {@link LocatorFamily#getArgs()} word-transform.</li>
 * </ul>
 *
 * <h3>When to use instead of LocatorFamily</h3>
 * <p>Prefer {@code AdvancedLocatorFamily} when most constants can use auto-derivation but
 * a few require custom values. Prefer {@link SwitchLocatorFamily} when all or most
 * constants require custom values and centralised exhaustive mapping is desired.</p>
 */
public interface AdvancedLocatorFamily extends LocatorFamily {

    /**
     * Returns the explicit semantic value for this constant, or {@code null} to request
     * automatic word-transform derivation from the constant name.
     */
    @Nullable
    String getSemanticValue();

    /**
     * Returns the runtime locator argument for this constant.
     * Uses {@link #getSemanticValue()} when non-null; otherwise delegates to
     * {@link LocatorFamily}'s word-transform derivation.
     */
    @Override
    default Object[] getArgs() {
        String explicit = getSemanticValue();
        return explicit != null ? new Object[]{explicit} : LocatorFamily.super.getArgs();
    }
}
