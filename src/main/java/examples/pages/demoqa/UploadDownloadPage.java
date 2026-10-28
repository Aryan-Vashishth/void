package examples.pages.demoqa;

import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;
import domain.automation.web.vocabulary.capability.Uploadable;

/** Upload and Download -- https://demoqa.com/upload-download */
public interface UploadDownloadPage {

    enum Buttons implements Clickable {
        DOWNLOAD_BUTTON;        // <a download="sampleFile.jpeg" href="data:image/jpeg;base64,...">
    }

    enum Fields implements Uploadable {
        FILE_UPLOAD;
    }

    enum Labels implements ReadOnly {
        UPLOADED_FILE_PATH;     // "C:\fakepath\<file name>"
    }
}
