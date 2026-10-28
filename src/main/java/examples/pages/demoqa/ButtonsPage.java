package examples.pages.demoqa;

import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;

/** Buttons -- https://demoqa.com/buttons */
public interface ButtonsPage {

    enum Buttons implements Clickable {
        CLICK_ME_BUTTON;        // id is generated per render; located by text
    }

    enum Labels implements ReadOnly {
        DYNAMIC_CLICK_MESSAGE;
    }
}
