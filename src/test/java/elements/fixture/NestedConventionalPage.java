package elements.fixture;

import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;
import domain.automation.web.vocabulary.capability.Typeable;

/**
 * Test-only page fixture with nested interfaces for verifying that the sync
 * pipeline and locator resolution work correctly for enums that are not direct
 * children of the top-level page interface.
 *
 * Resources live at elements/fixture/NestedConventionalPage/locators.json.
 */
public interface NestedConventionalPage {

    interface LoginSection {
        enum Fields implements Typeable   { USERNAME, PASSWORD }
        enum Actions implements Clickable { LOGIN_BUTTON }
    }

    interface Header {
        enum Labels implements ReadOnly   { PAGE_TITLE }
    }

    enum Footer implements Clickable { BACK_LINK }
}
