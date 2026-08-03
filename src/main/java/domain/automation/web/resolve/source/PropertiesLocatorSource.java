package domain.automation.web.resolve.source;

import domain.automation.web.resolve.api.LocatorPaths;
import domain.automation.web.resolve.api.LocatorRequest;
import domain.automation.web.resolve.properties.PropertiesFileLocatorReader;

import java.util.Locale;

/**
 * {@link LocatorSource} for {@code .properties} bundles on the classpath.
 *
 * <p>Currently delegates to {@link PropertiesFileLocatorReader#getRaw(String, String)}
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
        return PropertiesFileLocatorReader.getRaw(request.fileName(), request.key());
    }

    @Override
    public String name() { return "properties"; }
}

