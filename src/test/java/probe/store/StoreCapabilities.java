package probe.store;

import core.actions.ActionCapability;

public final class StoreCapabilities {

    public static final ActionCapability WRITE = ActionCapability.of("store.write");
    public static final ActionCapability READ  = ActionCapability.of("store.read");

    private StoreCapabilities() {}
}
