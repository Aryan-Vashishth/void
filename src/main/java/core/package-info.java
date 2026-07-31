/**
 * Core framework infrastructure — the backbone of the VOID automation platform.
 *
 * <p>This package contains all fundamental subsystems: the deferred execution model,
 * engine abstraction, driver management, locator resolution, hook pipelines,
 * logging, configuration, and general-purpose utilities.</p>
 *
 * <h3>Sub-package map</h3>
 * <ul>
 *   <li>{@code core.actions}        — Deferred execution model: {@code Action}, {@code ElementActions}, {@code HookedAction}</li>
 *   <li>{@code core.adapters}       — Integration adapters (Cucumber BDD)</li>
 *   <li>{@code core.annotations}    — Stability-tier markers: {@code @Beta}, {@code @Internal}</li>
 *   <li>{@code core.bootstrap}      — One-time framework initialisation ({@code FrameworkBootstrap})</li>
 *   <li>{@code core.context}        — Per-session execution context ({@code ExecutionContext}, {@code SessionContext})</li>
 *   <li>{@code core.driver}         — WebDriver lifecycle: {@code SeleniumDriverFactory}, {@code SeleniumDriverContext}, {@code SeleniumDriverManager}</li>
 *   <li>{@code core.engine}         — Engine abstraction: {@code UIEngine}, {@code LocatorDescriptor}, {@code SeleniumEngine}</li>
 *   <li>{@code core.executor}       — Flow execution: {@code FlowExecutor}</li>
 *   <li>{@code core.flow}           — Declarative action composition: {@code Flow}</li>
 *   <li>{@code core.interactions}   — Legacy orchestrator ({@code Interactions}), hooks ({@code Before}/{@code After}), and helpers</li>
 *   <li>{@code core.logging}        — ANSI-colored, theme-aware logger: {@code CustomLogger}, themes, intents, rendering</li>
 *   <li>{@code core.resolvers}      — Role-based locator resolution pipeline (JSON, properties, templates)</li>
 *   <li>{@code core.runtime}        — Framework entry point: {@code VOID} façade and session lifecycle</li>
 *   <li>{@code core.utils}          — Cross-cutting utilities: configuration, DOM helpers, data generation, I/O</li>
 * </ul>
 *
 * <h3>Primary execution path</h3>
 * <pre>
 *   UIElement → Action (intent) → Flow (composition) → FlowExecutor (iteration) → UIEngine (execution)
 * </pre>
 *
 * <h3>Legacy execution path (frozen)</h3>
 * <pre>
 *   UIElement → Interactions (orchestrator) → UIEngine (execution)
 * </pre>
 */
package core;

