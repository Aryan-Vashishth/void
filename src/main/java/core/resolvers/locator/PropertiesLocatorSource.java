package core.resolvers.locator;

import core.resolvers.locator.properties.PropertiesFileLocatorReaderV1;

import java.util.Locale;

/**
 * {@link LocatorSource} for {@code .properties} bundles on the classpath.
 *
 * <p>Currently delegates to {@link PropertiesFileLocatorReaderV1#getRaw(String, String)}
 * — i.e. simple {@code PropertiesReader} lookup under
 * {@link LocatorPaths#PROPERTIES_BASE}. The legacy layered/cached
 * {@code ConfigLoader} path used by {@code ElementLocatorResolverV1} is preserved
 * separately for backward compatibility and may be migrated to this source in
 * Phase&nbsp;3.</p>
 */
public final class PropertiesLocatorSource implements LocatorSource {

    /** Singleton — backing reader holds its own cache. */
    public static final PropertiesLocatorSource INSTANCE = new PropertiesLocatorSource();

    private PropertiesLocatorSource() {}

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".properties");
    }

    @Override
    public String readRaw(LocatorRequest request) {
        if (!supports(request.fileName())) {
            throw new IllegalArgumentException(
                    "PropertiesLocatorSource does not support fileName: " + request.fileName());
        }
        return PropertiesFileLocatorReaderV1.getRaw(request.fileName(), request.key());
    }

    @Override
    public String name() { return "properties"; }
}

