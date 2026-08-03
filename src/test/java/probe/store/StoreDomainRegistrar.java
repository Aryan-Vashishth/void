package probe.store;

import core.engine.DomainRegistrar;
import core.engine.EngineBootstrap;
import core.engine.EngineConfig;
import core.engine.Executor;

import java.util.Properties;

public final class StoreDomainRegistrar implements DomainRegistrar {

    public static final String ID = "store";

    @Override
    public String name() {
        return ID;
    }

    @Override
    public Executor createExecutor(Properties config, EngineBootstrap bootstrap) {
        StoreExecutor executor = new StoreExecutor();
        executor.initialize(new EngineConfig(config));
        return executor;
    }
}
