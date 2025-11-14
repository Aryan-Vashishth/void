//package StepDefinition.accountMappingStepDefPack;
//
//import Configurations.InitialiseBaseTest;
//import Elements.*;
//import HelperClasses.HelperMethods;
////import com.applitools.eyes.selenium.Eyes;
//import io.cucumber.java.After;
//import io.cucumber.java.Before;
//import io.cucumber.java.Scenario;
//import io.cucumber.java.en.And;
//import io.cucumber.java.en.Then;
//import org.openqa.selenium.By;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.WebElement;
//import org.testng.Assert;
//import core.utils.CaptureScreenShot;
//import core.utils.ConfigLoader;
//import core.utils.DOMUtils;
//import core.driver.DriverContext;
//import core.driver.DriverFactory;
//import core.utils.DataGenerator;
//import core.utils.json.JsonLogger;
//import core.resolvers.locators.PropertiesFileLocatorReader;
//import core.logging.CustomLogger;
//import core.utils.KeyValuePairHandler;
//import core.utils.TableHandler;
//import core.utils.Upload;
//import core.utils.WaitUtils;
//
//import java.nio.file.Path;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//import static core.driver.DriverContext.quitAllDrivers;
//
//
//public class AccountMappingStepDef extends InitialiseBaseTest {
//
////    Eyes eyes;
//
//    @Before
//    public void setup(Scenario scenario) {
//            initialSetup();
//        try {
//            driver.manage().window().maximize();
//        }catch (Exception ignored){warn.failed("Failed to change window state");}
//
//        CustomLogger.initialize(this.getClass());
//        new DOMUtils().initializer(driver);
//
//
//        Properties cfg = ConfigLoader.Layered.builder()
//                .addClasspath("config/driver.properties", false)
//                .addClasspath("config/test.properties", false)
//                // let -Dconfig.file or ENV CONFIG_FILE point to an extra override file if you want
//                .externalOverrideKeys("config.file", "CONFIG_FILE")
//                // allow System/ENV to override last
//                .includeSystemProperties(true)
//                .includeEnvironment(true)
//                .build();
//        ConfigLoader.setActive(cfg);
//        debug.log("Config active. Keys loaded: " + cfg.size());
//        debug.log("account.mapping.fallback.path = " +
//                ConfigLoader.get("account.mapping.fallback.path", "<missing>"));
//        info.success("Test setup completed.");
////        eyes = new Eyes();
////        eyes.setApiKey("api key");
////        eyes.open(driver, "Your App Name", scenario.getName());
//    }
//
//    @After
//    public void tearDown(Scenario scenario) {
//        if (scenario.isFailed()) {
//            error.failed("Scenario failed. Capturing screenshot...");
//            new CaptureScreenShot(driver).takeScreenShotBase64(scenario);
//        }
////        try {
////            if (eyes != null) {
////                eyes.closeAsync();
////            }
////        } finally {
////            if (eyes != null) {
////                eyes.abortIfNotClosed();
////            }
////        }
//        softAssert.assertAll();
//        quitAllDrivers();
////         driver.quit();
//        info.complete("Test execution completed.");
//    }
//
//
//    @And("User Insert records in table from import Records")
//    public void userInsertRecordsInTableFromImportRecords() {
//
//        List<String> expectedColumns = TableHandler.getColumnHeaders(AccountMappingElements.ImportRecordsPopup.importRecordsTable.IMPORT_RECORDS_TABLE);
//        Map<String, CommonElements.FieldType> fieldTypeMap = DataGenerator.toFieldTypeMap(expectedColumns);
//
//        Map<String, String> expectedFirstRowData = DataGenerator.generateTestData(fieldTypeMap, "account.mapping.fallback.path");
//
//        TableHandler.insertRowInTable(expectedFirstRowData, AccountMappingElements.ImportRecordsPopup.importRecordsTable.IMPORT_RECORDS_TABLE);
//
////        driver.switchTo().defaultContent();
//
//        JsonLogger.Write.MapWriter.writeRowList(null, "last-inserted-open-records", List.of(expectedFirstRowData));
//    }
//
//    @Then("User uploads following file in Import Records popup {string}")
//    public void userUploadsFollowingFileInImportRecordsPopup(String fileName) {
//        info.upload("Uploading file: " + fileName);
//        try {
//            Upload.uploadFile(
//                    AccountMappingElements.ImportRecordsPopup.uploadField.DROP_FILES_HERE_OR_CLICK_TO_UPLOAD,
//                    fileName
//            );
//
//            boolean isValidFile = fileName.endsWith(".xlsx") || fileName.endsWith(".xls") || fileName.endsWith(".csv");
//            By errorLocator = PropertiesFileLocatorReader.getLocator(
//                    AccountMappingElements.ImportRecordsPopup.UploadFieldErrorMessage.YOU_CAN_ONLY_SELECT_CSV_OR_EXCEL_FILE
//            );
//
//            if (isValidFile) {
//                // Assert error message is ABSENT for valid files
//                boolean errorAbsent = WaitUtils.waitForElementToBeAbsent(errorLocator, 3);
//                if (!errorAbsent) {
//                    String errorMessage = vartopia.interaction().getText(
//                            AccountMappingElements.ImportRecordsPopup.UploadFieldErrorMessage.YOU_CAN_ONLY_SELECT_CSV_OR_EXCEL_FILE
//                    );
//                    softAssert.fail("Unexpected error for valid file: " + fileName + " - " + errorMessage);
//                    debug.error("Unexpected error for file: " + errorMessage);
//                } else {
//                    info.upload("Valid file uploaded with no error, as expected: " + fileName);
//                }
//            } else {
//                // For INVALID files, assert error message is present
//                boolean errorPresent = WaitUtils.waitForElementTextToBePresent(errorLocator, 1);
//                String errorMessage;
//                if (errorPresent) {
//                    errorMessage = vartopia.interaction().getText(
//                            AccountMappingElements.ImportRecordsPopup.UploadFieldErrorMessage.YOU_CAN_ONLY_SELECT_CSV_OR_EXCEL_FILE
//                    );
//                    info.validation("Correct error shown for invalid file: " + fileName + " - " + errorMessage);
//                } else {
//                    softAssert.fail("No error message shown for invalid file: " + fileName);
//                    debug.error("Expected error message missing for file: " + fileName);
//                }
//            }
//
//        } catch (Exception outerCatch) {
//            warn.upload("Failed to upload: " + fileName);
//            debug.upload(outerCatch.getMessage());
//            try {
//                info.text("Current Popup view Heading: " + vartopia.interaction().getText(
//                        AccountMappingElements.ImportRecordsPopup.popupViewsHeaders.IMPORT_RECORDS
//                ));
//                Assert.fail(outerCatch.getMessage());
//            } catch (Exception lastCatch) {
//                debug.wait(lastCatch.getMessage());
//            }
//        }
//    }
//
//    @Then("User should be able to upload valid file {string} in Import Records popup")
//    public void userShouldBeAbleToUploadValidFileInImportRecordsPopup(String fileName) {
//        info.upload("Uploading valid file: " + fileName);
//        try {
//            Upload.uploadFile(
//                    AccountMappingElements.ImportRecordsPopup.uploadField.DROP_FILES_HERE_OR_CLICK_TO_UPLOAD,
//                    fileName
//            );
//
//            By errorLocator = PropertiesFileLocatorReader.getLocator(
//                    AccountMappingElements.ImportRecordsPopup.UploadFieldErrorMessage.YOU_CAN_ONLY_SELECT_CSV_OR_EXCEL_FILE
//            );
//
//            // Assert error message is ABSENT for valid files
//            boolean errorAbsent = WaitUtils.waitForElementToBeAbsent(errorLocator, 3);
//            if (!errorAbsent) {
//                String errorMessage = vartopia.interaction().getText(
//                        AccountMappingElements.ImportRecordsPopup.UploadFieldErrorMessage.YOU_CAN_ONLY_SELECT_CSV_OR_EXCEL_FILE
//                );
//                Assert.fail("Unexpected error for valid file: " + fileName + " - " + errorMessage);
//            } else {
//                info.upload("Valid file uploaded successfully, as expected: " + fileName);
//            }
//        } catch (Exception e) {
//            warn.upload("Failed to upload valid file: " + fileName);
//            Assert.fail(e.getMessage());
//        }
//    }
//
//    @Then("User should see error when uploading invalid file {string} in Import Records popup")
//    public void userShouldSeeErrorWhenUploadingInvalidFileInImportRecordsPopup(String fileName) {
//        info.upload("Uploading invalid file: " + fileName);
//        try {
//            Upload.uploadFile(
//                    AccountMappingElements.ImportRecordsPopup.uploadField.DROP_FILES_HERE_OR_CLICK_TO_UPLOAD,
//                    fileName
//            );
//
//            By errorLocator = PropertiesFileLocatorReader.getLocator(
//                    AccountMappingElements.ImportRecordsPopup.UploadFieldErrorMessage.YOU_CAN_ONLY_SELECT_CSV_OR_EXCEL_FILE
//            );
//
//            // Assert error message is PRESENT for invalid files
//            boolean errorPresent = WaitUtils.waitForElementTextToBePresent(errorLocator, 3);
//            if (errorPresent) {
//                String errorMessage = vartopia.interaction().getText(
//                        AccountMappingElements.ImportRecordsPopup.UploadFieldErrorMessage.YOU_CAN_ONLY_SELECT_CSV_OR_EXCEL_FILE
//                );
//                info.validation("Correct error shown for invalid file: " + fileName + " - " + errorMessage);
//            } else {
//                Assert.fail("No error message shown for invalid file: " + fileName);
//            }
//        } catch (Exception e) {
//            warn.upload("Failed to upload invalid file: " + fileName);
//            Assert.fail(e.getMessage());
//        }
//    }
//
//
//
//
//
//
//
//    @And("Wait for Import Records popup to disappear")
//    public void waitForImportRecordsPopupToDisappear() {
//        WaitUtils.waitForElementToDisappear(AccountMappingElements.ImportRecordsPopup.Div.IMPORTS_RECORDS_OVERLAY);
//        WaitUtils.resolveAngularLoader();
//    }
//
//
//    @And("User search for vartopia")
//    public void userSearchForVartopia() {
//        WebElement element = vartopia.interaction().getSearchedElement(NewRegistrationElements.PartnerInformation.SELECT_YOUR_PARTNER, "vartopia");
//        vartopia.interaction().clickOn(element);
//    }
//
//    @And("Temp 1")
//    public void temp1() {
//        // vartopia.interaction().clickOn(RecordsPageElements.AccountMapping.ThreeDotsMenu.ViewRegistrationDialogBox.OtherClickableElements.CLOSE);
//        Path propertiesPath = DriverFactory.createDefaultTemplate(true);
//        DriverContext.setSecondaryDriver(DriverFactory.fromConfigFile(propertiesPath).build());
//        WebDriver window2 = DriverContext.getSecondaryDriver();
//        window2.get("https://www.google.com/");
//        HelperMethods.fixedwait(1000);
//        DriverContext.quitAllDrivers();
//    }
//
//
//    @And("Temp 2")
//    public void temp2() {
//
//        Map<String, String> actualDetails = KeyValuePairHandler.collectAll(RecordsPageElements.AccountMapping.ThreeDotsMenu.ViewRegistrationDialogBox.class);
//        JsonLogger.Write.MapWriter.writeRowList(null, "last-actual-pending-records", List.of(actualDetails));
//
//    }
//}
