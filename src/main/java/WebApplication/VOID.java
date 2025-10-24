package WebApplication;

import Configurations.InitialiseBaseTest;
import core.driver.DriverContext;
import interactions.*;
import org.openqa.selenium.WebDriver;
import core.utils.Info;

public class VOID {

    private final WebDriver driver;

//    public VOID() {
////        DriverContext.setDriver(InitialiseBaseTest.getDriver());
////        driver = DriverContext.getDriver();
//        driver = InitialiseBaseTest.getDriver();
//    }

        public VOID() {
            driver = DriverContext.getActiveDriver();
        }

    // ===========================
    //         Interactions
    // ===========================

    public Interactions interaction() {
        return new Interactions(driver);
    }

    public StepDefInteractions stepDefInteraction() {
        return new StepDefInteractions(driver);
    }


    public Info info() {
        return new Info();
    }
}
