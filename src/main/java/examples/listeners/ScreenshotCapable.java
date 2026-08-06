package examples.listeners;

/**
 * Implemented by test classes that can provide a PNG screenshot of the current browser state.
 * {@link ScreenshotListener} calls this on failure so that it stays decoupled from any
 * specific test class.
 */
public interface ScreenshotCapable {

    /**
     * Returns a PNG screenshot of the current browser state, or an empty array if unavailable.
     * Must never throw -- failures are swallowed by the listener so they never mask the
     * original test failure.
     */
    byte[] captureScreenshot();
}
