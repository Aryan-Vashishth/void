package core.utils;

import Elements.interfacesv1.FileInputElement;
import core.resolvers.locator.LocatorResolverV1;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;

public class Upload extends BaseUtils {

    // Configurable upload base path from config
    private static final String UPLOAD_BASE_PATH = ConfigLoader.get("upload.base.path", "uploads/");


    /**
     * Uploads a file into a file input element, optionally switching to iframe first.
     *
     * @param fileElement Enum representing the file input
     * @param filePath    Path to the file to upload (relative to upload.base.path if not absolute)
     */
    public static void uploadFile(FileInputElement fileElement, String filePath) {
        try {
            By locator = LocatorResolverV1.getLocator(fileElement); // v1 resolution
            WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(locator));

            String absolutePath = FileUtils.resolveResourceAbsolutePath(UPLOAD_BASE_PATH, filePath, null);

            fileInput.sendKeys(absolutePath);

            info.success("File uploaded into: " + fileElement.getDisplayText() + " → " + absolutePath);

        } catch (Exception e) {
            error.upload("File upload failed for: " + fileElement.getDisplayText());
            error.upload(e.getMessage());
            throw new RuntimeException("File upload failed for: " + fileElement.getDisplayText(), e);
        }
    }

    /**
     * Resolves the absolute path of the file to upload.
     * If the file path is absolute, returns it.
     * Otherwise, attempts to load it as a resource relative to UPLOAD_BASE_PATH.
     *
     * @param filePath The file name or absolute path.
     * @return The absolute path on disk.
     */
    private static String resolveUploadFileAbsolutePath(String filePath) {
        File uploadFile;
        if (new File(filePath).isAbsolute()) {
            uploadFile = new File(filePath);
        } else {
            String resourcePath = UPLOAD_BASE_PATH + filePath;
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            java.net.URL resourceUrl = classLoader.getResource(resourcePath);
            if (resourceUrl == null) {
                throw new RuntimeException("Upload file not found in test resources: " + resourcePath);
            }
            uploadFile = new File(resourceUrl.getFile());
        }

        if (!uploadFile.exists()) {
            throw new RuntimeException("Upload file not found: " + uploadFile.getAbsolutePath());
        }

        return uploadFile.getAbsolutePath();
    }
}
