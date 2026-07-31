package probe.store.actions;

import core.actions.Action;
import core.actions.ActionCapability;
import core.engine.Executor;
import domain.automation.web.locator.LocatorDescriptor;
import probe.store.StoreExecutor;

public final class ClearStoreAction implements Action {

    @Override
    public void perform(Executor executor) {
        ((StoreExecutor) executor).clear();
    }

    @Override
    public LocatorDescriptor resolve(Executor executor) {
        return null;
    }

    @Override
    public ActionCapability capability() {
        return ActionCapability.UNKNOWN;
    }

    @Override
    public String elementLabel() {
        return "store";
    }

    @Override
    public String operationLabel() {
        return "clear";
    }
}
