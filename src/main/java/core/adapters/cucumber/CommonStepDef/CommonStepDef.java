//package StepDefinition.CommonStepDef;
//
//import Elements.Interfaces.TableElement;
//import interactions.Interactions;
//import CommonUtilMethods.ApplicationCommonElements;
//import Configurations.EnvironmentConfiguration;
//import Configurations.InitialiseBaseTest;
//import Elements.Interfaces.ResolvableEnum;
//import Elements.ManageUsersElements;
//import HelperClasses.HelperMethods;
//import PageObjects.UserListing;
//import Pages.AdminHome;
//import Pages.Common.CommonMethods;
//import Pages.LoginPage;
//import Pages.ManageUser;
//import WebApplication.Vartopia;
//import com.beust.jcommander.internal.Nullable;
//import io.cucumber.java.ParameterType;
//import org.openqa.selenium.By;
//import org.testng.Assert;
//import core.utils.web.DOMUtils;
//import domain.automation.web.selenium.driver.SeleniumDriverContext;
//import core.utils.EnumResolver;
//import core.utils.json.JsonLogger;
//import core.utils.json.JsonReader;
//import core.resolvers.locators.JsonLocatorReader;
//import core.utils.CustomLogger;
//import core.utils.URLS;
//import core.utils.web.TableHandler;
//import core.utils.UIContext;
//import core.utils.data.DataVerifier;
//import core.utils.web.WaitUtils;
//import io.cucumber.java.en.And;
//import io.cucumber.java.en.Given;
//import io.cucumber.java.en.Then;
//import io.cucumber.java.en.When;
//import org.apache.log4j.Logger;
//import org.openqa.selenium.WebDriver;
//
//import java.util.*;
//
//import static HelperClasses.HelperMethods.scrollAndClick;
//import static automation.interactions.InteractionsDSL.resolveByContext;
//
//public class CommonStepDef extends InitialiseBaseTest {
//
//    private final Logger log = Logger.getLogger(CommonStepDef.class);
//    public WebDriver driver = getDriver();
//    ManageUser manageUser = new ManageUser(driver);
//    static String modulename;
//    CommonMethods commonMethods = new CommonMethods();
//    Vartopia vartopia;
//
//    // Γ£à Add constructor to initialize SeleniumDriverContext
//    public CommonStepDef() {
//       CustomLogger.initialize(this.getClass());
//        SeleniumDriverContext.setDriver(driver);
//        vartopia = new Vartopia();
//    }
//
//    // --- Small helpers for richer assert messages ---
//    private String activeKeySafe() {
//        try { return SeleniumDriverContext.getActiveKey(); } catch (Exception e) { return "n/a"; }
//    }
//    private String pageUrl() {
//        try { return HelperMethods.getCurrentURLPage(driver); } catch (Exception e) { return "n/a"; }
//    }
//
//    @ParameterType("true|false")
//    public boolean bool(String boolValue) {
//        return Boolean.parseBoolean(boolValue);
//    }
//
//    @ParameterType(".*")
//    public String nullable(String value) {
//        if (value == null) return null;
//
//        String trimmed = value.trim();
//        if (trimmed.equalsIgnoreCase("__NULL__") || trimmed.equalsIgnoreCase("null") || trimmed.equals("-")) {
//            return null;
//        }
//        return trimmed;
//    }
//
//    @Given("User is on Login Page")
//    public void user_is_on_login_page() {
//        driver.get(EnvironmentConfiguration.getBaseURL());
//        log.info("Hitting web URL");
//    }
//
//    @When("Enter valid login credentials")
//    public void enter_valid_login_credentials() {
//        try {
//            log.error(driver.findElement(By.xpath("//title[contains(text(), 'Bad Gateway')]")).getText());
//        } catch (org.openqa.selenium.NoSuchElementException exception) {
//            new LoginPage(driver).loginUser(EnvironmentConfiguration.getUsername(), EnvironmentConfiguration.getPassword());
//            WaitUtils.resolveAngularLoader();
//            setSprintVersion();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Then("Validate Title of page is {string}")
//    public void Validate_title_of_page_is(String expectedTitle) {
//        String actualTitle = driver.getTitle();
//        softAssert.assertEquals(
//                actualTitle,
//                expectedTitle,
//                String.format(
//                        "Page title mismatch [active=%s]. Expected: \"%s\", Actual: \"%s\", URL: %s",
//                        activeKeySafe(), expectedTitle, actualTitle, pageUrl()
//                )
//        );
//    }
//
//    @Then("Enter username going to login {string}")
//    public void enter_username_going_to_check(String loginuser) {
//        AdminHome.manage_User();
//        String actualUrl = HelperMethods.getCurrentURLPage(driver);
//        String expectedUrl = URLS.getManageUserURL();
//        softAssert.assertEquals(
//                actualUrl,
//                expectedUrl,
//                String.format(
//                        "Admin Login URL mismatch [active=%s]. Expected: %s, Actual: %s",
//                        activeKeySafe(), expectedUrl, actualUrl
//                )
//        );
//        manageUser.loginas((loginuser != null && !loginuser.isEmpty()) ? loginuser : UserListing.getDealCoachUser());
//    }
//
//    @Then("Move to {string} module")
//    public void moveToModule(String ModuleName) {
//        modulename = ModuleName;
//        manageUser.movetoHeaderModule(ModuleName);
//        softAssert.assertTrue(
//                ApplicationCommonElements.isBreadCrumbDisplayed(),
//                String.format(
//                        "Breadcrumb not displayed after navigating to module \"%s\" [active=%s], URL: %s",
//                        ModuleName, activeKeySafe(), pageUrl()
//                )
//        );
//    }
//
//    @Then("Check page URL")
//    public void check_page_url() {
//        String moduleURL = modulename.equals("Registrations") ? URLS.getRegistrationPageURL() :
//                modulename.equals("Renewals") ? URLS.getRenewalsPageURL() :
//                        modulename.equals("Analytics") ? URLS.getAnalyticsDashboardURL() :
//                                URLS.getHomePageURL();
//
//        String actualUrl = HelperMethods.getCurrentURLPage(driver);
//        softAssert.assertEquals(
//                actualUrl,
//                moduleURL,
//                String.format(
//                        "Module page URL mismatch for \"%s\" [active=%s]. Expected: %s, Actual: %s",
//                        modulename, activeKeySafe(), moduleURL, actualUrl
//                )
//        );
//    }
//
//    @Then("Assert all soft assertions")
//    public void assert_all_soft_assertions() {
//        // If your TestNG version supports message in assertAll, you can pass it.
//        // Otherwise, this call is fine as-is and failures will list all collected messages.
//        softAssert.assertAll("One or more soft assertions failed in CommonStepDef steps.");
//    }
//
//    @Then("Move to Grid View {string} {string}")
//    public void move_to_grid_view(String reportType, String ReportName) {
//        HelperMethods.deleteDownloadedFiles();
//        commonMethods.moveToGridView(reportType.trim(), modulename.trim(), ReportName.trim());
//    }
//
//    @And("Move to Another Report {string} {string}")
//    public void Move_to_Another_Report(String reportType, String reportName) {
//        commonMethods.moveToReport(reportType, reportName);
//    }
//
//    @And("Test some class and methods")
//    public void testSomeClassAndMethods() {
//        vartopia.newRegistrationInteraction().openRegistrationForm(
//                List.of("Acme Global Deal Registration"),
//                null,
//                null,
//                null,
//                "acmeglobal@vartopia.mailinator.com",
//                "American Traffic Solutions",
//                "John Smith"
//        );
//    }
//
//    @And("Register a form")
//    public void registerAForm() throws Exception {
//        vartopia.newRegistrationInteraction().fillRequiredFields();
//    }
//
//    public static class IgnoreKeyParser {
//
//        private static final Set<String> NULL_KEYWORDS = Set.of("__NULL__", "null", "-", "\"\"", "");
//
//        public static List<String> fromString(String raw) {
//            if (raw == null) return null;
//
//            String cleaned = raw.trim().replaceAll("^\"|\"$", "").toLowerCase(); // remove outer quotes
//
//            if (NULL_KEYWORDS.contains(cleaned)) {
//                return null;
//            }
//
//            return Arrays.stream(raw.split(","))
//                    .map(String::trim)
//                    .filter(s -> !s.isBlank())
//                    .toList();
//        }
//    }
//
//    @Given("the following dropdown options from {string} in {string} are visible on the page:")
//    public void theFollowingDropdownOptionsAreVisibleOnThePage(String keyPrefix, String keySuffix, List<String> options) {
//        vartopia.dsl().triggerDropdownByContext(keyPrefix, keySuffix);
//        boolean isVisible = vartopia.dsl().verifyElementsAreVisible(keyPrefix, keySuffix, options);
//        DOMUtils.sendEscapeKey();
//        softAssert.assertTrue(
//                isVisible,
//                String.format(
//                        "Expected dropdown options %s from context [%s.%s] to be visible, but one or more were not [active=%s, URL=%s].",
//                        options, keyPrefix, keySuffix, activeKeySafe(), pageUrl()
//                )
//        );
//    }
//
//    @Given("{string} with Email {string} is Logged in from Manage Users Page")
//    public void userTypeWithEmailIsLoggedIn(String userType, String userEmail) {
//        try {
//           debug.click("Attempting to login with user email: " + userEmail + " User Type: " + userType);
//            vartopia.interaction().clickOn(ManageUsersElements.ActionBar.LayoutSwitcher.GRID_VIEW);
//            ManageUsersElements.FilterBy.UserTypeDropdown selectedType = EnumResolver.stringToEnum(ManageUsersElements.FilterBy.UserTypeDropdown.class, userType);
//            vartopia.manageUsersInteraction().filterBy(selectedType, Map.of(ManageUsersElements.FilterBy.TextInputField.EMAIL, userEmail), null);
//            vartopia.interaction().clickOn(ManageUsersElements.UserCardClickableElement.LOGIN_AS_BUTTON);
//
//            WaitUtils.resolveAngularLoader();
//
//           info.complete("Successfully logged in as '" + userEmail + "' and verified user type: " + userType);
//
//        } catch (Exception e) {
//           error.fallback("Login failed for user: " + userEmail);
//            Assert.fail(
//                    String.format(
//                            "Error while logging in as '%s' or verifying user type '%s' [active=%s, URL=%s].",
//                            userEmail, userType, activeKeySafe(), pageUrl()
//                    ),
//                    e
//            );
//        }
//    }
//
//    @When("{string} with Username {string} is Logged in from Manage Users Page")
//    public void withUsernameIsLoggedInFromManageUsersPage(String userType, String username) {
//        try {
//           debug.click("Attempting to login with user username: " + username);
//            WaitUtils.resolveAngularLoader();
//            vartopia.interaction().clickOn(ManageUsersElements.ActionBar.LayoutSwitcher.GRID_VIEW);
//            vartopia.manageUsersInteraction().loginWithUsername(username);
//
//           debug.wait("Waiting for Angular loader after login...");
//            WaitUtils.resolveAngularLoader();
//
//           info.complete("Successfully logged in as '" + username + "' and verified user type: " + userType);
//
//        } catch (Exception e) {
//           error.fallback("Login failed for user: " + username);
//            Assert.fail(
//                    String.format(
//                            "Error while logging in as '%s' or verifying user type '%s' [active=%s, URL=%s].",
//                            username, userType, activeKeySafe(), pageUrl()
//                    ),
//                    e
//            );
//        }
//    }
//
//    @Then("User successfully landed on {string} Page")
//    public void isUserSuccessfullyLandedOnPage(String pageNameOnTheBreadcrumb) {
//       info.validation("Using breadcrumb for page verification: " + pageNameOnTheBreadcrumb);
//
//        boolean result = DataVerifier.VerifyCurrentPage.nameViaBreadcrumb(pageNameOnTheBreadcrumb, true);
//        softAssert.assertTrue(
//                result,
//                String.format(
//                        "Incorrect current page via breadcrumb verification. Expected: \"%s\" [active=%s, URL=%s].",
//                        pageNameOnTheBreadcrumb, activeKeySafe(), pageUrl()
//                )
//        );
//    }
//
//    @And("Land on {string} Page")
//    public void landOnPage(String pageName) {
//       info.click("Navigating to page: " + pageName);
//        CommonElements.NavigationBar navItem =
//                EnumResolver.stringToEnum(CommonElements.NavigationBar.class, pageName);
//        vartopia.interaction().clickOn(Interactions.of(Interactions.Before.HIGHLIGHT_ELEMENT), navItem, Interactions.of(Interactions.After.WAIT_FOR_ANGULAR_LOADER));
//    }
//
//    @And("Click on {string} from navigation bar")
//    public void clickOnFromNavigationBar(String buttonName) {
//       info.click("Clicking on navigation bar button: " + buttonName);
//        vartopia.dsl().clickOnNavigationBar(buttonName);
//    }
//
//    @Given("All elements are visible")
//    public void allElementsAreVisible() {
//       info.validation("Verifying all elements are visible on the page.");
//    }
//
//
//    @When("User click {string} Dropdown and select {string}")
//    public void userClickDropdownAndSelect(String dropdownLabel, String optionLabel) {
//       info.dropdown("Selecting '" + optionLabel + "' from '" + dropdownLabel + "' dropdown");
//        vartopia.accountMappingInteraction().selectFromDropdownByContext(dropdownLabel, optionLabel);
//    }
//
//    @And("Switch to Import Records popup iframe")
//    public void switchToImportRecordsPopup() {
//        try {
//           info.frame("Switching to Import Records popup frame");
//            vartopia.accountMappingInteraction().switchToImportRecordsPopup();
//        } catch (Exception e) {
//           debug.frame(e.getMessage());
//           info.log("Trying clicking last element");
//            vartopia.interaction().clickOn(UIContext.getLastElement());
//           info.frame("Switching to Import Records popup frame");
//            vartopia.accountMappingInteraction().switchToImportRecordsPopup();
//        }
//    }
//
//    @And("User clicks on {string} from {string} in {string} and wait for angular loader")
//    public void clickOnFromAndWaitForAngularLoader(String label, String keyPrefix, String keySuffix) {
//       info.click("Clicking on '" + label + "' from '" + keyPrefix + "' in '" + keySuffix + "'");
//        vartopia.dsl().clickOnFrom(keyPrefix, keySuffix, label, Interactions.After.WAIT_FOR_ANGULAR_LOADER);
//    }
//
//    @And("User clicks on {string} from {string} in {string}")
//    public void clickOnFrom(String label, String keyPrefix, String keySuffix) {
//       info.click("Clicking on '" + label + "' from '" + keyPrefix + "' in '" + keySuffix + "'");
//        vartopia.dsl().clickOnFrom(keyPrefix, keySuffix, label, Interactions.After.DO_NOTHING);
//    }
//
//    @When("User searches for {string} using {string} from {string} in {string}")
//    public void user_searches_using_field_in_context(String searchTerm, String unresolvedEnumName, String keyPrefix, String keySuffix) {
//       info.click("Searching for '" + searchTerm + "' using '" + unresolvedEnumName + "' from '" + keyPrefix + "' in '" + keySuffix + "'");
//        vartopia.dsl().getSearchedElementByContext(keyPrefix, keySuffix, unresolvedEnumName, searchTerm);
//    }
//
//    @And("User searches and click on {string} using {string} from {string} in {string}")
//    public void userSearchesAndClickOnUsingFromIn(String searchTerm, String unresolvedEnumName, String keyPrefix, String keySuffix) {
//       info.click("Searching and clicking '" + searchTerm + "' using '" + unresolvedEnumName + "' from '" + keyPrefix + "' in '" + keySuffix + "'");
//        vartopia.dsl().clickSearchableElementByContext(keyPrefix, keySuffix, unresolvedEnumName, searchTerm);
//    }
//
//    @And("User searches and click on {string} using {string} from {string} in {string} and wait for angular loader")
//    public void userSearchesAndClickOnUsingFromInAndWaitForAngularLoader(String searchTerm, String unresolvedEnumName, String keyPrefix, String keySuffix) {
//       info.click("Searching and clicking '" + searchTerm + "' using '" + unresolvedEnumName + "' from '" + keyPrefix + "' in '" + keySuffix + "'");
//        vartopia.dsl().clickSearchableElementByContext(keyPrefix, keySuffix, unresolvedEnumName, searchTerm);
//        WaitUtils.resolveAngularLoader();
//    }
//
//    @And("User selects {string} from {string} dropdown in {string}")
//    public void userSelectsFromDropdown(String label, String keyPrefix, String keySuffix) {
//       info.dropdown("Selecting '" + label + "' from '" + keyPrefix + "' dropdown in '" + keySuffix + "'");
//        vartopia.dsl().selectFromDropdownByContext(keyPrefix, keySuffix, label);
//    }
//
//    @And("User selects {string} option from {string} dropdown #{int} in {string}")
//    public void userSelectsFromIndexedDropdown(String label, String keyPrefix, int index, String keySuffix) {
//       info.dropdown("Selecting '" + label + "' from '" + keyPrefix + "' dropdown #" + index + " in '" + keySuffix + "'");
//        vartopia.dsl().selectFromDropdownByContext(keyPrefix, keySuffix, index, label);
//    }
//
//    @And("User selects {string} from {string} option dropdown #{int} in {string} an wait for angular loader")
//    public void userSelectsFromIndexedDropdownAndWaitForAngularLoader(String label, String keyPrefix, int index, String keySuffix) {
//       info.dropdown("Selecting '" + label + "' from '" + keyPrefix + "' dropdown #" + index + " in '" + keySuffix + "'");
//        vartopia.dsl().selectFromDropdownByContext(keyPrefix, keySuffix, index, label);
//        WaitUtils.resolveAngularLoader();
//    }
//
//    // Alternative wording if you prefer index at the end:
//    @And("User selects {string} from {string} dropdown in {string} at index {int}")
//    public void userSelectsFromIndexedDropdownAlt(String label, String keyPrefix, String keySuffix, int index) {
//       info.dropdown("Selecting '" + label + "' from '" + keyPrefix + "' dropdown #" + index + " in '" + keySuffix + "'");
//        vartopia.dsl().selectFromDropdownByContext(keyPrefix, keySuffix, index, label);
//    }
//
//    @Then("User Click on three-Dots of record number {int} in Records Page")
//    public void userClickOnThreeDotsOfRecordNumberInRecordsPage(int rowNumber) {
//        vartopia.recordsGridInteraction().row.click3Dots(rowNumber);
//    }
//
//    @Given("the following dropdown options for dropdown #{int} from {string} in {string} are visible:")
//    public void theFollowingDropdownOptionsForDropdownsAreVisibleOnThePage(
//            int recordIndex,
//            String keyPrefix,
//            String keySuffix,
//            List<String> expectedOptions
//    ) {
//        vartopia.dsl().triggerDropdownByContext(keyPrefix, keySuffix, recordIndex);
//        boolean allVisible = vartopia.dsl()
//                .verifyElementsAreVisible(keyPrefix, keySuffix, expectedOptions);
//        DOMUtils.sendEscapeKey();
//        softAssert.assertTrue(
//                allVisible,
//                String.format(
//                        "Not all three-dots menu options are visible for record #%d. Expected options: %s [context=%s.%s, active=%s, URL=%s]",
//                        recordIndex, expectedOptions, keyPrefix, keySuffix, activeKeySafe(), pageUrl()
//                )
//        );
//    }
//
//    @Given("the following elements from {string} in {string} are visible on the page:")
//    public void theFollowingElementsAreVisibleOnThePage(String keyPrefix, String keySuffix, List<String> elements) {
//        boolean isVisible = vartopia.dsl().verifyElementsAreVisible(keyPrefix, keySuffix, elements);
//        softAssert.assertTrue(
//                isVisible,
//                String.format(
//                        "Expected elements %s from context [%s.%s] to be visible on the page, but one or more were not [active=%s, URL=%s].",
//                        elements, keyPrefix, keySuffix, activeKeySafe(), pageUrl()
//                )
//        );
//    }
//
//    @Given("User sorts records by {string} in {string} order")
//    public void SortsRecordsByInOrder(String columnHeader, String sortingOrder) {
//        // Implement sorting logic if needed
//    }
//
//    @And("Get first row data from {string} on {string}")
//    public List<Map<String, String>> getFirstRowDataFrom(String keyPrefix, String keySuffix) {
//        ResolvableEnum resolved = resolveByContext(keySuffix, keyPrefix);
//        if (!(resolved instanceof TableElement table)) {
//            throw new IllegalArgumentException("Enum for context '" + keySuffix + "' is not a TableElement.");
//        }
//        List<Map<String, String>> actualFirstRowData = TableHandler.getRow(table, 1, null, false);
//       info.complete("Actual data in first row: " + actualFirstRowData);
//
//        JsonLogger.Write.MapWriter.writeRowList(null, "last-actual-records", actualFirstRowData);
//        return actualFirstRowData;
//    }
//
//    @And("save the first row from {string} in {string} page to {string} as JSON")
//    public void saveTheFirstRowFromTheOnThePageToAsJSON(String enumName, String resolvedKey, String fileDestinationPath) {
//        ResolvableEnum resolved = resolveByContext(enumName, resolvedKey);
//        if (!(resolved instanceof TableElement table)) {
//            throw new IllegalArgumentException("Enum for context '" + resolvedKey + "' is not a TableElement.");
//        }
//        List<Map<String, String>> actualFirstRowData = TableHandler.getRow(table, 1, null, false);
//       info.complete("Actual data in first row: " + actualFirstRowData);
//
//        JsonLogger.Write.MapWriter.writeRowList(null, fileDestinationPath, actualFirstRowData);
//    }
//
//    @When("User Get column names from {string} on {string}")
//    public List<String> getColumnNamesFromOn(String keyPrefix, String keySuffix) {
//        ResolvableEnum resolved = resolveByContext(keySuffix, keyPrefix);
//        if (!(resolved instanceof TableElement table)) {
//            throw new IllegalArgumentException("Enum for context '" + keySuffix + "' is not a DropdownElement.");
//        }
//        List<String> columnNames = TableHandler.getColumnHeaders(table);
//       info.log(columnNames.toString());
//        return columnNames;
//    }
//
//    @Then("Verify last inserted records with last actual records")
//    public void verifyLastInsertedRecordsWithLastActualRecords() {
//        List<Map<String, String>> inserted = JsonReader.Read.ListReader.asRowList(null, "last-inserted-records");
//        List<Map<String, String>> actual = JsonReader.Read.ListReader.asRowList(null, "last-actual-records");
//        // DataVerifier should itself throw/assert on mismatches; if it returns boolean in future, wrap with Assert + message.
//        DataVerifier.compare.listOfMaps(inserted, actual, List.of("Incentive Amount"), true, true);
//    }
//
//    @And("Switch branding to vartopia hub")
//    public void switchBrandingToVartopiaHub() {
//        try {
//            vartopia.interaction().selectFromDropdown(CommonElements.VartopiaSwitcher.VARTOPIA_HUB);
//        } catch (Exception ignored) {
//            // Consider logging this as debug if it's expected to fail silently
//        }
//    }
//
//    @Then("the expected JSON file {string} data should match the actual JSON file {string} with ignore extra actual records set to {bool} and ignoring the following expected keys: {string}")
//    public void compareJsonFilesWithInlineIgnoreKeys(
//            String expectedResultRelativeFilePath,
//            String actualResultRelativeFilePath,
//            boolean ignoreUncommonKeys,
//            @Nullable String ignoredExpectedKeysStrings
//    ) {
//        // Parse ignore keys from inline string
//        List<String> ignoreKeys = IgnoreKeyParser.fromString(ignoredExpectedKeysStrings);
//
//        // Read both JSONs as row-list
//        List<Map<String, String>> expectedData = JsonReader.Read.ListReader.asRowList(null, expectedResultRelativeFilePath);
//        List<Map<String, String>> actualData = JsonReader.Read.ListReader.asRowList(null, actualResultRelativeFilePath);
//
//        // Use MASTER listOfMaps signature:
//        // (expected, actual, ignoreKeys, keyCorrectionMap, onlyCompareKeysInActual, onlyCompareKeysInExpected)
//        boolean result = DataVerifier.compare.listOfMaps(
//                expectedData,
//                actualData,
//                ignoreKeys,
//                Map.of(
//                        // Key Correction Map
//                        "Incentive Amount (In USD)", "Incentive Amount",
//                        "Record Id", "AM Id"
//                ),
//                ignoreUncommonKeys,   // ΓåÆ onlyCompareKeysInActual
//                ignoreUncommonKeys    // ΓåÆ onlyCompareKeysInExpected
//        );
//
//        Assert.assertTrue(
//                result,
//                String.format(
//                        "Mismatch between expected [%s] and actual [%s] JSON data [ignoreExtras=%s, ignoreKeys=%s].",
//                        expectedResultRelativeFilePath, actualResultRelativeFilePath, ignoreUncommonKeys, ignoreKeys
//                )
//        );
//    }
//
//    @Then("Switch to default iframe")
//    public void switchToDefaultIframe() {
//        DOMUtils.switchToDefaultContent();
//    }
//
//    @Given("Temp step")
//    public void tempStep() {
//        By locator = JsonLocatorReader.getLocator(AdminHomeElements.Tiles.MANAGE_USERS);
//        vartopia.interaction().clickOn(driver.findElement(locator));
//        WaitUtils.resolveAngularLoader();
//    }
//
//}
