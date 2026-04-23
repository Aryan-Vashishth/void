package core.resolvers.locator.source;

import core.resolvers.locator.api.LocatorRequest;
import core.resolvers.locator.json.JsonLocatorReaderV1;

import java.util.Locale;

/**
 * {@link LocatorSource} for {@code .json} locator files on the classpath.
 *
 * <p>Delegates to {@link JsonLocatorReaderV1#getRaw(String, String)}, which handles
 * dot-path traversal with deep-find fallback. See {@code JsonNodeLookup} for the
 * extracted traversal logic.</p>
 */
public final class JsonLocatorSource implements LocatorSource {

    /** Singleton — JSON reader holds its own cache. */
    public static final JsonLocatorSource INSTANCE = new JsonLocatorSource();

    private JsonLocatorSource() {}

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".json");
    }

    @Override
    public String readRaw(LocatorRequest request) {
        if (!supports(request.fileName())) {
            throw new IllegalArgumentException(
                    "JsonLocatorSource does not support fileName: " + request.fileName());
        }
        return JsonLocatorReaderV1.getRaw(request.fileName(), request.key());
    }

    @Override
    public String name() { return "json"; }
}

