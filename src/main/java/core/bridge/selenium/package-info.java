/**
 * {@code core.bridge.selenium} -- temporary Selenium compatibility shims.
 *
 * <p>Contains classes that bridge deprecated Selenium-typed APIs to the engine-neutral
 * model while legacy call sites are migrated. Every class in this package is
 * {@link java.lang.Deprecated @Deprecated(forRemoval = true)}.</p>
 *
 * <h3>Contents</h3>
 * <ul>
 *   <li>{@link core.bridge.selenium.SeleniumLocatorBridge} -- converts Selenium
 *       {@link org.openqa.selenium.By} locators to {@link core.engine.LocatorDescriptor}.
 *       Exists to support the deprecated {@code By}-parameter methods in
 *       {@link core.interactions.Interactions}. Remove together with those methods.</li>
 * </ul>
 *
 * <h3>Usage policy</h3>
 * <p>No new call sites. All classes here are bridge-only. When the deprecated
 * {@code Interactions(WebDriver)} constructor and its associated {@code By}-parameter
 * methods are removed, this package should be deleted entirely.</p>
 *
 * @see core.interactions.Interactions
 * @see core.engine.LocatorDescriptor
 */
package core.bridge.selenium;
