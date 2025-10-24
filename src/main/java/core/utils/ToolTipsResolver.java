package core.utils;

import Elements.InfoElements;
import Elements.Interfaces.ReadOnlyElement;
import Elements.Interfaces.ToolTipElement;
import core.locators.PropertiesFileLocatorReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import static Configurations.InitialiseBaseTest.driver;

import java.util.List;

public class ToolTipsResolver extends BaseUtils {

    public ToolTipsResolver(WebDriver driver){
        initializer(driver);
    }

    public static String resolveTooltipViaRole(WebElement cellElement) {
        try {
            String cellText = cellElement.getText().trim();
            debug.text("Cell text: " + cellText);

            // Return as-is if not truncated
            if (!cellText.endsWith("...")) {
                debug.log("Text is not truncated, returning original value.");
                return cellText;
            }

            String baseText = cellText.substring(0, cellText.length() - 3).trim().toLowerCase();
            debug.log("Truncated base for tooltip match: " + baseText);

            List<WebElement> tooltips = driver.findElements(By.xpath("//*[@role='tooltip']"));

            for (int i = 0; i < tooltips.size(); i++) {
                WebElement tooltip = tooltips.get(i);
                String tooltipText = tooltip.getAttribute("textContent").trim();

                debug.log("[ROLE-TOOLTIP #" + i + "] " + tooltipText);

                if (tooltipText.toLowerCase().startsWith(baseText)) {
                    debug.log("Match found, returning tooltip text: " + tooltipText);
                    return tooltipText;
                }
            }

            debug.failed("No matching tooltip found, returning original truncated text.");
            return cellText;

        } catch (Exception e) {
            error.log("Failed to resolve tooltip via role='tooltip': " + e.getMessage());
            throw new RuntimeException("Tooltip resolution via [role='tooltip'] failed.", e);
        }
    }



    public static String resolveCommonTooltipByHover(WebElement element) {
        try {
            String unresolvedText = element.getText();
            debug.text("Unresolved Text: " + unresolvedText);

            String tooltipText = unresolvedText;

            if (unresolvedText != null && unresolvedText.endsWith("...")) {
                // Use LocatorReader helper to get tooltip locator
                By tooltipLocator = PropertiesFileLocatorReader.getLocator(CommonElements.TooltipsElements.COMMON_TOOLTIP_CONTAINER);

                DOMUtils.hoverOnElement(element);
                debug.log("Hovering on element: " + unresolvedText);

//                wait.until(ExpectedConditions.visibilityOf(driver.findElement(tooltipLocator)));
                // Wait for tooltip text to appear
//                WaitUtils.waitForElementText(tooltipLocator, 10);

                if(WaitUtils.waitForElementTextToBePresent(tooltipLocator)) {
                    tooltipText = VOID.interaction().getText(CommonElements.TooltipsElements.COMMON_TOOLTIP_CONTAINER);
                }else{
                    debug.failed("Tooltip text found to be null");
                }
            }

            debug.log("Resolved tooltip from hover: " + tooltipText);
            return tooltipText != null ? tooltipText.trim() : "";

        } catch (Exception e) {
            error.log("Failed to resolve tooltip by hover on element: " + element.getText());
            throw new RuntimeException("Tooltip resolution by hover failed for element: " + element.getText(), e);
        }
    }


    public static String resolveTooltipByHover(ToolTipElement element) {
        try {
            debug.log("Hovering on element: " + element.getDisplayText());

            WebElement webElement = driver.findElement(PropertiesFileLocatorReader.getLocator(
                    element.getPropertyFile(),
                    element.getKey(),
                    element.getArgs()
            ));

            DOMUtils.hoverOnElement(webElement);

            String tooltipAttr = webElement.getAttribute("title");
            String ariaLabelAttr = webElement.getAttribute("aria-label");

            String resolvedTooltip = tooltipAttr != null && !tooltipAttr.trim().isEmpty() ? tooltipAttr.trim() : ariaLabelAttr;

            debug.log("Resolved tooltip from hover: " + resolvedTooltip);
            return resolvedTooltip;

        } catch (Exception e) {
            error.log("Failed to resolve tooltip by hover on element: " + element.getDisplayText());
            throw new RuntimeException("Tooltip resolution by hover failed for element: " + element.getDisplayText(), e);
        }
    }


    public static String resolveTooltipByTooltipElements(String identifier, ReadOnlyElement tooltipContainer) {
        try {
            debug.log("Resolving tooltip by scanning elements under: " + tooltipContainer.getDisplayText());

            List<WebElement> tooltips = driver.findElements(PropertiesFileLocatorReader.getLocator(
                    tooltipContainer.getPropertyFile(),
                    tooltipContainer.getKey(),
                    tooltipContainer.getArgs()
            ));

            for (int i = 0; i < tooltips.size(); i++) {
                String tooltipText = tooltips.get(i).getText().trim();
                debug.log("[TOOLTIP #" + i + "] " + tooltipText);
                if (tooltipText.equalsIgnoreCase(identifier)) {
                    return tooltipText;
                }
            }

            return null;

        } catch (Exception e) {
            error.log("Failed to resolve tooltip by elements under: " + tooltipContainer.getDisplayText());
            throw new RuntimeException("Tooltip resolution by elements failed for: " + tooltipContainer.getDisplayText(), e);
        }
    }

    public static String resolveTooltipWithHoverFallback(ToolTipElement element, String identifier, ReadOnlyElement tooltipContainer) {
        try {
            // Hover and try resolving via title or aria-label first
            WebElement webElement = driver.findElement(PropertiesFileLocatorReader.getLocator(
                    element.getPropertyFile(),
                    element.getKey(),
                    element.getArgs()
            ));

            String resolvedTooltip = resolveTooltipByHover(element);

            // Check if fallback is needed based on emptiness or element's getEndsWith pattern
            if (resolvedTooltip == null || resolvedTooltip.isEmpty() || resolvedTooltip.endsWith(element.getEndsWith())) {
                debug.log("Tooltip from hover is empty or ends with '" + element.getEndsWith() + "', falling back to tooltip elements.");
                resolvedTooltip = resolveTooltipByTooltipElements(identifier, tooltipContainer);
            }

            return resolvedTooltip;

        } catch (Exception e) {
            error.log("Failed to resolve tooltip with hover fallback for identifier: " + identifier);
            throw new RuntimeException("Tooltip resolution with hover fallback failed for identifier: " + identifier, e);
        }
    }


    public static String resolveTooltipValueFromTruncated(String truncatedText) {
        try {
            List<WebElement> tooltips = driver.findElements(
                    PropertiesFileLocatorReader.getLocator(InfoElements.ALL_TOOLTIPS.getPropertyFile(), InfoElements.ALL_TOOLTIPS.getKey())
            );
            for (WebElement tooltip : tooltips) {
                String tooltipText = tooltip.getText().trim();
                if (tooltipText.toLowerCase().startsWith(truncatedText.toLowerCase().replace("...", ""))) {
                    return tooltipText;
                }
            }
        } catch (Exception e) {
            warn.log("Failed to resolve tooltip for truncated text: " + truncatedText);
        }
        return truncatedText;
    }
}
