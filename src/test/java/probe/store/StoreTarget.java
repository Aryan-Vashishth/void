package probe.store;

import core.target.Target;

public enum StoreTarget implements Target {
    CONFIG, SESSION_VALUE, TEMP_BUFFER;

    @Override
    public String getDisplayText() {
        return "StoreTarget." + name();
    }
}
