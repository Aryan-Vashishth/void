package examples.pages.demoqa;

import domain.automation.web.vocabulary.capability.Checkable;
import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;

/** Check Box tree -- https://demoqa.com/checkbox (rc-tree, state in aria-checked) */
public interface CheckBoxPage {

    enum Nodes implements Checkable {
        HOME_CHECKBOX,
        DESKTOP_CHECKBOX,
        DOCUMENTS_CHECKBOX,
        DOWNLOADS_CHECKBOX;
    }

    enum Buttons implements Clickable {
        EXPAND_HOME_TOGGLE;
    }

    enum Labels implements ReadOnly {
        SELECTED_ITEMS;         // one span per selected node (for allTexts)
    }
}
