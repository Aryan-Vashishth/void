package examples.tests;

import core.actions.Action;
import core.flow.Flow;
import core.logging.CustomLogger;
import core.logging.theme.LogTheme;
import core.runtime.VOID;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.role.ElementRole;
import examples.listeners.ScreenshotCapable;
import examples.pages.demoqa.ButtonsPage;
import examples.pages.demoqa.CheckBoxPage;
import examples.pages.demoqa.PracticeFormPage;
import examples.pages.demoqa.PracticeFormPage.ConfirmationModal;
import examples.pages.demoqa.PracticeFormPage.PersonalDetails;
import examples.pages.demoqa.SelectMenuPage;
import examples.pages.demoqa.UploadDownloadPage;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.*;

/**
 * DemoQA test suite -- form inputs (text, number, dropdown, multi-select), checkboxes, radios,
 * buttons, file upload and file download.
 *
 * <p>Test IDs match DemoQA_Test_Cases.csv.</p>
 */
@Test
public class DemoQATest implements ScreenshotCapable {

    private static final String BASE_URL          = "https://demoqa.com";
    private static final String PRACTICE_FORM     = BASE_URL + "/automation-practice-form";
    private static final String SELECT_MENU       = BASE_URL + "/select-menu";
    private static final String UPLOAD_DOWNLOAD   = BASE_URL + "/upload-download";
    private static final String BUTTONS           = BASE_URL + "/buttons";
    private static final String CHECK_BOX         = BASE_URL + "/checkbox";

    private static final String UPLOAD_RESOURCE   = "/examples/testdata/demoqa/profile-picture.png";
    private static final String DOWNLOAD_FILE     = "sampleFile.jpeg";
    private static final Path   DOWNLOAD_DIR      = Path.of("target", "downloads", "demoqa").toAbsolutePath();

    // Per-thread session -- each parallel test method gets its own browser instance.
    private static final ThreadLocal<VOID> SESSION = new ThreadLocal<>();

    private VOID app() { return SESSION.get(); }

    @Override
    public byte[] captureScreenshot() {
        try {
            VOID app = SESSION.get();
            return app != null ? app.browser().takeScreenshot() : new byte[0];
        } catch (Exception e) {
            return new byte[0];
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    @BeforeClass
    public void setUp() throws IOException {
        CustomLogger.initialize(DemoQATest.class);
        CustomLogger.enableAnsi();
        CustomLogger.setTheme(LogTheme.HIGH_CONTRAST);
        // Read by SeleniumDriverFactory (system properties layer) when each session starts.
        Files.createDirectories(DOWNLOAD_DIR);
        System.setProperty("downloadsDir", DOWNLOAD_DIR.toString());
        // DemoQA's ad scripts can stall a full page load until Chrome's renderer times out on slow
        // connections. Return once the DOM is ready; element waits cover the rest. -D overrides it.
        if (System.getProperty("pageLoadStrategy") == null) {
            System.setProperty("pageLoadStrategy", "eager");
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void startSession() {
        CustomLogger.info.log(Thread.currentThread().getName());
        SESSION.set(VOID.builder().start());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        VOID app = SESSION.get();
        if (app != null) app.shutdown();
        SESSION.remove();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private void open(String url) {
        app().browser().navigateTo(url);
        // DemoQA injects ad iframes and a fixed banner that overlap the lower form controls.
        app().run(executor -> ((UIEngine) executor).executeScript(
                "document.querySelectorAll('#fixedban, footer, iframe').forEach(e => e.remove());"));
    }

    private void fillRequiredFields(String first, String last, PersonalDetails.Gender gender, String mobile) {
        app().run(Flow.of(
                PersonalDetails.Fields.FIRST_NAME_INPUT.type(first),
                PersonalDetails.Fields.LAST_NAME_INPUT.type(last),
                gender.click(),
                PersonalDetails.Fields.MOBILE_INPUT.type(mobile)
        ));
    }

    private void submitForm() {
        app().run(Flow.of(PracticeFormPage.Form.SUBMIT_BUTTON.click()));
    }

    private String read(ConfirmationModal.Labels row) {
        return app().reader().query(row.getText());
    }

    private boolean isChecked(Clickable element) {
        return "true".equals(app().elements().attribute(element, "checked"));
    }

    /**
     * The year dropdown spans 1900-2100, so it cannot be a {@code Dropdown} enum (which binds option
     * text to a constant). Select by free text through the engine instead.
     */
    private static Action selectYear(String year) {
        return executor -> {
            UIEngine engine = (UIEngine) executor;
            engine.selectByVisibleText(
                    engine.resolve(PracticeFormPage.DateOfBirth.Buttons.YEAR_SELECT, ElementRole.TRIGGER), year);
        };
    }

    /** Clickable exposes no key presses; send one to the element's focus target through the engine. */
    private static Action pressEscapeOn(Clickable element) {
        return executor -> {
            UIEngine engine = (UIEngine) executor;
            engine.sendKey(engine.resolve(element, ElementRole.TRIGGER), "ESCAPE");
        };
    }

    private static Path uploadFixture() {
        try {
            return Path.of(DemoQATest.class.getResource(UPLOAD_RESOURCE).toURI());
        } catch (URISyntaxException | NullPointerException e) {
            throw new IllegalStateException("Upload fixture missing from classpath: " + UPLOAD_RESOURCE, e);
        }
    }

    private static Path awaitDownload(Path file, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (Files.exists(file) && Files.size(file) > 0) return file;
            } catch (IOException ignored) {
                // File still being written; retry.
            }
            Thread.sleep(250);
        }
        return null;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRACTICE FORM  (FORM-01 ... FORM-11)
    // ═════════════════════════════════════════════════════════════════════════

    @Test(groups = "form", description = "FORM-01 -- Submit fully populated form; confirmation modal echoes every field")
    public void form01_submitAllFields() {
        open(PRACTICE_FORM);
        Path picture = uploadFixture();

        app().run(Flow.of(
                PersonalDetails.Fields.FIRST_NAME_INPUT.type("Priya"),
                PersonalDetails.Fields.LAST_NAME_INPUT.type("Sharma"),
                PersonalDetails.Fields.EMAIL_INPUT.type("priya.sharma@example.com"),
                PersonalDetails.Gender.FEMALE_RADIO.click(),
                PersonalDetails.Fields.MOBILE_INPUT.type("9876543210"),
                PracticeFormPage.DateOfBirth.Buttons.CALENDAR_TOGGLE.click(),
                PracticeFormPage.DateOfBirth.Months.JANUARY.select(),
                selectYear("1995"),
                PracticeFormPage.DateOfBirth.DynamicButtons.DAY_CELL.click().withArgs("015"),
                PracticeFormPage.Subjects.Fields.SUBJECTS_INPUT.typeAndPress("Maths", "ENTER"),
                PracticeFormPage.Subjects.Fields.SUBJECTS_INPUT.typeAndPress("Physics", "ENTER"),
                PracticeFormPage.Hobbies.Options.SPORTS_CHECKBOX.set(true),
                PracticeFormPage.Hobbies.Options.MUSIC_CHECKBOX.set(true),
                PracticeFormPage.Picture.Fields.PICTURE_UPLOAD.upload(picture.toString()),
                PracticeFormPage.Address.Fields.CURRENT_ADDRESS_INPUT.type("221B Baker Street"),
                PracticeFormPage.Address.Fields.STATE_INPUT.typeAndPress("NCR", "ENTER"),
                PracticeFormPage.Address.Fields.CITY_INPUT.typeAndPress("Delhi", "ENTER"),
                PracticeFormPage.Form.SUBMIT_BUTTON.click()
        ));
        app().elements().waitForVisible(ConfirmationModal.Buttons.CLOSE_BUTTON);

        assertEquals(read(ConfirmationModal.Labels.MODAL_TITLE), "Thanks for submitting the form");
        assertEquals(read(ConfirmationModal.Labels.STUDENT_NAME_VALUE), "Priya Sharma");
        assertEquals(read(ConfirmationModal.Labels.STUDENT_EMAIL_VALUE), "priya.sharma@example.com");
        assertEquals(read(ConfirmationModal.Labels.GENDER_VALUE), "Female");
        assertEquals(read(ConfirmationModal.Labels.MOBILE_VALUE), "9876543210");
        assertEquals(read(ConfirmationModal.Labels.DATE_OF_BIRTH_VALUE), "15 January,1995");
        assertEquals(read(ConfirmationModal.Labels.SUBJECTS_VALUE), "Maths, Physics");
        assertEquals(read(ConfirmationModal.Labels.HOBBIES_VALUE), "Sports, Music");
        assertEquals(read(ConfirmationModal.Labels.PICTURE_VALUE), picture.getFileName().toString());
        assertEquals(read(ConfirmationModal.Labels.ADDRESS_VALUE), "221B Baker Street");
        assertEquals(read(ConfirmationModal.Labels.STATE_AND_CITY_VALUE), "NCR Delhi");
    }

    @Test(groups = "form", description = "FORM-02 -- Empty submit is blocked; required fields report validation messages")
    public void form02_emptySubmitBlocked() {
        open(PRACTICE_FORM);
        submitForm();

        assertFalse(app().elements().isVisible(ConfirmationModal.Labels.MODAL_TITLE),
                "Confirmation modal must not open for an empty form");
        for (PersonalDetails.Fields required : List.of(
                PersonalDetails.Fields.FIRST_NAME_INPUT,
                PersonalDetails.Fields.LAST_NAME_INPUT,
                PersonalDetails.Fields.MOBILE_INPUT)) {
            assertFalse(app().elements().attribute(required, "validationMessage").isBlank(),
                    required + " should report a required-field message");
        }
        assertFalse(app().elements().attribute(PersonalDetails.Gender.MALE_RADIO, "validationMessage").isBlank(),
                "Gender radio group should be required");
        assertTrue(app().elements().attribute(PersonalDetails.Fields.EMAIL_INPUT, "validationMessage").isBlank(),
                "Email is optional and should not report an error when empty");
    }

    @Test(groups = "form", description = "FORM-03 -- Mobile number field accepts at most 10 digits")
    public void form03_mobileMaxLength() {
        open(PRACTICE_FORM);
        app().run(Flow.of(PersonalDetails.Fields.MOBILE_INPUT.type("123456789012")));

        assertEquals(app().elements().attribute(PersonalDetails.Fields.MOBILE_INPUT, "value"), "1234567890",
                "Input beyond 10 digits should be truncated by maxlength");
    }

    @Test(groups = "form", description = "FORM-04 -- Non-numeric mobile number fails pattern validation and blocks submit")
    public void form04_mobileNonNumericRejected() {
        open(PRACTICE_FORM);
        fillRequiredFields("Priya", "Sharma", PersonalDetails.Gender.FEMALE_RADIO, "abcdefghij");
        submitForm();

        assertFalse(app().elements().attribute(PersonalDetails.Fields.MOBILE_INPUT, "validationMessage").isBlank(),
                "Letters in the mobile field should fail the \\d* pattern");
        assertFalse(app().elements().isVisible(ConfirmationModal.Labels.MODAL_TITLE),
                "Confirmation modal must not open with an invalid mobile number");
    }

    @Test(groups = "form", description = "FORM-05 -- Malformed email fails validation and blocks submit")
    public void form05_invalidEmailRejected() {
        open(PRACTICE_FORM);
        fillRequiredFields("Priya", "Sharma", PersonalDetails.Gender.FEMALE_RADIO, "9876543210");
        app().run(Flow.of(PersonalDetails.Fields.EMAIL_INPUT.type("priya.sharma@invalid")));
        submitForm();

        assertFalse(app().elements().attribute(PersonalDetails.Fields.EMAIL_INPUT, "validationMessage").isBlank(),
                "Email without a TLD should fail the pattern");
        assertFalse(app().elements().isVisible(ConfirmationModal.Labels.MODAL_TITLE),
                "Confirmation modal must not open with an invalid email");
    }

    @Test(groups = "form", description = "FORM-06 -- Subjects multi-select adds chips in order and removes a single chip")
    public void form06_subjectsMultiSelect() {
        open(PRACTICE_FORM);
        app().run(Flow.of(
                PracticeFormPage.Subjects.Fields.SUBJECTS_INPUT.typeAndPress("Maths", "ENTER"),
                PracticeFormPage.Subjects.Fields.SUBJECTS_INPUT.typeAndPress("Physics", "ENTER"),
                PracticeFormPage.Subjects.Fields.SUBJECTS_INPUT.typeAndPress("English", "ENTER")
        ));
        assertEquals(app().elements().allTexts(PracticeFormPage.Subjects.Labels.SELECTED_SUBJECT_CHIPS),
                List.of("Maths", "Physics", "English"));

        app().run(Flow.of(PracticeFormPage.Subjects.DynamicButtons.REMOVE_SUBJECT_BUTTON.click().withArgs("Physics")));
        assertEquals(app().elements().allTexts(PracticeFormPage.Subjects.Labels.SELECTED_SUBJECT_CHIPS),
                List.of("Maths", "English"));
    }

    @Test(groups = "form", description = "FORM-07 -- Hobby checkboxes check, uncheck, and stay idempotent")
    public void form07_hobbyCheckboxes() {
        open(PRACTICE_FORM);
        var sports  = PracticeFormPage.Hobbies.Options.SPORTS_CHECKBOX;
        var reading = PracticeFormPage.Hobbies.Options.READING_CHECKBOX;
        var music   = PracticeFormPage.Hobbies.Options.MUSIC_CHECKBOX;

        app().run(Flow.of(sports.set(true), music.set(true)));
        assertTrue(isChecked(sports), "Sports should be checked");
        assertFalse(isChecked(reading), "Reading should remain unchecked");
        assertTrue(isChecked(music), "Music should be checked");

        app().run(Flow.of(sports.toggle(), music.set(true)));
        assertFalse(isChecked(sports), "Toggling Sports should uncheck it");
        assertTrue(isChecked(music), "set(true) on a checked box must not uncheck it");
    }

    @Test(groups = "form", description = "FORM-08 -- Gender radio buttons are mutually exclusive")
    public void form08_genderRadioExclusive() {
        open(PRACTICE_FORM);
        app().run(Flow.of(PersonalDetails.Gender.FEMALE_RADIO.click()));
        assertTrue(isChecked(PersonalDetails.Gender.FEMALE_RADIO), "Female should be selected");

        app().run(Flow.of(PersonalDetails.Gender.MALE_RADIO.click()));
        assertTrue(isChecked(PersonalDetails.Gender.MALE_RADIO), "Male should be selected");
        assertFalse(isChecked(PersonalDetails.Gender.FEMALE_RADIO), "Selecting Male should clear Female");
        assertFalse(isChecked(PersonalDetails.Gender.OTHER_RADIO), "Other should remain unselected");
    }

    @Test(groups = "form", description = "FORM-09 -- City dropdown is disabled until a State is chosen, then filters by state")
    public void form09_dependentStateCityDropdowns() {
        open(PRACTICE_FORM);
        assertEquals(app().elements().attribute(PracticeFormPage.Address.Fields.CITY_INPUT, "disabled"), "true",
                "City should be disabled before a state is selected");

        app().run(Flow.of(PracticeFormPage.Address.Fields.STATE_INPUT.typeAndPress("Uttar Pradesh", "ENTER")));
        assertNull(app().elements().attribute(PracticeFormPage.Address.Fields.CITY_INPUT, "disabled"),
                "City should be enabled after a state is selected");

        app().run(Flow.of(PracticeFormPage.Address.Fields.CITY_INPUT.typeAndPress("Lucknow", "ENTER")));
        assertEquals(app().reader().query(PracticeFormPage.Address.Labels.SELECTED_STATE.getText()), "Uttar Pradesh");
        assertEquals(app().reader().query(PracticeFormPage.Address.Labels.SELECTED_CITY.getText()), "Lucknow");
    }

    @Test(groups = "form", description = "FORM-10 -- Date picker month/year dropdowns and day grid set Date of Birth")
    public void form10_datePickerDropdowns() {
        open(PRACTICE_FORM);
        app().run(Flow.of(
                PracticeFormPage.DateOfBirth.Buttons.CALENDAR_TOGGLE.click(),
                PracticeFormPage.DateOfBirth.Months.AUGUST.select(),
                selectYear("1990"),
                PracticeFormPage.DateOfBirth.DynamicButtons.DAY_CELL.click().withArgs("021")
        ));

        assertEquals(app().elements().attribute(PracticeFormPage.DateOfBirth.Fields.DATE_OF_BIRTH_INPUT, "value"),
                "21 Aug 1990");
    }

    @Test(groups = {"form", "known-defect"}, description = "FORM-11 -- Modal dismissal; KNOWN DEFECT: Close button does not close it, Escape does")
    public void form11_closeConfirmationModal() {
        open(PRACTICE_FORM);
        fillRequiredFields("Priya", "Sharma", PersonalDetails.Gender.OTHER_RADIO, "9876543210");
        submitForm();
        app().elements().waitForVisible(ConfirmationModal.Buttons.CLOSE_BUTTON);

        // KNOWN DEFECT (FORM-11): Close is clickable and receives the click (native and JS) but the
        // modal stays open. Asserting the actual broken behavior as a regression anchor.
        app().run(Flow.of(ConfirmationModal.Buttons.CLOSE_BUTTON.click()));
        assertTrue(app().elements().isVisible(ConfirmationModal.Labels.MODAL_TITLE),
                "KNOWN DEFECT -- modal still open after clicking Close; expected it to close");

        app().run(pressEscapeOn(ConfirmationModal.Buttons.CLOSE_BUTTON));
        app().elements().waitForHidden(ConfirmationModal.Buttons.CLOSE_BUTTON);
        assertFalse(app().elements().isVisible(ConfirmationModal.Labels.MODAL_TITLE),
                "Escape should dismiss the modal");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SELECT MENUS  (SEL-01 ... SEL-02)
    // ═════════════════════════════════════════════════════════════════════════

    @Test(groups = "dropdown", description = "SEL-01 -- Native single select keeps exactly the last chosen option")
    public void sel01_singleSelect() {
        open(SELECT_MENU);
        app().run(Flow.of(SelectMenuPage.OldStyleSelect.Colors.PURPLE.select()));
        assertEquals(app().reader().query(SelectMenuPage.OldStyleSelect.Labels.SELECTED_COLOR.getText()), "Purple");

        app().run(Flow.of(SelectMenuPage.OldStyleSelect.Colors.AQUA.select()));
        assertEquals(app().elements().allTexts(SelectMenuPage.OldStyleSelect.Labels.SELECTED_COLOR), List.of("Aqua"),
                "A single select should hold only the latest option");
    }

    @Test(groups = "dropdown", description = "SEL-02 -- Native multi-select accumulates several selected options")
    public void sel02_multiSelect() {
        open(SELECT_MENU);
        app().run(Flow.of(
                SelectMenuPage.CarsMultiSelect.Cars.VOLVO.select(),
                SelectMenuPage.CarsMultiSelect.Cars.AUDI.select()
        ));

        assertEquals(app().elements().allTexts(SelectMenuPage.CarsMultiSelect.Labels.SELECTED_CARS),
                List.of("Volvo", "Audi"));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FILES  (FILE-01 ... FILE-02)
    // ═════════════════════════════════════════════════════════════════════════

    @Test(groups = "file", description = "FILE-01 -- Uploading an image shows its file name in the result path")
    public void file01_uploadImage() {
        open(UPLOAD_DOWNLOAD);
        Path picture = uploadFixture();
        app().run(Flow.of(UploadDownloadPage.Fields.FILE_UPLOAD.upload(picture.toString())));

        String shown = app().reader().query(UploadDownloadPage.Labels.UPLOADED_FILE_PATH.getText());
        assertTrue(shown.endsWith(picture.getFileName().toString()),
                "Uploaded path should end with the file name, was: " + shown);
    }

    @Test(groups = "file", description = "FILE-02 -- Download button saves a valid JPEG to the downloads directory")
    public void file02_downloadImage() throws IOException, InterruptedException {
        Path target = DOWNLOAD_DIR.resolve(DOWNLOAD_FILE);
        Files.deleteIfExists(target);

        open(UPLOAD_DOWNLOAD);
        app().run(Flow.of(UploadDownloadPage.Buttons.DOWNLOAD_BUTTON.click()));

        Path downloaded = awaitDownload(target, 15_000);
        assertNotNull(downloaded, "Expected " + DOWNLOAD_FILE + " in " + DOWNLOAD_DIR);
        try (InputStream in = Files.newInputStream(downloaded)) {
            byte[] magic = in.readNBytes(2);
            assertEquals(magic, new byte[]{(byte) 0xFF, (byte) 0xD8}, "File should start with the JPEG SOI marker");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BUTTONS / CHECKBOX TREE  (BTN-01, CHK-01 ... CHK-02)
    // ═════════════════════════════════════════════════════════════════════════

    @Test(groups = "button", description = "BTN-01 -- Button with a generated id is clicked by its text")
    public void btn01_dynamicClick() {
        open(BUTTONS);
        app().run(Flow.of(ButtonsPage.Buttons.CLICK_ME_BUTTON.click()));

        assertEquals(app().reader().query(ButtonsPage.Labels.DYNAMIC_CLICK_MESSAGE.getText()),
                "You have done a dynamic click");
    }

    @Test(groups = "checkbox", description = "CHK-01 -- Checking a parent node checks every descendant")
    public void chk01_parentChecksDescendants() {
        open(CHECK_BOX);
        app().run(Flow.of(
                CheckBoxPage.Nodes.HOME_CHECKBOX.set(true),
                CheckBoxPage.Buttons.EXPAND_HOME_TOGGLE.click()
        ));

        for (CheckBoxPage.Nodes child : List.of(
                CheckBoxPage.Nodes.DESKTOP_CHECKBOX,
                CheckBoxPage.Nodes.DOCUMENTS_CHECKBOX,
                CheckBoxPage.Nodes.DOWNLOADS_CHECKBOX)) {
            assertEquals(app().elements().attribute(child, "aria-checked"), "true", child + " should be checked");
        }
        List<String> selected = app().elements().allTexts(CheckBoxPage.Labels.SELECTED_ITEMS);
        assertEquals(selected.size(), 17, "Home and all 16 descendants should be listed, got " + selected);
        assertEquals(selected.get(0), "home");
    }

    @Test(groups = "checkbox", description = "CHK-02 -- Unchecking one child leaves the parent partially checked")
    public void chk02_childUncheckMakesParentPartial() {
        open(CHECK_BOX);
        app().run(Flow.of(
                CheckBoxPage.Nodes.HOME_CHECKBOX.set(true),
                CheckBoxPage.Buttons.EXPAND_HOME_TOGGLE.click(),
                CheckBoxPage.Nodes.DESKTOP_CHECKBOX.set(false)
        ));

        assertNotEquals(app().elements().attribute(CheckBoxPage.Nodes.HOME_CHECKBOX, "aria-checked"), "true",
                "Home should no longer be fully checked");
        List<String> selected = app().elements().allTexts(CheckBoxPage.Labels.SELECTED_ITEMS);
        assertFalse(selected.contains("home"), "Home should drop out of the selection");
        assertFalse(selected.contains("desktop"), "Desktop should drop out of the selection");
        assertTrue(selected.contains("documents"), "Documents should still be selected");
    }
}
