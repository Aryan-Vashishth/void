package core.utils.web;

import core.utils.ConfigLoader;
import core.utils.io.FileUtils;
import elements.api.capability.Uploadable;
import core.driver.Waiter;
import core.resolvers.locator.api.LocatorResolvers;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;

import static core.logging.CustomLogger.info;
import static core.logging.CustomLogger.error;

/**
 * Static utility for automating file uploads through {@code <input type="file">} elements.
 *
 * <p>Resolves the target element's locator via {@link core.resolvers.locator.api.LocatorResolvers},
 * waits for it to be present, then calls {@link org.openqa.selenium.WebElement#sendKeys(CharSequence...)}
 * with the absolute file path.</p>
 *
 * <p>File paths can be absolute or relative to the configurable
 * {@code upload.base.path} (default: {@code uploads/}).</p>
 *
 * <p>Example:
 * <pre>
 *   Upload.uploadFile(MyElements.RESUME_INPUT, "test-resume.pdf");
 * </pre>
 *
 * @see elements.api.Uploadable
 * @see core.utils.ConfigLoader
 */
public class Upload {

    private Upload() { /* static utility */ }

    // Configurable upload base path from config
    private static final String UPLOAD_BASE_PATH = ConfigLoader.get("upload.base.path", "uploads/");


    /**
     * Uploads a file into a file input element, optionally switching to iframe first.
     *
     * @param fileElement Enum representing the file input
     * @param filePath    Path to the file to upload (relative to upload.base.path if not absolute)
     */
    public static void uploadFile(Uploadable fileElement, String filePath) {
        try {
            By locator = LocatorResolvers.strict().resolve(fileElement);
            WebElement fileInput = Waiter.get().until(ExpectedConditions.presenceOfElementLocated(locator));

            String absolutePath = FileUtils.resolveResourceAbsolutePath(UPLOAD_BASE_PATH, filePath, null);

            fileInput.sendKeys(absolutePath);

            info.success("File uploaded into: " + fileElement.getDisplayText() + " Ã¢â€ â€™ " + absolutePath);

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
