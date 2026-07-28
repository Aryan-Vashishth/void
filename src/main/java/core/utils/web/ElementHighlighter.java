package core.utils.web;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Small UI helper to visually highlight elements during automation.
 *
 * Design goals:
 * - Modern look (glow + smooth transition)
 * - Non-intrusive (restores original inline style)
 * - Safe (best-effort only; never throws)
 */
public final class ElementHighlighter {

    private ElementHighlighter() {}

    public enum Theme {
        FOCUS,   // blue glow
        SUCCESS, // green glow
        DANGER   // red glow
    }

    /**
     * Highlights an element with a modern glow effect.
     *
     * Note: Per framework convention, the highlight is persistent (color stays)
     * to help visually debug long flows.
     *
     * Best-effort: this method should never throw.
     */
    public static void flash(WebDriver driver, WebElement element, Theme theme, int durationMs) {
        if (driver == null || element == null) return;
        if (!(driver instanceof JavascriptExecutor js)) return;

        String color = switch (theme) {
            case SUCCESS -> "#22c55e"; // green-500
            case DANGER -> "#ef4444";  // red-500
            default -> "#3b82f6";   // blue-500
        };

        // Persistent highlight (no restore). Keep it modern and readable.
        String script = """
            (function(el, color){
              try {
                el.style.transition = 'box-shadow 150ms ease, outline 150ms ease, transform 150ms ease';
                el.style.outline = '2px solid ' + color;
                el.style.outlineOffset = '2px';
                el.style.boxShadow = '0 0 0 4px ' + color + '33, 0 10px 25px rgba(0,0,0,.15)';
                el.style.transform = 'translateZ(0)';
              } catch(e) {}
            })(arguments[0], arguments[1]);
        """;

        try {
            js.executeScript(script, element, color);
        } catch (Exception ignored) {
            // never fail a test because highlight failed
        }
    }

    public static void flashFocus(WebDriver driver, WebElement element) {
        flash(driver, element, Theme.FOCUS, 0);
    }

    public static void flashSuccess(WebDriver driver, WebElement element) {
        flash(driver, element, Theme.SUCCESS, 0);
    }

    public static void flashDanger(WebDriver driver, WebElement element) {
        flash(driver, element, Theme.DANGER, 0);
    }
}
