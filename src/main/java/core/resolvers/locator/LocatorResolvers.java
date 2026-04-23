package core.resolvers.locator;

import java.util.List;

/**
 * Static façade exposing two preconfigured {@link LocatorResolver} singletons that
 * back the legacy {@code ElementLocatorResolverV1} and {@code LocatorResolverV1}
 * façades.
 *
 * <p>New code should prefer {@link #strict()} (or build a custom instance via
 * {@link LocatorResolver#builder()}) instead of the legacy static façades.</p>
 */
public final class LocatorResolvers {

    /**
     * STRICT template policy + simple {@link PropertiesLocatorSource} (uncached, MAIN-only).
     * Mirrors the legacy {@code LocatorResolverV1} contract.
     */
    private static final LocatorResolver STRICT = LocatorResolver.builder()
            .policy(LocatorTemplate.Policy.STRICT)
            .registry(LocatorSourceRegistry.DEFAULT)
            .build();

    /**
     * PAD_LAST template policy + {@link LayeredPropertiesLocatorSource} (cached, layered).
     * Mirrors the legacy {@code ElementLocatorResolverV1} contract — last-arg padding plus
     * the {@code ConfigLoader.Layered} bundle behaviour.
     */
    private static final LocatorResolver LEGACY_PADDED = LocatorResolver.builder()
            .policy(LocatorTemplate.Policy.PAD_LAST)
            .registry(new LocatorSourceRegistry(List.of(
                    HardcodedLocatorSource.INSTANCE,
                    LayeredPropertiesLocatorSource.INSTANCE,
                    JsonLocatorSource.INSTANCE
            )))
            .build();

    private LocatorResolvers() {}

    /** Strict resolver — recommended for new code. */
    public static LocatorResolver strict()       { return STRICT; }

    /** Legacy padded resolver — maintained for backward compatibility with {@code ElementLocatorResolverV1}. */
    public static LocatorResolver legacyPadded() { return LEGACY_PADDED; }
}

