package core.utils;

import Elements.interfacesv1.FileInputElement; // v1
import Elements.interfacesv1.Element; // base if needed
import Elements.interfacesv1.ReadOnlyElement; // if table headers treated as read-only
import core.resolvers.locator.LocatorResolverV1;
import org.openqa.selenium.WebDriver;
import core.driver.DriverContext;
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

    public interface TableElementV1 extends ReadOnlyElement { // minimal v1 table contract
        String getHeaderLocator();
        String getRowLocator();
        @Override default String getTextLocator(){ return getHeaderLocator(); }
        @Override default Object[] getArgs(){ return new Object[0]; }
        default String getPropertyFile(){ return getExternalFileName(); }
    }

    /**
     * Inserts one row of data into a dynamic table footer by mapping headers to values.
     *
     * @param fieldNameToValue Map of header names to values
     * @param tableElement     Table element enum used for locating headers/inputs
     */
    public static void insertRowInTable(Map<String, String> fieldNameToValue, TableElementV1 tableElement) {
        try {
            WebDriver driver = DriverContext.getDriver();
            By headersBy = LocatorResolverV1.getLocator(tableElement.getExternalFileName(), tableElement.getHeaderLocator());
            List<WebElement> headers = driver.findElements(headersBy);
            List<String> headerNames = new ArrayList<>();
            for (WebElement header : headers) headerNames.add(header.getText().trim());
            List<WebElement> inputFields = driver.findElements(By.cssSelector("#outputTable tfoot input"));
            if (headerNames.size() != inputFields.size()) throw new RuntimeException("Header count and input fields count mismatch in table.");
            for (int i = 0; i < headerNames.size(); i++) {
                String headerName = headerNames.get(i);
                if (!fieldNameToValue.containsKey(headerName)) { warn.log("[TABLE] No value provided for field: '" + headerName + "', skipping input."); continue; }
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
            TableElementV1 tableElement,
            Map<String, DataGenerator.FieldType> fieldTypeMap
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
            WebElement addButton = driver.findElement(LocatorResolverV1.getLocator(tableElement.getExternalFileName(), "IMPORT_RECORDS_TABLE_ADD_ROW_BUTTON"));
            DOMUtils.scrollToElement(addButton);
            addButton.click();
            info.log("[TABLE] Clicked 'Add Row' button successfully.");


            // 6. Click "Next" button to proceed
            WebElement nextButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@class, 'btn btn-primary ml-1')]")));
            DOMUtils.scrollToElement(nextButton);
            nextButton.click();
            info.log("[POPUP] Clicked 'Next' button successfully.");


        } catch (Exception e) {
            error.log("Failed to insert new records process.");
            throw new RuntimeException("Failed during Insert New Records flow", e);
        }
    }

    public static List<String> getColumnHeaders(TableElementV1 tableElement) {
        try {
            WebDriver driver = DriverContext.getDriver();
            By headerBy = LocatorResolverV1.getLocator(tableElement.getExternalFileName(), tableElement.getHeaderLocator());
            List<WebElement> headerElements = driver.findElements(headerBy);
            List<String> headers = new ArrayList<>();
            for (WebElement header : headerElements) {
                String text = header.getText().trim();
                if (!text.isEmpty()) headers.add(text);
            }
            info.table("[HEADERS] Found columns: " + headers);
            return headers;
        } catch (Exception e) {
            error.log("Failed to get column headers for table: " + tableElement.getDisplayText());
            throw new RuntimeException("Could not read table headers", e);
        }
    }

    public static List<Map<String, String>> getRow(
            TableElementV1 tableElement,
            Integer rowNumber,
            @Nullable Map<String, Object> columnData,
            boolean areMultipleRows
    ) {
        try {
            WebDriver driver = DriverContext.getDriver();
            int startIndex = (rowNumber == null) ? 0 : rowNumber - 1;
            List<String> headers = getColumnHeaders(tableElement);
            By rowsBy = LocatorResolverV1.getLocator(tableElement.getExternalFileName(), tableElement.getRowLocator());
            List<WebElement> rows = driver.findElements(rowsBy);
            if (rows.isEmpty()) throw new RuntimeException("No rows found for table: " + tableElement.getDisplayText());
            List<Map<String, String>> rowDataList = new ArrayList<>();
            int endIndex = areMultipleRows ? rows.size() : startIndex + 1;
            for (int i = startIndex; i < endIndex && i < rows.size(); i++) {
                WebElement row = rows.get(i);
                List<WebElement> cells = row.findElements(By.xpath("./td"));
                Map<String, String> rowData = new LinkedHashMap<>();
                for (int c = 0; c < headers.size() && c < cells.size(); c++) {
                    String header = headers.get(c);
                    String cellText = cells.get(c).getText().trim();
                    rowData.put(header, cellText);
                }
                rowDataList.add(rowData);
            }
            info.table("[ROWS] Extracted " + rowDataList.size() + " row(s) from table.");
            return rowDataList;
        } catch (Exception e) {
            error.log("Failed to extract row data for table: " + tableElement.getDisplayText());
            throw new RuntimeException("Could not read table rows", e);
        }
    }
}
