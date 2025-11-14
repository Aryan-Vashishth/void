package core.utils;

import Elements.ManageUsersElements;
import Elements.interfacesv1.ReadOnlyElement;
import core.driver.DriverContext;
import core.resolvers.locator.LocatorResolverV1;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static core.logging.CustomLogger.*;

/**
 * Info utilities (v1 only) supplying breadcrumb and simple page/user info access.
 */
public class Info extends BaseUtils {

    public Info(){ initializer(); }

    public static String getFullBreadcrumbText() {
        try {
            List<WebElement> items = DriverContext.getDriver().findElements(By.xpath("//app-breadcrumb//ul/li"));
            List<String> parts = new ArrayList<>();
            for (WebElement item : items) {
                String text = item.getText().trim();
                if (!text.isEmpty()) parts.add(text);
            }
            String breadcrumb = String.join(" > ", parts);
            info.breadcrumb("Full trail: " + breadcrumb);
            return breadcrumb;
        } catch (Exception e) {
            error.breadcrumb("Failed to fetch full breadcrumb trail." + e);
            return "";
        }
    }

    public static String getCurrentPageViaBreadcrumb() {
        String full = getFullBreadcrumbText();
        if (full.contains(">")) return full.substring(full.lastIndexOf(">") + 1).trim();
        return full.trim();
    }

    public static String getPreviousPage() {
        String full = getFullBreadcrumbText();
        String[] parts = full.split(">");
        return (parts.length >= 2) ? parts[parts.length - 2].trim() : "";
    }

    public static class ManageUsersInfo {
        public static java.util.Map<String,String> getUserInfo(String identifier){
            java.util.Map<String,String> out = new java.util.LinkedHashMap<>();
            try {
                // Find all user card containers
                org.openqa.selenium.By containerBy = LocatorResolverV1.getLocator(ManageUsersElements.UserCards.CONTAINER);
                java.util.List<org.openqa.selenium.WebElement> cards = DriverContext.getDriver().findElements(containerBy);
                for (org.openqa.selenium.WebElement card : cards) {
                    // Match identifier by email or username text
                    String email = safeChildText(card, ManageUsersElements.UserCards.EMAIL);
                    String username = safeChildText(card, ManageUsersElements.UserCards.USERNAME);
                    if (identifier.equalsIgnoreCase(email) || identifier.equalsIgnoreCase(username)) {
                        out.put("FULL_NAME", safeChildText(card, ManageUsersElements.UserCards.FULL_NAME));
                        out.put("USERNAME", username);
                        out.put("EMAIL", email);
                        out.put("COMPANY", safeChildText(card, ManageUsersElements.UserCards.COMPANY));
                        out.put("USER_TYPE", safeChildText(card, ManageUsersElements.UserCards.USER_TYPE));
                        return out;
                    }
                }
                warn.log("[ManageUsersInfo] No matching user card for identifier: " + identifier);
            } catch (Exception e){
                error.log("[ManageUsersInfo] Failed to fetch user info for: " + identifier + " " + e.getMessage());
            }
            return out;
        }
        private static String safeChildText(org.openqa.selenium.WebElement card, ManageUsersElements.UserCards field){
            try {
                org.openqa.selenium.By by = LocatorResolverV1.getLocator(field);
                org.openqa.selenium.WebElement el = card.findElement(by);
                return el.getText()==null?"":el.getText().trim();
            } catch (Exception ignored){ return ""; }
        }
    }
}
