/**
 * Automation layer — BDD / Cucumber-specific code.
 *
 * <p>Everything in this package tree <em>depends on</em> the framework layer
 * ({@code core}, {@code elements}, {@code interactions}, {@code WebApplication})
 * but the framework layer never imports from here. This enforces a strict
 * one-way dependency: automation → framework.</p>
 *
 * <h3>Packages</h3>
 * <ul>
 *   <li>{@code automation.interactions} — {@code StepDefInteractions}: high-level
 *       Cucumber step helpers that translate plain-text BDD parameters into
 *       typed enum actions via {@code EnumClassRegistry}.</li>
 *   <li>{@code automation.WebApplication} — {@code AutomationVOID}: the automation-layer
 *       façade that extends the framework's {@code VOID} with
 *       {@code stepDefInteraction()}.</li>
 * </ul>
 *
 * <h3>Dependency diagram</h3>
 * <pre>
 *  StepDefinition.*       (Cucumber glue / @Step methods)
 *       │
 *       ▼
 *  AutomationVOID         (automation façade)
 *       │  extends
 *       ▼
 *  VOID                   (framework façade — NO BDD imports)
 *       │
 *       ├──▶ Interactions          (raw UI actions)
 *       │         │
 *       │         └──▶ core.*      (driver / logging / resolvers)
 *       │
 *       └──▶ elements.*            (element contracts / registry)
 * </pre>
 */
package automation;

