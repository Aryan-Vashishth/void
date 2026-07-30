package probe.store;

import core.engine.EngineConfig;
import core.engine.Executor;

import java.util.HashMap;
import java.util.Map;

public final class StoreExecutor implements Executor {

    private final Map<String, String> store = new HashMap<>();

    @Override
    public void initialize(EngineConfig config) {}

    @Override
    public void shutdown() {
        store.clear();
    }

    @Override
    public String getEngineName() {
        return "store";
    }

    public void write(String key, String value) {
        store.put(key, value);
    }

    public String read(String key) {
        return store.get(key);
    }

    public void clear() {
        store.clear();
    }

    public Map<String, String> snapshot() {
        return Map.copyOf(store);
    }
}
