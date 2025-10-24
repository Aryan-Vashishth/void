package StepDefinition.accountMappingStepDefPack;//package StepDefinition.AccountMappingDefPack;
//
//import interactions.*;
//import Elements.AdminHomeElements;
//import Elements.CommonElements;
//import HelperClasses.HelperMethods;
//import interactions.AccountMappingInteractions;
//import interactions.Interactions;
//import Pages.*;
//import Configurations.InitialiseBaseTest;
//import Pages.AccountMappingPages.AccountMappingHomePage;
//import Pages.Admin.AdminHomeInteractions;
//import core.utils.WaitUtils;
//import io.cucumber.java.Before;
//import io.cucumber.java.en.*;
//import org.openqa.selenium.By;
//import org.testng.Assert;
//
//import java.util.*;
//
//import static Pages.Common.CommonMethods.*;
//import static Pages.Common.CommonMethods.getTableCellValue;
//
//public class AccountMappingOldDef extends InitialiseBaseTest {
//    String quickSearchRecordIdValue;
//    String quickSearchAccountNameValue;
//    String quickSearchWebsiteValue;
//    List<String> quickSearchValues;
//    private final ArrayList<String> recordIds = new ArrayList<>();
//    ManageUsersInteractions manageUsersAction;
//    AdminHomeInteractions adminHomeAction;
//    Interactions action;
//    RecordsGridInteractions recordsGridAction;
//    AccountMappingInteractions accountMappingAction;
//
//
//    @Before
//    public void setup() {
//        initialSetup();
//        action = new Interactions(driver);
//        manageUsersAction = new ManageUsersInteractions(driver);
//        adminHomeAction = new AdminHomeInteractions(driver);
//        recordsGridAction = new RecordsGridInteractions(driver);
//        accountMappingAction = new AccountMappingInteractions(driver);
//    }
//
//    @Given("User login with valid email {string}")
//    public void userLoginWithValidUsername(String email) {
//        adminHomeAction.clickOn(AdminHomeElements.Tiles.MANAGE_USERS);
//        log.debug(ANSI_BLACK_BG_WHITE_TEXT + "[DEBUG] Entered method: loginWithEmail(email=" + email + ")" + ANSI_RESET);
////        manageUsersAction.loginUsing(ManageUsersElements.FilterByTextFieldElement.EMAIL, ManageUsersElements.UserCards.EMAIL, email, null);
////        manageUsersAction.loginWithEmail(username, null);
////        log.info("User logged in with username: " + username);
//        manageUsersAction.loginWithEmail(email);
//    }
//
//    @And("Land on Account Mapping home page")
//    public void landOnAccountMappingHomePage() {
////        objectFactory.getAccountMappingHomePage().isLogoVisible();
////        objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
////        commonMethods.switchBranding("vartopia hub");
//        action.clickOn(CommonElements.NavigationBar.ACCOUNT_MAPPING, Interactions.After.WAIT_FOR_ANGULAR_LOADER);
//
//    }
//
//    @Then("Verified all elements on home page are visible and functional based on user type {string}")
//    public void verifyAllElementsAreOnAccountMappingHomePage(String userType) {
//
//        try {
//            // Verify Bread Crumbs in Account Mapping home page
//            String breadcrumbText = objectFactory.getCommonMethods().getBreadcrumbText();
//            if (breadcrumbText.equalsIgnoreCase("Home > Account Mapping")) {
//                log.info(ANSI_GREEN + "Correct bread crumb text is displayed" + ANSI_RESET);
//            } else {
//                log.info(ANSI_RED + "Incorrect bread crumb text is displayed" + ANSI_RESET);
//                softAssert.fail(ANSI_RED + "Correct bread crumb text should be displayed" + ANSI_RESET);
//            }
//        } catch (Exception e) {
//            log.warn(ANSI_RED + "Error while verifying Bread Crumb" + ANSI_RESET);
//        } finally {
//            softAssert.assertAll();
//        }
//        // Verify Import Records button visibility based on user type
//        objectFactory.getAccountMappingHomePage().isImportRecordsButtonVisible(userType);
//
//        try {
//            // Verify logo visibility
//            try {
//                if (objectFactory.getAccountMappingHomePage().isLogoVisible()) {
//                    log.info(ANSI_GREEN +
//                            "Vartopia Hub logo is visible." +
//                            ANSI_RESET);
//                } else {
//                    log.warn(ANSI_YELLOW +
//                            "Vartopia Hub logo is NOT visible." +
//                            ANSI_RESET);
//                    softAssert.fail(ANSI_YELLOW +
//                            "Vartopia Hub logo should be visible."
//                            + ANSI_RESET);
//                }
//            } catch (Exception e) {
//                log.error(ANSI_RED +
//                        "Error while verifying logo visibility." +
//                        ANSI_RESET, e);
//                softAssert.fail(ANSI_RED +
//                        "Logo visibility verification failed due to an error." +
//                        ANSI_RESET);
//            }
//
//            // Verify Quick Search elements
//            try {
//                objectFactory.getAccountMappingHomePage().verifyQuickSearchElements(softAssert);
//                log.info(ANSI_GREEN +
//                        "Quick Search elements verified successfully." +
//                        ANSI_RESET);
//            } catch (Exception e) {
//                log.error(ANSI_RED +
//                        "Error while verifying Quick Search elements." +
//                        ANSI_RESET, e);
//                softAssert.fail(ANSI_RED +
//                        "Quick Search elements verification failed due to an error." +
//                        ANSI_RESET);
//            }
//
//            // Verify Default Report buttons
//            try {
//                objectFactory.getAccountMappingHomePage().verifyDefaultReportsElements(softAssert, userType);
//                log.info(ANSI_GREEN +
//                        "Default Reports buttons verified successfully." +
//                        ANSI_RESET);
//            } catch (Exception e) {
//                log.error(ANSI_RED +
//                        "Error while verifying Default Reports buttons." +
//                        ANSI_RESET, e);
//                softAssert.fail(ANSI_RED +
//                        "Default Reports buttons verification failed due to an error." +
//                        ANSI_RESET);
//            }
//        } finally {
//            // Assert all soft assertions
//            softAssert.assertAll();
//        }
//    }
//
//    @And("Verified all elements on records page are visible and functional based on user type {string}")
//    public void verifiedAllElementsOnRecordsPageAreVisibleAndFunctionalBasedOnUserType(String userType) {
//        objectFactory.getAccountMappingHomePage().clickDefaultReport("Open Accounts");
//        log.info(AccountMappingHomePage.ANSI_YELLOW + "Getting values for Record Id, Account Name and Website" + AccountMappingHomePage.ANSI_RESET);
////        quickSearchValues = recordsGridAction.getRowValues(CommonElements.RECORDS_GRID.DEFAULT, null, Set.of("Record Id , Account Name, Website"));
//        quickSearchRecordIdValue = getTableCellValue("record id");
//        quickSearchAccountNameValue = getTableCellValue("account name");
//        quickSearchWebsiteValue = getTableCellValue("website");
//
//        try {
//            // Verify Bread Crumbs in records page
//            String breadcrumbText = objectFactory.getCommonMethods().getBreadcrumbText();
//            if (breadcrumbText.equalsIgnoreCase("Home > Account Mapping > Records")) {
//                log.info(ANSI_GREEN + "Correct bread crumb text is displayed" + ANSI_RESET);
//            } else {
//                log.info(ANSI_RED + "Incorrect bread crumb text is displayed" + ANSI_RESET);
//                softAssert.fail(ANSI_RED + "Correct bread crumb text should be displayed" + ANSI_RESET);
//            }
//
//            // New Registration option should be enabled when the status is Open (Record > 3 Dots)
//            ArrayList<String> expectedElementNamesIn3DotsButtonOnRecord = AccountMappingHomePage.getExpectedElementNamesIn3DotsButtonOnRecord(userType);
//            clickThreeDotsButtonOnFirstRecord();
//            List<String> actualElementNamesIn3DotsButtonOnRecord = getElementNamesIn3DotsButtonOnRecord();
//
//            if (HelperMethods.compareLists(expectedElementNamesIn3DotsButtonOnRecord, actualElementNamesIn3DotsButtonOnRecord)) {
//                log.info(ANSI_GREEN + "Elements In 3 Dots Button On Record are as expected" + ANSI_RESET);
//            } else {
//                Assert.fail(ANSI_RED + "Elements In 3 Dots Button On Record are NOT as expected" + ANSI_RESET);
//            }
//
//            // Click on Group By > Drop-down should contain following options: Country, Geo Location, HQ State/Province, Program Name, Status (and also "Vendor" for partner)
////            commonMethods.clickGroupByButtonOnRecordsPage();
////
////            List<WebElement> actualGroupByElements = driver.findElements(By.xpath("//*[@id='mat-menu-panel-3']//button"));
////
////            ArrayList<String> expectedGroupByElementNames = accountMappingHomePage.getExpectedGroupByElementNames(userType);
////            ArrayList<String> actualGroupByElementNames = new ArrayList<>();
////
////            for(WebElement actualGroupByElement: actualGroupByElements){
////                actualGroupByElementNames.add(actualGroupByElement.getText());
////            }
////
////            commonMethods.clickGroupByButtonOnRecordsPage(); // Close the "Group By" Drop-Down
////
////            boolean isExpectedGroupByElements = HelperMethods.compareLists(expectedGroupByElementNames, actualGroupByElementNames);
////
////            if(!isExpectedGroupByElements){
////                softAssert.fail("Group By drop-down should contain all the expected elements");
////            }
//
//
//        } catch (Exception e) {
//            log.warn(ANSI_RED + "Error while verifying all elements on records page are visible and functional based on user type" + e + ANSI_RESET);
//        } finally {
//            softAssert.assertAll();
//        }
//
//    }
//
//    @And("Verify quick search functionality")
//    public void verifyQuickSearchFunctionality() {
//        objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
////        action.clickOn(CommonElements.NavigationBar.ACCOUNT_MAPPING);
//
//        String[] fields = {"record id", "account name", "website"};
//        for (String field : fields) {
//            WaitUtils.resolveAngularLoader();
//            switch (field) {
//                case "record id":
//                    objectFactory.getAccountMappingHomePage().enterTextInQuickSearch(field, quickSearchRecordIdValue);
//                    objectFactory.getAccountMappingHomePage().clickSearchButton();
//                    if (getTableCellValue(field).contains(quickSearchRecordIdValue)) {
//                        log.info(ANSI_GREEN + "Quick Search for "
//                                + ANSI_CYAN + field +
//                                ANSI_GREEN + " was successful" + ANSI_RESET);
//                    } else {
//                        log.warn(ANSI_RED + "Quick Search for " +
//                                ANSI_CYAN + field +
//                                ANSI_RED + " was unsuccessful" + ANSI_RESET);
//                    }
//                    break;
//
//                case "account name":
//                    objectFactory.getAccountMappingHomePage().enterTextInQuickSearch(field, quickSearchAccountNameValue);
//                    objectFactory.getAccountMappingHomePage().clickSearchButton();
//                    if (getTableCellValue(field).contains(quickSearchAccountNameValue)) {
//                        log.info(ANSI_GREEN + "Quick Search for " +
//                                ANSI_CYAN + field +
//                                ANSI_GREEN + " was successful" + ANSI_RESET);
//                    } else {
//                        log.warn(ANSI_RED + "Quick Search for " +
//                                ANSI_CYAN + field +
//                                ANSI_RED + " was unsuccessful" + ANSI_RESET);
//                    }
//                    break;
//
//                case "website":
//                    objectFactory.getAccountMappingHomePage().enterTextInQuickSearch(field, quickSearchWebsiteValue);
//                    objectFactory.getAccountMappingHomePage().clickSearchButton();
//                    if (getTableCellValue(field).contains(quickSearchWebsiteValue)) {
//                        log.info(ANSI_GREEN + "Quick Search for " +
//                                ANSI_CYAN + field +
//                                ANSI_GREEN + " was successful " + ANSI_RESET);
//                    } else {
//                        log.warn(ANSI_RED + "Quick Search  " +
//                                ANSI_CYAN + field +
//                                ANSI_RED + " was unsuccessful" + ANSI_RESET);
//                    }
//                    break;
//            }
//            objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
//        }
//    }
//
//    @When("Column names and Records should be visible when I click on default reports based on user type {string}")
//    public void checkAccountMappingRecordsVisibility(String userType) {
//        objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
//        // Verify Columns in grid
//        try {
//            // Comparing expected columns and actual columns on landing
//            log.info(ANSI_YELLOW + "Verifying Column names in the grid on landing for " + ANSI_PURPLE + userType + ANSI_YELLOW + " user" + ANSI_RESET);
//            objectFactory.getAccountMappingHomePage().clickDefaultReport("Open Accounts");
//            List<String> actualColumnsOnLanding = objectFactory.getCommonMethods().getTableColumnNames();
//            List<String> expectedColumns = objectFactory.getAccountMappingHomePage().getExpectedColumnNames(userType);
//            if (HelperMethods.compareLists(expectedColumns, actualColumnsOnLanding)) {
//                log.info(ANSI_GREEN + "Column names in the grid on landing for " + ANSI_PURPLE + userType + ANSI_GREEN + " user is AS EXPECTED" + ANSI_RESET);
//            } else {
//                log.error(ANSI_RED + "Column names in the grid on landing for " + ANSI_PURPLE + userType + ANSI_RED + " user is NOT AS EXPECTED" + ANSI_RESET);
//                softAssert.fail(ANSI_RED + "Column names in the grid on landing for " + ANSI_PURPLE + userType + ANSI_RED + " user is NOT AS EXPECTED" + ANSI_RESET);
//            }
//            // Comparing actual columns before and after search
////            log.info(ANSI_YELLOW + "Verifying Column names in the grid on landing for before and after search for " + ANSI_PURPLE + userType + ANSI_YELLOW + " user" + ANSI_RESET);
////            commonMethods.searchThisList(commonMethods.getTableCellValue("record id"));
////            List<String> actualColumnsAfterSearch = commonMethods.getTableColumnNames();
////
////            if(HelperMethods.compareLists(actualColumnsOnLanding, actualColumnsAfterSearch)){
////                log.info(ANSI_GREEN + "Column names in the grid on landing for before and after search for " + ANSI_PURPLE + userType + ANSI_GREEN + " user is AS EXPECTED" + ANSI_RESET);
////            }else{
////                log.info(ANSI_RED + "Column names in the grid before and after search for " + ANSI_PURPLE + userType + ANSI_RED + " user is AS EXPECTED" + ANSI_RESET);
////                log.error(ANSI_RED + "Column names in the grid before and after search for " + ANSI_PURPLE + userType + ANSI_RED + " user is AS EXPECTED" + ANSI_RESET);
////            }
//
//        } catch (Exception e) {
//            softAssert.fail(ANSI_RED + "Failed to Verify Column names in the grid on landing" + ANSI_RESET);
//            log.error(e);
//        }
//
//        // Verify Records in Grid
//        try {
//            for (String currentDefaultReport : objectFactory.getAccountMappingHomePage().getDefaultReport(userType)) {
//                objectFactory.getAccountMappingHomePage().clickDefaultReport(currentDefaultReport);
//                HelperMethods.waitUntilElementIsvisiblity(driver, By.xpath("//table[@role='table']"), 10);
//                String actualRecordStatus = getTableCellValue("status");
//                if (currentDefaultReport.contains(actualRecordStatus) && !currentDefaultReport.contains("SmartMatch")) {
//                    log.info(ANSI_GREEN + "Record status " + ANSI_CYAN + actualRecordStatus + ANSI_GREEN + " matches the current report " + ANSI_CYAN + currentDefaultReport + ANSI_RESET);
//
//                } else if (currentDefaultReport.contains("SmartMatch")) {
//                    log.info(ANSI_YELLOW + "Skipping the current report " + ANSI_CYAN + currentDefaultReport + "as test cases doesn't implies on current report" + ANSI_RESET);
//
//                } else {
//                    log.warn(ANSI_RED + "Record status " + ANSI_CYAN + actualRecordStatus + ANSI_RED + " doesn't matches the current report " + ANSI_CYAN + currentDefaultReport + ANSI_RESET);
//                    softAssert.fail(ANSI_RED + "Record status " + ANSI_CYAN + actualRecordStatus + ANSI_GREEN + " should match the current report i.e. " + ANSI_CYAN + currentDefaultReport + ANSI_RESET);
//                }
//
//                // Define the fields and their respective values using a Map
//                Map<String, String> fieldValues = Map.of(
//                        "record id", getTableCellValue("record id"),
//                        "account name", getTableCellValue("account name"),
//                        "website", getTableCellValue("website")
//                );
//
//                // Iterate over field-value pairs
//                for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
//                    String field = entry.getKey();
//                    String value = entry.getValue();
//
//                    objectFactory.getCommonMethods().searchThisList(value);
//                    log.info(ANSI_YELLOW + "Searching for the value: " +
//                            ANSI_GREEN + value +
//                            ANSI_RESET);
//
//                    HelperMethods.waitUntilElementIsvisiblity(driver, By.xpath("//table[@role='table']"), 10);
//
//                    // Retrieve the actual value based on the current field
//                    String actualValue = getTableCellValue(field);
//
//                    // Determine comparison logic based on the field
//                    boolean isMatch = field.equalsIgnoreCase("record id") ?
//                            actualValue.contains(value) :
//                            actualValue.equalsIgnoreCase(value);
//
//                    // Log the result and perform assertions
//                    if (isMatch) {
//                        log.info(ANSI_GREEN + "Search for "
//                                + ANSI_CYAN + field + ": "
//                                + value
//                                + ANSI_GREEN + " was successful."
//                                + ANSI_RESET);
//                    } else {
//                        log.error(ANSI_RED + "Search for "
//                                + ANSI_CYAN + field + ": "
//                                + value
//                                + ANSI_RED + " was unsuccessful."
//                                + ANSI_RESET);
//                        softAssert.fail(ANSI_RED
//                                + "Search functionality verification failed for "
//                                + field + ": " + value
//                                + ANSI_RESET);
//                    }
//                    // clear the search field before the next iteration
//                    // commonMethods.clearSearchInputField();
//                }
//                objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
//            }
//        } catch (Exception e) {
//            // Go back to Account Mapping home page
//            objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
//            log.error(ANSI_RED +
//                    "Error while verifying Records grid. Grid might be empty for this report"
//                    + ANSI_RESET, e);
//        } finally {
//            softAssert.assertAll();
//        }
//    }
//
//    @Then("Verify default sorting for records based on user type {string}")
//    public void verifyDefaultSortingForRecordsBasedOnUserType(String userType) {
//
////        //temp
////        accountMappingHomePage.clickDefaultReport("claimed accounts");
////        String value = commonMethods.getTableCellValue("website");
////        int actualCharacterLimit = commonMethods.getCharacterLimit(value);
//
//
//        // Verifying default sorting
//        if (userType.equalsIgnoreCase("vendor")) {
//            objectFactory.getAccountMappingHomePage().clickDefaultReport("open accounts");
//            String firstRecord = getTableCellValue("created on", 1);
//            String lastRecord = getTableCellValue("created on", 10);
//
//            String actualLatestDate = HelperMethods.getLatestDate(firstRecord, lastRecord);
//
//            if (actualLatestDate.equalsIgnoreCase(firstRecord)) {
//                log.info(ANSI_GREEN + "As expected default sorting for records is set to 'Created on'" + ANSI_RESET);
//            } else {
//                softAssert.fail(ANSI_RED + "Default sorting for records is NOT set to 'Created on'" + ANSI_RESET);
//            }
//
//            // Verifying sorting "Created On" in Oldest to Newest order
//            commonMethods.sortByTableColumn("created on");
//            HelperMethods.loaderwait(driver, new UserHome(driver).getAngularLoaderOverLay(), 5);
//            firstRecord = getTableCellValue("created on", 1);
//            lastRecord = getTableCellValue("created on", 10);
//
//            actualLatestDate = HelperMethods.getLatestDate(firstRecord, lastRecord);
//
//            if (actualLatestDate.equalsIgnoreCase(lastRecord)) {
//                log.info(ANSI_GREEN + "'Created on' sorting functionality is working as expected" + ANSI_RESET);
//            } else {
//                softAssert.fail(ANSI_RED + "'Created on' sorting functionality is NOT working as expected" + ANSI_RESET);
//            }
//        } else {
//            log.info(ANSI_YELLOW + "Test Scenario Skipped because 'Created on' column is not visible to partner user which " + ANSI_GREEN + "is expected" + ANSI_RESET);
//        }
//    }
//
//    @Given("Partner User is Logged in")
//    public void partnerUserIsLoggedIn() {
//        try {
////            objectFactory.getCommonMethods().switchBranding("Acme Global");
//            action.selectFromDropdown(CommonElements.VartopiaSwitcher.ACME_GLOBAL);
//            adminHomeAction.clickTile("Manage Users");
////            HelperMethods.clickWebelement(driver, By.xpath("//*[@id='ManageUsers']/button"));
//            manageUsersAction.loginWithEmail("suman@mailinator.com");
//            log.info(ANSI_GREEN + "User logged in with username: suman@mailinator.com" + ANSI_RESET);
////            action.clickOn(CommonElements.NavigationBar.ACCOUNT_MAPPING);
//            objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
//            log.info(ANSI_GREEN + "Successfully Logged in to Partner user" + ANSI_RESET);
//        } catch (Exception e) {
//            Assert.fail(ANSI_RED + "Error while Logging in Partner user" + ANSI_RESET, e);
//        }
//    }
//
//    @When("Get Record ID of records under Open Accounts, Pending Accounts and Claimed Accounts from Partner User")
//    public void getRecordIDOfRecordsUnderOpenAccountsPendingAccountsAndClaimedAccountsFromPartnerUser() {
//        try {
//            log.info(ANSI_GREEN + "Getting Record ID of records under Open Accounts, Pending Accounts and Claimed Accounts from Partner User" + ANSI_RESET);
//            String[] defaultReports = {"Open Accounts", "Pending Accounts", "Claimed Accounts"};
//
//            for (String defaultReport : defaultReports) {
//
//                objectFactory.getAccountMappingHomePage().clickDefaultReport(defaultReport);
//
//                // Get record ID and add it to the list
//                String recordId = getTableCellValue("Record id");
//                if (recordId != null && !recordId.isEmpty()) {
//                    recordIds.add(recordId);
//                }
//                objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
//            }
//            log.info(ANSI_PURPLE + "Record IDs retrieved: " + recordIds + ANSI_RESET);
//
//        } catch (Exception e) {
//            log.error("An error occurred while retrieving record IDs", e);
//        }
//    }
//
//    @And("Then login as a connected DRS Vendor user")
//    public void loginAsAConnectedDRSVendorUser() {
//        objectFactory.getManageUser().revertToUser();
//        objectFactory.getManageUser().goToManageUsers();
//        objectFactory.getManageUser().loginAs("nishant@vartopia.mailinator.com.acmeglobal");
//        log.info(ANSI_GREEN + "Vendor users Logged in successfully" + ANSI_RESET);
//    }
//
//    @Then("Records should exist under Open Accounts, Pending Accounts and Claimed Accounts with respective statuses")
//    public void partnerShouldBeAbleToViewTheRecordsOfTheirConnectedDRSVendors() {
//        try {
//
//            objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
//
//            String[] statuses = {"Open", "Pending", "Claimed"};
//            int i = 0;
//            for (String status : statuses) {
//                String recordId = recordIds.get(i);
//                objectFactory.getAccountMappingHomePage().enterTextInQuickSearch("record id", recordId);
//                objectFactory.getAccountMappingHomePage().clickSearchButton();
//                switch (status) {
//                    case "Open":
//
//                    case "Pending":
//
//                    case "Claimed":
//                        if (getTableCellValue("status").contains(status) && getTableCellValue("record id").contains(recordId)) {
//                            log.info(ANSI_GREEN + "Record ID: " + recordId + " | " + "Status: " + status + " exist in the Vendor user records" + ANSI_RESET);
//                        } else {
//                            log.warn(ANSI_RED + "Record ID: " + recordId + " | " + "Status: " + status + " does not exist in the Vendor user records" + ANSI_RESET);
//                            Assert.fail(ANSI_RED + "Record ID: " + recordId + " | " + "Status: " + status + " should exist in the Vendor user records" + ANSI_RESET);
//                        }
//                        break;
//                }
//                i++;
//                objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
//            }
//
//        } catch (Exception e) {
//            Assert.fail("An error occurred while verifying records", e);
//        } finally {
//            // Assert all soft assertions
//            softAssert.assertAll();
//        }
//
//    }
//
//    @And("login as another Partner user connected to same Vendor")
//    public void loginAsAnotherPartnerUserConnectedToAcmeVendor() {
//        objectFactory.getManageUser().revertToUser();
//        objectFactory.getManageUser().goToManageUsers();
//        objectFactory.getManageUser().loginAs("con2025@vartopia.mailinator.com");
//        objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
//    }
//
//
//    // Records in Open Accounts should be visible to both users and Records in Pending and Claimed should only be visible to the submitter or users under its sales territories
//    @Then("Records should exist under Open Accounts, Pending Accounts, and Claimed Accounts with respective statuses")
//    public void recordsShouldExistUnderOpenPendingAndClaimedAccountsWithRespectiveStatuses() {
//        try {
//            objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
//
//            String[] statuses = {"Open", "Pending", "Claimed"};
//            int i = 0;
//
//            for (String status : statuses) {
//                String recordId = recordIds.get(i);
//                objectFactory.getAccountMappingHomePage().enterTextInQuickSearch("record id", recordId);
//                objectFactory.getAccountMappingHomePage().clickSearchButton();
//
//                switch (status) {
//                    case "Open":
//                        if (getTableCellValue("status").equalsIgnoreCase(status) &&
//                                getTableCellValue("record id").contains(recordId)) {
//                            log.info(ANSI_GREEN + "Record ID: " + recordId + " | Status: " + status + " exists for both users" + ANSI_RESET);
//                        } else {
//                            log.warn(ANSI_RED + "Record ID: " + recordId + " | Status: " + status + " does not exist for both users" + ANSI_RESET);
//                            Assert.fail(ANSI_RED + "Record with status " + status + " should be visible to both users" + ANSI_RESET);
//                        }
//                        break;
//
//                    case "Pending":
//                    case "Claimed":
//                        boolean tableIsVisible = false;
//                        try {
//                            tableIsVisible = HelperMethods.waitUntilElementIsvisiblity(driver, tableLocator, 2);
//                        } catch (Exception ignored) {
//                        }
//
//                        if (!tableIsVisible) {
//                            log.info(ANSI_GREEN + "As Expected, Record ID: " + recordId + " | Status: " + status + " does not exist for users of a different partner" + ANSI_RESET);
//                        } else if (getTableCellValue("status").equalsIgnoreCase(status) &&
//                                getTableCellValue("record id").contains(recordId)) {
//                            log.warn(ANSI_RED + "Record ID: " + recordId + " | Status: " + status + " exists for users of a different partner" + ANSI_RESET);
//                            Assert.fail(ANSI_RED + "Record with status " + status + " should not be visible to users of a different partner" + ANSI_RESET);
//                        } else {
//                            log.warn(ANSI_RED + "Unexpected error while verifying visibility for Record ID: " + recordId + " | Status: " + status + ANSI_RESET);
//                            softAssert.fail(ANSI_RED + "Unexpected error while verifying visibility for Record ID: " + recordId + " | Status: " + status + ANSI_RESET);
//                        }
//                        break;
//
//                    default:
//                        log.warn(ANSI_RED + "Unhandled status: " + status + ANSI_RESET);
//                        softAssert.fail(ANSI_RED + "Unhandled status: " + status + ANSI_RESET);
//                }
//
//                i++;
//                objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
//            }
//
//        } catch (Exception e) {
//            Assert.fail("An error occurred while verifying records", e);
//        } finally {
//            // Assert all soft assertions
//            softAssert.assertAll();
//        }
//    }
//
//    @Given("Vendor User in Logged in")
//    public void vendorUserInLoggedIn() {
//        try {
//            objectFactory.getCommonMethods().switchBranding("Acme Global");
//            HelperMethods.clickWebelement(driver, By.xpath("//*[@id='ManageUsers']/button"));
//            manageUsersAction.loginWithEmail("acmeglobal@vartopia.com");
//            log.info(ANSI_GREEN + "User logged in with username: qaminitest@vartopia.mailinator.com.acmeglobal" + ANSI_RESET);
//            objectFactory.getAccountMappingHomePage().navigateToAccountMappingHome();
//            log.info(ANSI_GREEN + "Successfully Logged in to Vendor user" + ANSI_RESET);
//        } catch (Exception e) {
//            Assert.fail(ANSI_RED + "Error while Logging in Vendor user" + ANSI_RESET, e);
//        }
//    }
//
//    @Then("Users should be able to upload and submit records via both upload method")
//    public void usersShouldBeAbleToUploadAndSubmitRecordsViaBothUploadMethod() throws InterruptedException {
//
//        objectFactory.getAccountMappingHomePage().clickImportRecordsOption("New");
//        objectFactory.getAccountMappingHomePage().uploadRecordsFile("src/main/resources/AccountMappingFiles/Account_Records_list.xlsx");
//        objectFactory.getAccountMappingHomePage().navigateImportRecordsPopup("Next");
//        objectFactory.getAccountMappingHomePage().insertNewRecordsInImportRecordsPopup();
//
//
//
//
////        driver.switchTo().defaultContent();
//    }
//
//    @And("Copy-paste method")
//    public void copyPasteMethod() {
//
//    }
//}
