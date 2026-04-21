//package core.interactions;
//
//import WebApplication.VOID;
//import core.driver.DriverContext;
//import core.driver.DriverFactory;
//import elements.PracticePageElements;
//import interactions.Via;
//import org.openqa.selenium.By;
//import org.openqa.selenium.Keys;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.WebElement;
//
//public class interactions {
//
//    static WebDriver driver;
//    static VOID VOID;
//
//    public static void main(String[] args) throws InterruptedException {
//
//        driver = DriverFactory.builder()
//                .browser(DriverFactory.Browser.CHROME)
//                .maximize(true)
//                .addArg("--no-sandbox")
//                .addArg("--disable-dev-shm-usage")
//                .addArg("--disable-blink-features=AutomationControlled")
//                .addArg("--remote-allow-origins=*")
//                .addArg("--proxy-server=direct://")
//                .addArg("--proxy-bypass-list=*")
//                .build();
//        DriverContext.setPrimaryDriver(driver);
//        VOID = new VOID();   // must be created AFTER the driver is registered in DriverContext
//        driver.get("https://lincoln.vartopia.com/Login");
//
//        // ── Original: click a checkbox ──────────────────────────────────────
//
//        // ── Via: cast-safety checks ──────────────────────────────────────────
//        // DayCheckboxes implements Checkbox (which extends Clickable) → safe cast
//        System.out.println("Is clickable? " + Via.isClickable(PracticePageElements.DayCheckboxes.CHECKBOX_FRIDAY));   // true
//        System.out.println("Is text input? " + Via.isTextInput(PracticePageElements.FormInputs.NAME_INPUT));           // true
//
//        // ── Via: resolve a By locator directly from an element descriptor ────
//        By nameInputLocator = Via.locator(PracticePageElements.FormInputs.NAME_INPUT);
//        By submitLocator    = Via.locator(PracticePageElements.FormButtons.SUBMIT_BUTTON);
//        System.out.println("Name input locator  : " + nameInputLocator);
//        System.out.println("Submit btn locator  : " + submitLocator);
//
//        // ── Via: find a live WebElement (waits for visibility) ───────────────
//        WebElement nameField = Via.webElement(driver, PracticePageElements.FormInputs.NAME_INPUT);
//
//        // ── Via + typeInto: type into FormInputs using the framework helper ──
//        VOID.interaction().typeInto(PracticePageElements.FormInputs.NAME_INPUT,  "Jane Doe");
//        VOID.interaction().typeInto(PracticePageElements.FormInputs.PHONE_INPUT, "555-0100");
//        VOID.interaction().typeInto(PracticePageElements.FormInputs.EMAIL_INPUT, "jane@example.com");
//
//        // ── Via + typeIntoAndPress: type and hit ENTER on the key-press input ─
//        VOID.interaction().typeIntoAndPress(
//                PracticePageElements.KeyPressArea.KEY_PRESS_INPUT, "Hello", Keys.ENTER);
//
//        // ── Via + getText: read the page heading via ReadOnlyElement ─────────
//        String heading = VOID.interaction().getText(PracticePageElements.PageHeader.PAGE_HEADING);
//        System.out.println("Page heading: " + heading);
//
//        // ── Via: click Submit using a resolved locator (low-level path) ──────
//        VOID.interaction().clickOn(submitLocator);
//    }
//
//}
