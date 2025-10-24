package Elements.Interfaces;

import org.openqa.selenium.By;

public interface Form {
    String getPropertyFile();
    By getRequiredElement();
    By getClickableElement();
    By getRequiredClickableElement();
    By getDropdownElement();
    By getCheckboxElement();
    By getSubmitButton();
    By getSaveAsDraftButton();
}
