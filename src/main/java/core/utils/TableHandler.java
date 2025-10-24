package core.utils;

import Elements.Interfaces.FileInputElement;
import Elements.Interfaces.TableElement;
import org.openqa.selenium.WebDriver;
import core.driver.DriverContext;
import core.locators.PropertiesFileLocatorReader;
import com.beust.jcommander.internal.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TableHandler extends BaseUtils {
    WebDriver driver;


    public TableHandler() {
        initializer();
        driver = DriverContext.getDriver();
    }

    /**
     * Inserts one row of data into a dynamic table footer by mapping headers to values.
     *
     * @param fieldNameToValue Map of header names to values
     * @param tableElement     Table element enum used for locating headers/inputs
     */
    public static void insertRowInTable(Map<String, String> fieldNameToValue, TableElement tableElement) {
        try {
            WebDriver driver = DriverContext.getDriver();

            List<WebElement> headers = driver.findElements(
                    PropertiesFileLocatorReader.getLocator(tableElement.getPropertyFile(), tableElement.getHeaderKey(), tableElement.getArgs())
            );

            List<String> headerNames = new ArrayList<>();
            for (WebElement header : headers) {
                headerNames.add(header.getText().trim());
            }

            List<WebElement> inputFields = driver.findElements(By.cssSelector("#outputTable tfoot input"));

            if (headerNames.size() != inputFields.size()) {
                throw new RuntimeException("Header count and input fields count mismatch in table.");
            }

            for (int i = 0; i < headerNames.size(); i++) {
                String headerName = headerNames.get(i);
                if (!fieldNameToValue.containsKey(headerName)) {
                    warn.log("[TABLE] No value provided for field: '" + headerName + "', skipping input.");
                    continue;
                }

                String value = fieldNameToValue.get(headerName);
                WebElement input = inputFields.get(i);

                input.clear();
                input.sendKeys(value);

                info.log("[DATA ENTRY] Inserted '" + value + "' into field '" + headerName + "'");
            }


            info.log("[TABLE] Successfully inserted row into table.");

        } catch (Exception e) {
            error.log("Failed to insert row into table.");
            throw new RuntimeException("Row insertion failed.", e);
        }
    }

    public static void insertNewRecords(
            String filePath,
            FileInputElement fileUploadElement,
            @Nullable By iframeLocator,
            TableElement tableElement,
            Map<String, CommonElements.FieldType> fieldTypeMap
    ) {
        try {
            WebDriver driver = DriverContext.getDriver();
            // 1. Upload the file
            Upload.uploadFile(fileUploadElement, filePath);

            // 2. Switch back if iframe used (already handled in uploadFile)


            // 3. Generate test data
            Map<String, String> generatedTestData = DataGenerator.generateTestData(fieldTypeMap, null);

            // 4. Insert the generated row into the Import Records Table
            insertRowInTable(generatedTestData, tableElement);

            // 5. Add Row (Click 'Add' Button if needed)
            WebElement addButton = driver.findElement(
                    PropertiesFileLocatorReader.getLocator(tableElement.getPropertyFile(), "IMPORT_RECORDS_TABLE_ADD_ROW_BUTTON")
            );
            DOMUtils.scrollToElement(addButton);
            addButton.click();
            info.log("[TABLE] Clicked 'Add Row' button successfully.");


            // 6. Click "Next" button to proceed
            WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(@class, 'btn btn-primary ml-1')]")
            ));
            DOMUtils.scrollToElement(nextButton);
            nextButton.click();
            info.log("[POPUP] Clicked 'Next' button successfully.");


        } catch (Exception e) {
            error.log("Failed to insert new records process.");
            throw new RuntimeException("Failed during Insert New Records flow", e);
        }
    }

    public static List<String> getColumnHeaders(TableElement tableElement) {
        try {
            WebDriver driver = DriverContext.getDriver();
            List<WebElement> headerElements = driver.findElements(PropertiesFileLocatorReader.getLocator(
                    tableElement.getPropertyFile(),
                    tableElement.getHeaderKey(),
                    tableElement.getArgs()
            ));

            List<String> headers = new ArrayList<>();
            for (WebElement header : headerElements) {
                String text = header.getText().trim();
                if (!text.isEmpty()) {
                    headers.add(text);
                }
            }

            info.table("[HEADERS] Found columns: " + headers);
            return headers;

        } catch (Exception e) {
            error.log("Failed to get column headers for table: " + tableElement.getDisplayText());
            throw new RuntimeException("Could not read table headers", e);
        }
    }

    public static List<Map<String, String>> getRow(
            TableElement tableElement,
            Integer rowNumber,
            @Nullable Map<String, Object> columnData,
            boolean areMultipleRows
    ) {
        try {

            WebDriver driver = DriverContext.getDriver();
            int startIndex = (rowNumber == null) ? 0 : rowNumber - 1;

            List<String> headers = getColumnHeaders(tableElement);
            List<WebElement> rows = driver.findElements(
                    PropertiesFileLocatorReader.getLocator(tableElement.getPropertyFile(), tableElement.getRowKey(), tableElement.getArgs())
            );


            List<Map<String, String>> matchingRows = new ArrayList<>();

            for (int i = 0; i < rows.size(); i++) {
                if (!areMultipleRows && i == startIndex) {
                    WebElement row = rows.get(i);
                    Map<String, String> rowData = extractRowData(row, headers);
                    matchingRows.add(rowData);
                    break;
                }

                if (areMultipleRows && i >= startIndex) {
                    WebElement row = rows.get(i);
                    Map<String, String> rowData = extractRowData(row, headers);

                    boolean matches = true;
                    if (columnData != null && !columnData.isEmpty()) {
                        for (Map.Entry<String, Object> entry : columnData.entrySet()) {
                            String colName = entry.getKey();
                            Object expectedValue = entry.getValue();

                            if (!rowData.containsKey(colName)) {
                                matches = false;
                                break;
                            }

                            if (expectedValue != null &&
                                    !rowData.get(colName).equalsIgnoreCase(expectedValue.toString())) {
                                matches = false;
                                break;
                            }
                            // if expectedValue is null: include this column in output, but skip filtering
                        }
                    }

                    if (matches) {
                        matchingRows.add(rowData);
                    }
                }
            }

            info.table("Matching rows found: " + matchingRows.size());
            return matchingRows;

        } catch (Exception e) {
            error.log("Failed to get row(s) from table: " + e.getMessage());
            throw new RuntimeException("Failed to get row(s).", e);
        }
    }

    public static List<WebElement> getRowElements(
            TableElement tableElement,
            Integer rowNumber,
            @Nullable Map<String, Object> columnData,
            boolean areMultipleRows,
            String tagName
    ) {
        try {
            WebDriver driver = DriverContext.getDriver();

            int startIndex = (rowNumber == null) ? 0 : rowNumber - 1;
            List<WebElement> rows = driver.findElements(
                    PropertiesFileLocatorReader.getLocator(tableElement.getPropertyFile(), tableElement.getRowKey(), tableElement.getArgs())
            );

            List<WebElement> resultElements = new ArrayList<>();

            for (int i = 0; i < rows.size(); i++) {
                if (!areMultipleRows && i != startIndex) continue;
                if (areMultipleRows && i < startIndex) continue;

                WebElement row = rows.get(i);
                Map<String, String> rowData = extractRowData(row, getColumnHeaders(tableElement));

                boolean matches = true;
                if (columnData != null) {
                    for (Map.Entry<String, Object> entry : columnData.entrySet()) {
                        String key = entry.getKey();
                        Object val = entry.getValue();
                        if (!rowData.containsKey(key)) {
                            matches = false;
                            break;
                        }
                        if (val != null && !rowData.get(key).equalsIgnoreCase(val.toString())) {
                            matches = false;
                            break;
                        }
                    }
                }

                if (matches) {
                    resultElements.addAll(row.findElements(By.tagName(tagName)));
                    if (!areMultipleRows) break;
                }
            }

            info.log("[TABLE] Found " + resultElements.size() + " <" + tagName + "> tag(s) in matching row(s).");
            return resultElements;

        } catch (Exception e) {
            error.log("Failed to get row element(s): " + e.getMessage());
            throw new RuntimeException("Failed to get row element(s)", e);
        }
    }

    public static WebElement getRowElement(
            TableElement tableElement,
            Integer rowNumber,
            @Nullable Map<String, Object> columnData,
            String tagName
    ) {
        List<WebElement> elements = getRowElements(tableElement, rowNumber, columnData, false, tagName);
        if (elements.isEmpty()) {
            throw new RuntimeException("No element found in specified row for tag: <" + tagName + ">");
        }
        return elements.get(0);
    }


    private static Map<String, String> extractRowData(WebElement row, List<String> headers) {
        List<WebElement> cells = row.findElements(By.tagName("td"));
        Map<String, String> rowData = new LinkedHashMap<>();

        int offset = 2; // Skip the first 2 non-data columns: checkbox and 3-dots menu

        // Adjusted mapping: Start from td[2] onwards and map to headers[0] onwards
        for (int j = 0; j < headers.size() && (j + offset) < cells.size(); j++) {
            String header = headers.get(j);
//            String value = cells.get(j + offset).getText().trim();
//            String fullCellValue = ToolTipsResolver.resolveCommonTooltipByHover(cells.get(j + offset));
            String fullCellValue = ToolTipsResolver.resolveTooltipViaRole(cells.get(j + offset));
            rowData.put(header, fullCellValue);
        }
        return rowData;
    }
}
