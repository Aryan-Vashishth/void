package elements.fixture;

import elements.api.capability.Clickable;
import elements.api.capability.ReadOnly;

/**
 * Test-only page used to verify Phase 5 conventional path resolution.
 * Resources live at elements/fixture/ConventionalTestPage/locators.json.
 */
public interface ConventionalTestPage {

    enum Buttons implements Clickable {
        SUBMIT, CANCEL;
    }

    enum Labels implements ReadOnly {
        STATUS_MESSAGE;
    }
}
