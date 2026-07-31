package core.resolvers.locator.sync;

import domain.automation.web.vocabulary.capability.Clickable;
import domain.automation.web.vocabulary.capability.ReadOnly;
import domain.automation.web.vocabulary.capability.Selectable;
import domain.automation.web.vocabulary.capability.Typeable;

/** Minimal page fixture for LocatorSync unit tests. Not a real page. */
interface SyncTestFixturePage {
    enum Inputs implements Typeable   { USERNAME, EMAIL }
    enum Actions implements Clickable { SUBMIT, CANCEL }
    enum Labels implements ReadOnly   { ERROR_MSG }
    enum Dropdowns implements Selectable {
        COUNTRY, STATE;
        @Override public String getTriggerLocator()   { return name(); }
        @Override public String getListLocator()      { return name() + "_LIST"; }
        @Override public String getExternalFileName() { return null; }
        @Override public Object[] getArgs()           { return new Object[]{name()}; }
    }
}
