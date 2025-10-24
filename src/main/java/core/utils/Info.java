package core.utils;

import Elements.InfoElements;
import core.locators.PropertiesFileLocatorReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Configurations.InitialiseBaseTest.*;
import static core.utils.ToolTipsResolver.resolveTooltipByTooltipElements;

public class Info extends BaseUtils {

    public Info(){
        initializer(driver);
    }


    public static class InvalidUserTypeException extends RuntimeException {
        public InvalidUserTypeException(String message) {
            super(message);
        }
    }

    public static String currentUserType() {
        debug.log("[DEBUG] Entered method: currentUserType()");
        try {
            String current = VOID.interaction().getText(InfoElements.CURRENT_USER_TYPE).trim();
            info.log(ANSI_CYAN + "[INFO] Current user type: " + current);
            return current;
        } catch (Exception e) {
            error.log("Failed to fetch current user type from UI." +e);
            throw new RuntimeException("Unable to retrieve current user type.", e);
        }
    }

    public static Boolean isCurrentUserType(String userType) {
        debug.log("[DEBUG] Entered method: isCurrentUserType(userType=" + userType + ")");
        try {
            if (userType == null || userType.trim().isEmpty()) {
                throw new InvalidUserTypeException("User type must not be null or empty.");
            }

            String normalized = userType.trim().toLowerCase();
            switch (normalized) {
                case "admin":
                case "vendor":
                case "distributor":
                case "partner":
                    break;
                default:
                    throw new InvalidUserTypeException("Unsupported user type: '" + userType + "'. Expected one of: Admin, Vendor, Distributor, Partner.");
            }

            String current = currentUserType();
            boolean match = userType.equalsIgnoreCase(current);

            if (match) {
                info.log("[MATCH] User type matches expected: " + userType);
            } else {
                warn.log("[MISMATCH] Expected: " + userType + ", but found: " + current);
            }

            return match;

        } catch (InvalidUserTypeException e) {
            error.log("Invalid user type input: " + userType +e);
            throw e;
        }
    }

    public static String getFullBreadcrumbText() {
        debug.breadcrumb("[DEBUG] Entered method: getFullBreadcrumbText()");
        try {
            List<WebElement> items = driver.findElements(By.xpath("//app-breadcrumb//ul/li"));
            List<String> textParts = new ArrayList<>();
            for (WebElement item : items) {
                String text = item.getText().trim();
                if (!text.isEmpty()) textParts.add(text);
            }
            String breadcrumb = String.join(" > ", textParts);
            info.breadcrumb("Full trail: " + breadcrumb);
            return breadcrumb;
        } catch (Exception e) {
            error.breadcrumb("Failed to fetch full breadcrumb trail." + e);
            return "";
        }
    }

    public static String getCurrentPageViaBreadcrumb() {
        debug.log("[DEBUG] Entered method: getCurrentPage()");
        String full = getFullBreadcrumbText();
        if (full.contains(">")) {
            return full.substring(full.lastIndexOf(">") + 1).trim();
        }
        return full.trim();
    }

    public static String getPreviousPage() {
        debug.log("[DEBUG] Entered method: getPreviousPage()");
        String full = getFullBreadcrumbText();
        String[] parts = full.split(">");
        return (parts.length >= 2) ? parts[parts.length - 2].trim() : "";
    }

    public static class ManageUsersInfo {

        public static class UserCards {
            private final WebElement card;
            private final Map<String, String> info;

            public UserCards(WebElement tile, Map<String, String> info) {
                this.card = tile;
                this.info = info;
            }

            public WebElement card() {
                return card;
            }

            public Map<String, String> info() {
                return info;
            }
        }

        public static UserCards getUserCard(String identifier) { //Need fix
            debug.log("[DEBUG] Entered method: getUserCard(identifier=" + identifier + ")");
            debug.log("[DEBUG] ClassLoader Location: " + ManageUsersInfo.class.getProtectionDomain().getCodeSource().getLocation());
            debug.log("[DEBUG] Executing class: " + ManageUsersInfo.class.getName() + " @ " + System.currentTimeMillis());

            try {
                By by = PropertiesFileLocatorReader.getLocator(
                        ManageUsersElements.UserCards.CONTAINER.getPropertyFile(),
                        ManageUsersElements.UserCards.CONTAINER.getKey(),
                        ManageUsersElements.UserCards.CONTAINER.getArgs()
                );

                debug.log("[DEBUG] Resolved locator for UserCards.CONTAINER: " + by);

                List<WebElement> cards = driver.findElements(by);
                debug.log("[DEBUG] Total user cards found: " + cards.size());

                if (cards.isEmpty()) {
                    error.log("No user cards found. Locator: " + by);
                    throw new RuntimeException("No user cards found for identifier: " + identifier);
                }

                for (WebElement card : cards) {
                    for (ManageUsersElements.UserCards fieldToCheck : List.of(
                            ManageUsersElements.UserCards.EMAIL,
                            ManageUsersElements.UserCards.USERNAME
                    )) {
                        try {
                            // Fully centralized resolution using Vartopia.action().getText()
                            String resolvedValue = VOID.interaction().getText(fieldToCheck).trim();
                            debug.log("[DEBUG] Resolved value from Vartopia.action().getText(): " + resolvedValue);

                            // Optional: fallback to tooltip elements if still unresolved or truncated
                            if (resolvedValue.isEmpty() || resolvedValue.endsWith(fieldToCheck.getEndsWith())) {
                                debug.log("[DEBUG] Value is empty or ends with '" + fieldToCheck.getEndsWith() + "', falling back to tooltip elements.");
                                resolvedValue = resolveTooltipByTooltipElements(identifier, InfoElements.ALL_TOOLTIPS);
                            }

                            // Match validation
                            if (resolvedValue != null && resolvedValue.equalsIgnoreCase(identifier)) {
                                Map<String, String> userInfo = new HashMap<>();
                                for (ManageUsersElements.UserCards element : ManageUsersElements.UserCards.values()) {
                                    WebElement data = card.findElement(PropertiesFileLocatorReader.getLocator(
                                            element.getPropertyFile(),
                                            element.getKey(),
                                            element.getArgs()
                                    ));

                                    String rawText = data.getText().trim();
                                    String resolvedText = rawText.contains("...") ? ToolTipsResolver.resolveTooltipValueFromTruncated(rawText) : rawText;

                                    userInfo.put(element.name(), resolvedText);
                                }

                                info.log("User card resolved for identifier: " + identifier);
                                return new UserCards(card, userInfo);
                            }

                        } catch (Exception e) {
                            warn.log("[WARN] Failed while checking field: " + fieldToCheck.name() + e);
                        }
                    }
                }

            } catch (Exception e) {
                error.log("Unexpected exception in getUserCard for identifier: " + identifier + e);
                throw e;
            }

            try {
                List<WebElement> allTooltips = driver.findElements(PropertiesFileLocatorReader.getLocator(
                        InfoElements.ALL_TOOLTIPS.getPropertyFile(),
                        InfoElements.ALL_TOOLTIPS.getKey(),
                        InfoElements.ALL_TOOLTIPS.getArgs()
                ));
                debug.log("Tooltip fallback inspection: All visible tooltips:");
                for (int i = 0; i < allTooltips.size(); i++) {
                    debug.log("[TOOLTIP #" + i + "] " + allTooltips.get(i).getText().trim());
                }
            } catch (Exception e) {
                warn.log("[WARN] Could not fetch tooltip debug info." + e);
            }

            throw new RuntimeException("No user card found matching: " + identifier);
        }


        public static Map<String, String> getUserInfo(String identifier) {
            debug.log("Entered method: getUserInfo(identifier=" + identifier + ")");
            return getUserCard(identifier).info();
        }
    }
}
