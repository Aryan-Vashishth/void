package core.utils;

import Elements.InfoElements;
import Elements.interfacesv1.ReadOnlyElement;
import Elements.interfacesv1.ToolTipElement;
import core.driver.DriverContext;
import core.resolvers.locator.LocatorResolverV1;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static core.logging.CustomLogger.*;

public class ToolTipsResolver extends BaseUtils {

    public ToolTipsResolver(){ initializer(DriverContext.getActiveDriver()); }
    private static WebDriver active() { return DriverContext.getDriver(); }

    public static String resolveTooltipViaRole(WebElement cellElement) {
        try {
            String cellText = cellElement.getText().trim();
            debug.text("Cell text: " + cellText);
            if (!cellText.endsWith("...")) return cellText; // not truncated
            String baseText = cellText.substring(0, cellText.length() - 3).trim().toLowerCase();
            List<WebElement> tooltips = active().findElements(By.xpath("//*[@role='tooltip']"));
            for (WebElement tooltip : tooltips) {
                String tooltipText = tooltip.getAttribute("textContent").trim();
                if (tooltipText.toLowerCase().startsWith(baseText)) return tooltipText;
            }
            return cellText; // fallback
        } catch (Exception e) {
            error.log("Failed role tooltip resolution: " + e.getMessage());
            throw new RuntimeException("Tooltip role resolution failed", e);
        }
    }

    public static String resolveCommonTooltipByHover(WebElement element) {
        try {
            String unresolved = element.getText();
            if (unresolved != null && unresolved.endsWith("...")) {
                By tooltipLocator = LocatorResolverV1.getLocator(InfoElements.ALL_TOOLTIPS);
                DOMUtils.hoverOnElement(element);
                if (WaitUtils.waitForElementTextToBePresent(tooltipLocator)) {
                    return VOID.interaction().getText(InfoElements.ALL_TOOLTIPS).trim();
                }
            }
            return unresolved == null ? "" : unresolved.trim();
        } catch (Exception e) {
            error.log("Hover tooltip failed: " + e.getMessage());
            throw new RuntimeException("Hover tooltip failed", e);
        }
    }

    public static String resolveTooltipByHover(ToolTipElement element) {
        try {
            By by = LocatorResolverV1.getLocator(element);
            WebElement webElement = active().findElement(by);
            DOMUtils.hoverOnElement(webElement);
            String tooltipAttr = webElement.getAttribute("title");
            String ariaLabel = webElement.getAttribute("aria-label");
            return (tooltipAttr != null && !tooltipAttr.isBlank()) ? tooltipAttr.trim() : ariaLabel;
        } catch (Exception e) {
            error.log("Tooltip hover failed for: " + element.getDisplayText());
            throw new RuntimeException("Tooltip hover failed", e);
        }
    }

    public static String resolveTooltipByTooltipElements(String identifier, ReadOnlyElement container) {
        try {
            By c = LocatorResolverV1.getLocator(container);
            for (WebElement el : active().findElements(c)) {
                String txt = el.getText().trim();
                if (txt.equalsIgnoreCase(identifier)) return txt;
            }
            return null;
        } catch (Exception e) {
            error.log("Tooltip scan failed: " + container.getDisplayText());
            throw new RuntimeException("Tooltip scan failed", e);
        }
    }

    public static String resolveTooltipWithHoverFallback(ToolTipElement element, String identifier, ReadOnlyElement container) {
        String tip = resolveTooltipByHover(element);
        if (tip == null || tip.isBlank() || tip.endsWith(element.getEndsWith())) {
            return resolveTooltipByTooltipElements(identifier, container);
        }
        return tip;
    }

    public static String resolveTooltipValueFromTruncated(String truncated) {
        try {
            By allTooltips = LocatorResolverV1.getLocator(InfoElements.ALL_TOOLTIPS);
            for (WebElement el : active().findElements(allTooltips)) {
                String txt = el.getText().trim();
                if (txt.toLowerCase().startsWith(truncated.toLowerCase().replace("...", ""))) return txt;
            }
        } catch (Exception ignored) { warn.log("Failed to resolve tooltip for truncated text: " + truncated); }
        return truncated;
    }
}
