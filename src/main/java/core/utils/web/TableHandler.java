package core.utils.web;

import elements.api.capability.ReadOnly;
import core.driver.DriverContext;
import elements.locator.LocatorDescriptor;
import core.engine.selenium.SeleniumEngine;
import core.resolvers.locator.api.LocatorRequest;
import core.resolvers.locator.api.LocatorResolvers;
import org.openqa.selenium.WebDriver;
import com.beust.jcommander.internal.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static core.logging.CustomLogger.info;
import static core.logging.CustomLogger.warn;
import static core.logging.CustomLogger.error;

/**
 * Static utility for reading and manipulating HTML tables via Selenium.
 *
 * @deprecated All methods in this class access {@link org.openqa.selenium.WebDriver} directly,
 *             violating ADR-007. Table operations should be performed through
 *             {@link core.engine.UIEngine} methods. This class will be removed once
 *             engine-agnostic table access is available on UIEngine.
 * @see core.engine.UIEngine
 */
@Deprecated(forRemoval = true)
public class TableHandler {

    private TableHandler() { /* static utility */ }

    public interface TableElementV1 extends ReadOnly { // minimal v1 table contract
        String getHeaderLocator();
        String getRowLocator();
        @Override default String getTextLocator(){ return getHeaderLocator(); }
        @Override default Object[] getArgs(){ return new Object[0]; }
        default String getPropertyFile(){ return getExternalFileName(); }
    }

    /**
     * @deprecated Accesses WebDriver directly in violation of ADR-007.
     *             Migrate to UIEngine-based table operations when available.
     */
    @Deprecated(forRemoval = true)
    public static void insertRowInTable(Map<String, String> fieldNameToValue, TableElementV1 tableElement) {
        try {
            WebDriver driver = DriverContext.getDriver();
            LocatorDescriptor headersDescriptor = LocatorResolvers.strict().resolveDescriptor(LocatorRequest.of(tableElement.getExternalFileName(), tableElement.getHeaderLocator()));
            By headersBy = SeleniumEngine.toBy(headersDescriptor);
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


    /**
     * @deprecated Accesses WebDriver directly in violation of ADR-007.
     *             Migrate to UIEngine-based table operations when available.
     */
    @Deprecated(forRemoval = true)
    public static List<String> getColumnHeaders(TableElementV1 tableElement) {
        try {
            WebDriver driver = DriverContext.getDriver();
            LocatorDescriptor headerDescriptor = LocatorResolvers.strict().resolveDescriptor(LocatorRequest.of(tableElement.getExternalFileName(), tableElement.getHeaderLocator()));
            By headerBy = SeleniumEngine.toBy(headerDescriptor);
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

    /**
     * @deprecated Accesses WebDriver directly in violation of ADR-007.
     *             Migrate to UIEngine-based table operations when available.
     */
    @Deprecated(forRemoval = true)
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
            LocatorDescriptor rowsDescriptor = LocatorResolvers.strict().resolveDescriptor(LocatorRequest.of(tableElement.getExternalFileName(), tableElement.getRowLocator()));
            By rowsBy = SeleniumEngine.toBy(rowsDescriptor);
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
