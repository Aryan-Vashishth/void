/**
 * {@code core.utils} — Cross-cutting framework utilities.
 *
 * <p>Provides configuration management, enum resolution, UI context state,
 * and organizes domain-specific helpers into focused sub-packages.</p>
 *
 * <h3>Key types (this package)</h3>
 * <ul>
 *   <li>{@link core.utils.ConfigLoader} — hierarchical configuration loader with
 *       resolution order: System properties → environment variables → classpath
 *       files → defaults.</li>
 *   <li>{@link core.utils.ConfigPaths} — constants for standard config file paths
 *       (driver.properties, test.properties, etc.).</li>
 *   <li>{@link core.utils.EnumResolver} — normalised name → enum constant lookup
 *       supporting both standard Java enum names and custom labels via
 *       {@link core.utils.ResolvableEnum}.</li>
 *   <li>{@link core.utils.ResolvableEnum} — mixin interface for enums that provide
 *       a human-readable label in addition to the Java constant name.</li>
 *   <li>{@link core.utils.UIContext} — <i>(deprecated)</i> thread-local holder for
 *       the last resolved {@code LocatorDescriptor}. Retained for legacy
 *       {@code Interactions} compatibility only.</li>
 * </ul>
 *
 * <h3>Sub-packages</h3>
 * <ul>
 *   <li>{@code core.utils.data} — test data generation ({@code DataGenerator}) and
 *       assertion/verification ({@code DataVerifier}) utilities.</li>
 *   <li>{@code core.utils.io} — file-system I/O utilities, JSON readers/loggers,
 *       and properties file readers.</li>
 *   <li>{@code core.utils.web} — browser/DOM utilities: {@code DOMUtils} (JS scroll,
 *       highlight), {@code WaitUtils} (fluent waits, Angular stabilisation),
 *       {@code TableHandler}, {@code KeyValuePairHandler}, {@code Upload}.</li>
 * </ul>
 *
 * @see core.utils.ConfigLoader
 * @see core.utils.EnumResolver
 */
package core.utils;
