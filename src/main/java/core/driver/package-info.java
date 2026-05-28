/**
 * {@code core.driver} — Selenium WebDriver lifecycle management and waiting utilities.
 *
 * <p>Manages the complete WebDriver lifecycle: creation, configuration, thread-local
 * storage, and shutdown. All driver concerns are isolated here to keep the rest of
 * the framework engine-agnostic.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link core.driver.DriverFactory} — fluent WebDriver builder supporting Chrome,
 *       Firefox, Edge, headless mode, remote/grid execution, mobile emulation, proxy,
 *       and custom capabilities. Configured entirely via {@code driver.properties}.</li>
 *   <li>{@link core.driver.DriverContext} — thread-local driver holder. Provides
 *       static access to the active driver for the current thread.</li>
 *   <li>{@link core.driver.DriverManager} — isolated lifecycle manager that creates
 *       drivers via {@code DriverFactory}, registers them in {@code DriverContext},
 *       and handles shutdown/cleanup.</li>
 *   <li>{@link core.driver.Waiter} — explicit-wait helpers for common conditions
 *       (visibility, clickability, presence, etc.).</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * <p>Driver behavior is controlled by {@code driver.properties} on the classpath:</p>
 * <pre>
 *   browser=chrome           # chrome | firefox | edge
 *   headless=false
 *   remote=false
 *   gridUrl=                 # Selenium Grid URL (when remote=true)
 *   maximize=true
 *   implicitWait=5
 *   pageLoadTimeout=60
 *   pageLoadStrategy=NORMAL  # NORMAL | EAGER | NONE
 * </pre>
 *
 * <h3>Thread safety</h3>
 * <p>All classes are thread-safe. {@code DriverContext} uses {@code ThreadLocal}
 * storage, enabling safe parallel test execution.</p>
 *
 * @see core.driver.DriverFactory
 * @see core.driver.DriverContext
 * @see core.driver.DriverManager
 * @see core.driver.Waiter
 */
package core.driver;
