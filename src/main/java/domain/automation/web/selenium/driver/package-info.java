/**
 * {@code domain.automation.web.selenium.driver} -- Selenium WebDriver lifecycle management.
 *
 * <p>Manages the full lifecycle of {@code WebDriver} instances: creation, configuration,
 * thread-local storage, and shutdown. All WebDriver lifecycle concerns are isolated here
 * so the rest of the framework remains engine-agnostic.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link domain.automation.web.selenium.driver.SeleniumDriverFactory} -- fluent
 *       WebDriver builder; creates configured browser instances from a
 *       {@code core.runtime.SessionProfile}. Supports Chrome, Firefox, Edge; local and
 *       remote (Grid/Selenoid); headless, proxy, custom capabilities.</li>
 *   <li>{@link domain.automation.web.selenium.driver.SeleniumDriverContext} -- thread-local
 *       {@code WebDriver} holder; enables safe parallel test execution.</li>
 *   <li>{@link domain.automation.web.selenium.driver.SeleniumDriverManager} -- lifecycle
 *       orchestrator; creates, registers, and quits drivers. Delegates storage to
 *       {@code SeleniumDriverContext}.</li>
 *   <li>{@link domain.automation.web.selenium.driver.Waiter} -- explicit-wait helpers
 *       wrapping {@code WebDriverWait} for common conditions.</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * <p>Driver behaviour is controlled by {@code selenium-webdriver.properties} on the
 * classpath (browser, headless, remote, gridUrl, timeouts, etc.).</p>
 */
package domain.automation.web.selenium.driver;
