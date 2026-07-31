package probe.store.actions;

import core.actions.Action;
import core.actions.ActionCapability;
import core.engine.Executor;
import domain.automation.web.locator.LocatorDescriptor;
import probe.store.StoreCapabilities;
import probe.store.StoreExecutor;

public final class ReadStoreAction implements Action {

    private final String key;
    private String result;

    public ReadStoreAction(String key) {
        this.key = key;
    }

    @Override
    public void perform(Executor executor) {
        result = ((StoreExecutor) executor).read(key);
    }

    @Override
    public LocatorDescriptor resolve(Executor executor) {
        return null;
    }

    @Override
    public ActionCapability capability() {
        return StoreCapabilities.READ;
    }

    @Override
    public String elementLabel() {
        return "store[" + key + "]";
    }

    @Override
    public String operationLabel() {
        return "read";
    }

    public String getResult() {
        return result;
    }
}
