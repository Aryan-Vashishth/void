/**
 * {@code core.adapters} — Integration adapters for external test frameworks.
 *
 * <p>This package contains adapter layers that wire VOID's core interaction and
 * execution capabilities to external test frameworks. Each sub-package targets
 * a specific integration point.</p>
 *
 * <h3>Sub-packages</h3>
 * <ul>
 *   <li>{@code core.adapters.cucumber} — Cucumber BDD integration. Contains step
 *       definition classes that map Gherkin steps to VOID's
 *       {@link core.interactions.Interactions} layer.
 *       Cucumber is an <b>optional dependency</b> — this package is only loaded
 *       when Cucumber is on the classpath.</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>Adapters are <b>@Internal</b> — they exist for framework plumbing and may
 *       change without notice.</li>
 *   <li>Adapters never contain business logic — they only translate between
 *       VOID's API and the external framework's conventions.</li>
 *   <li>Each adapter sub-package is independently optional — VOID's core
 *       functions without any adapter present.</li>
 * </ul>
 *
 * @see core.interactions.Interactions
 */
package core.adapters;

