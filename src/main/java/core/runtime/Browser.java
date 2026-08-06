package core.runtime;

import domain.automation.web.engine.UIEngine;

/**
 * Session service for browser-level navigation and state.
 *
 * <p>Obtained via {@link VOID#browser()}. All operations are synchronous and
 * execute against the engine that owns this session.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   app.browser().navigateTo("https://example.com");
 *   assertTrue(app.browser().url().contains("/dashboard"));
 * </pre>
 */
public final class Browser {

    private final UIEngine engine;

    Browser(UIEngine engine) {
        this.engine = engine;
    }

    /** Navigates to the given URL. */
    public void navigateTo(String url) {
        engine.navigateTo(url);
    }

    /** Returns the current URL. */
    public String url() {
        return engine.getCurrentUrl();
    }

    /** Returns the page title. */
    public String title() {
        return engine.getTitle();
    }

    /** Reloads the current page. */
    public void refresh() {
        engine.refresh();
    }

    /** Takes a full-page screenshot and returns PNG bytes. */
    public byte[] takeScreenshot() {
        return engine.takeScreenshot();
    }

    /** Clears the browser's localStorage for the current origin. */
    public void clearLocalStorage() {
        engine.executeScript("localStorage.clear()");
    }
}
