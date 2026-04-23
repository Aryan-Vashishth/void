/**
 * Public surface of the void-framework logger.
 *
 * <p>This top-level package contains only the consumer-facing types:</p>
 * <ul>
 *   <li>{@link core.logging.CustomLogger} — the facade with {@code info/warn/error/debug} entry points</li>
 *   <li>{@link core.logging.ConsoleOnly}  — annotation marking terminal-only features</li>
 * </ul>
 *
 * <p>Internal layering:</p>
 * <pre>
 *   core.logging.ansi    ← ANSI escape primitives + named color catalog
 *   core.logging.intent  ← LogIntent taxonomy
 *   core.logging.theme   ← Theme model, builder, built-in catalog
 *   core.logging.config  ← Live LogConfig + Log4j holder (LoggerContext)
 *   core.logging.render  ← LogActions — renders log lines via Log4j
 * </pre>
 */
package core.logging;

