package elements.api;

/**
 * Extension of {@link LocatorFamily} for enum groups where all constants require explicit
 * semantic values and a centralised, compiler-validated mapping is preferred over
 * per-constant constructors.
 *
 * <h3>Mechanism</h3>
 * <p>The abstract {@link #getSemanticValue()} forces the implementing enum to provide an
 * exhaustive switch expression. The Java compiler rejects any enum that adds a constant
 * without updating the switch, and IntelliJ's <em>Add missing branches</em> quick-fix
 * inserts the new case automatically.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public interface ReportsPage {
 *
 *     enum Sections implements Clickable, SwitchLocatorFamily {
 *         OVERVIEW,
 *         KPI_SUMMARY,
 *         VENDOR_PERFORMANCE,
 *         YTD_ANALYSIS;
 *
 *         @Override public String getPrimaryLocator() { return SwitchLocatorFamily.super.getPrimaryLocator(); }
 *         @Override public String getTriggerLocator() { return getPrimaryLocator(); }
 *
 *         @Override
 *         public String getSemanticValue() {
 *             return switch (this) {
 *                 case OVERVIEW           -> "Overview";
 *                 case KPI_SUMMARY        -> "KPI Summary";
 *                 case VENDOR_PERFORMANCE -> "Vendor Performance";
 *                 case YTD_ANALYSIS       -> "YTD Analysis";
 *             };
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Key format</h3>
 * <p>Unchanged from {@link LocatorFamily}: {@code PageName.EnumName} — no constant suffix.</p>
 *
 * <h3>Argument resolution</h3>
 * <p>{@link #getArgs()} always returns {@code [getSemanticValue()]}. There is no fallback
 * to auto-derivation — every constant is expected to have an explicit mapping in the switch.</p>
 *
 * <h3>When to use instead of AdvancedLocatorFamily</h3>
 * <ul>
 *   <li>Most or all constants require custom semantic values.</li>
 *   <li>A single centralised mapping is preferred over scattered constructors.</li>
 *   <li>Compile-time exhaustiveness on every new constant is required.</li>
 * </ul>
 * <p>Prefer {@link AdvancedLocatorFamily} when most constants can auto-derive and only
 * a few need custom values — the constructor pattern is less verbose in that case.</p>
 */
public interface SwitchLocatorFamily extends LocatorFamily {

    /**
     * Returns the explicit semantic value for this constant.
     * <p>Must be implemented as an exhaustive switch expression so the compiler enforces
     * that every enum constant has a mapping. Never return {@code null} — for optional
     * fallback behaviour, use {@link AdvancedLocatorFamily} instead.</p>
     */
    String getSemanticValue();

    /**
     * Returns {@code [getSemanticValue()]} as the runtime locator argument.
     * No fallback to auto-derivation — the switch covers every constant.
     */
    @Override
    default Object[] getArgs() {
        return new Object[]{getSemanticValue()};
    }
}
