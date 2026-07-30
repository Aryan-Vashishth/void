/**
 * Engine abstraction layer for VOID framework.
 *
 * <p>This package defines the execution contract ({@link core.engine.UIEngine}) that
 * decouples VOID's interaction layer from any specific browser automation library.
 * Engine implementations (Selenium, Playwright, etc.) live in sub-packages.</p>
 *
 * <h3>Key types</h3>
 * <ul>
 *   <li>{@link core.engine.UIEngine} — the execution interface</li>
 *   <li>{@link core.engine.LocatorDescriptor} — engine-agnostic locator representation</li>
 *   <li>{@link core.engine.LocatorStrategy} — open locator strategy token (XPATH, CSS, ID, NAME + extensible)</li>
 *   <li>{@link core.engine.EngineConfig} — engine initialization parameters</li>
 * </ul>
 */
package core.engine;

