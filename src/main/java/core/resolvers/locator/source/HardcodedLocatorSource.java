package core.resolvers.locator.source;

import core.resolvers.locator.api.LocatorRequest;

/**
 * {@link LocatorSource} for "hardcoded" templates — i.e. requests where
 * {@link LocatorRequest#fileName()} is {@code null}. The {@link LocatorRequest#key()}
 * is itself the template and is returned verbatim.
 */
public final class HardcodedLocatorSource implements LocatorSource {

    /** Singleton — this source is stateless. */
    public static final HardcodedLocatorSource INSTANCE = new HardcodedLocatorSource();

    private HardcodedLocatorSource() {}

    @Override
    public boolean supports(String fileName) {
        return fileName == null;
    }

    @Override
    public String readRaw(LocatorRequest request) {
        if (!supports(request.fileName())) {
            throw new IllegalArgumentException(
                    "HardcodedLocatorSource only supports null fileName; got: " + request.fileName());
        }
        return request.key();
    }

    @Override
    public String name() { return "hardcoded"; }
}

