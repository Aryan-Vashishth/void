package CommonUtilMethods;

import HelperClasses.HelperMethods;
import Pages.Common.CommonMethods;
import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class ApplicationCommonElements {
    static WebDriver driver;
    static Logger logger = Logger.getLogger(ApplicationCommonElements.class);

    @FindBy(xpath = "//*[@id=\"maincontainer\"]/div/div[2]/app-root/app-breadcrumb/div/ul")
    static WebElement Breadcrumbs;

    public ApplicationCommonElements(WebDriver driver){
        this.driver = driver;
        PageFactory.initElements(driver, this);
        logger.info("Initialising Application Common Elements web elements");
    }

    public static boolean isBreadCrumbDisplayed() {
        logger.info("checking Breadcrumb in header");
        return HelperMethods.waitUntilElementIsvisiblity(driver,Breadcrumbs);
    }
}
