package core.resolvers.locator.sync;

import elements.api.capability.Clickable;
import elements.api.capability.ReadOnly;
import elements.api.capability.Selectable;
import elements.api.capability.Typeable;

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
