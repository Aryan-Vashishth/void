<h1 align="center">DemoQA Automation</h1>

Automation suite for [demoqa.com](https://demoqa.com), built on the VOID interaction runtime.
SauceDemo covers commerce flows; this suite covers the form controls SauceDemo does not have:
number input, multi-select, checkboxes, radios, file upload and file download.

Framework concepts (page contracts, flows, external locators) are documented in the
[SauceDemo README](../saucedemo/README.md). This page covers only what is specific to DemoQA.

---

## Quick start

> Requires JDK 17+, Maven 3.8+, Chrome. Selenium Manager auto-downloads ChromeDriver.

```powershell
mvn clean test "-Dsurefire.suiteXmlFiles=src/testNgXml/demoqa.xml"
```

Headless:

```bash
mvn clean test -Dsurefire.suiteXmlFiles=src/testNgXml/demoqa.xml -Dheadless=true
```

**Report:** `logs/extent-report/DemoQASuite.html` (Extent Spark). Allure results are written too;
`mvn allure:serve` opens them.

SauceDemo and DemoQA together, one Extent report at `logs/extent-report/AssignmentSuite.html`:

```powershell
mvn clean test "-Dsurefire.suiteXmlFiles=src/testNgXml/assignment.xml"
```

---

## Coverage

| Component | Where | Test IDs |
|---|---|---|
| Text input | Practice Form: name, email, address | FORM-01, FORM-02, FORM-05 |
| Number input | Practice Form: mobile (`pattern=\d*`, length 10) | FORM-03, FORM-04 |
| Dropdown (native) | Select Menu: old style select; date picker month/year | SEL-01, FORM-10 |
| Dropdown (dependent) | Practice Form: State, then City | FORM-09 |
| Multi-select | Practice Form: Subjects chips; Select Menu: native `<select multiple>` | FORM-06, SEL-02 |
| Checkbox | Practice Form: Hobbies; Check Box tree with parent/child propagation | FORM-07, CHK-01, CHK-02 |
| Radio | Practice Form: Gender | FORM-08 |
| Button | Submit, modal Close, dynamic-id button, Download | FORM-01, FORM-11, BTN-01, FILE-02 |
| File upload | Practice Form picture; Upload page | FORM-01, FILE-01 |
| File download | Upload and Download page | FILE-02 |
| Add form, all inputs | Practice Form end to end, verified via confirmation modal | FORM-01 |

18 automated, 20 manual. Manual cases with steps, data, expected/actual results, severity and
priority: [`DemoQA_Test_Cases.csv`](../../../../resources/examples/testcases/DemoQA_Test_Cases.csv).
The master workbook for both applications (summary, all 65 cases, requirement coverage,
defects): [`Assignment_Master_Test_Cases.xlsx`](../../../../resources/examples/testcases/Assignment_Master_Test_Cases.xlsx).

### Known defect

**FORM-11:** the confirmation modal's Close button takes the click (native and JavaScript) but
the modal stays open. Escape closes it. The test asserts the current behavior so it will flag a
change, the same way SauceDemo handles LAND-12. It is in the `known-defect` TestNG group, which the
Extent report shows as a warning rather than a plain pass.

### Not automated

BTN-02 (double click) and BTN-03 (right click) are in the manual sheet. The runtime has no
double-click or context-click capability yet.

---

## DemoQA specifics

- **Ads.** DemoQA adds ad iframes and a fixed banner that cover the lower form controls.
  `DemoQATest.open()` removes them after each navigation.
- **Downloads.** `@BeforeClass` sets the `downloadsDir` system property to
  `target/downloads/demoqa`. `SeleniumDriverFactory` reads it when each session starts.
- **Year dropdown.** The date picker's year select runs from 1900 to 2100, too many values for a
  `Dropdown` enum, so `selectYear()` selects it by text through the engine. Months use the
  typed `Dropdown` enum.
- **Locator sync.** Run `mvn compile` before `JsonMigratorCli --sync`. The CLI reads
  `.properties` from `target/classes`, so if you skip compile the JSON is written with keys
  where the locators should be.

---

## Files

```
src/main/java/examples/
 ├── pages/demoqa/
 │    ├── PracticeFormPage.java      student registration form + confirmation modal
 │    ├── SelectMenuPage.java        native single and multi selects
 │    ├── UploadDownloadPage.java    upload input, download link
 │    ├── ButtonsPage.java           dynamic-id button
 │    └── CheckBoxPage.java          rc-tree checkbox tree
 ├── tests/DemoQATest.java           18 test methods
 └── listeners/ExtentReportListener.java
src/main/resources/examples/
 ├── pages/demoqa/<Page>/locators.{properties,json}
 ├── testdata/demoqa/profile-picture.png
 ├── testcases/DemoQA_Test_Cases.csv
 └── testcases/Assignment_Master_Test_Cases.xlsx
src/testNgXml/demoqa.xml        DemoQA only
src/testNgXml/assignment.xml    SauceDemo + DemoQA
```
