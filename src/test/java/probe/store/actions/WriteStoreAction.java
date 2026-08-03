package probe.store.actions;

import core.actions.Action;
import core.actions.ActionCapability;
import core.engine.Executor;
import domain.automation.web.locator.LocatorDescriptor;
import probe.store.StoreCapabilities;
import probe.store.StoreExecutor;

public final class WriteStoreAction implements Action {

    private final String key;
    private final String value;

    public WriteStoreAction(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void perform(Executor executor) {
        ((StoreExecutor) executor).write(key, value);
    }

    @Override
    public LocatorDescriptor resolve(Executor executor) {
        return null;
    }

    @Override
    public ActionCapability capability() {
        return StoreCapabilities.WRITE;
    }

    @Override
    public String elementLabel() {
        return "store[" + key + "]";
    }

    @Override
    public String operationLabel() {
        return "write";
    }
}
