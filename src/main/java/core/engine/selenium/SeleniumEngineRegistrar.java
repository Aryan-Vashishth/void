package core.engine.selenium;

import core.driver.DriverFactory;
import core.engine.EngineBootstrap;
import core.engine.EngineRegistrar;
import core.engine.UIEngine;

import java.util.Arrays;
import java.util.Properties;

/**
 * {@link EngineRegistrar} SPI entry for the Selenium WebDriver engine.
 *
 * <p>Discovered automatically via {@code META-INF/services/core.engine.EngineRegistrar}.
 * No edits to {@code core.engine.UIEngineFactory} are needed to activate this engine.</p>
 *
 * <p>Reads the {@value PROFILE_KEY} setting from the bootstrap settings to select a
 * {@link DriverFactory.Profile}. Absent or blank defaults to
 * {@link DriverFactory.Profile#DEFAULT}.</p>
 */
public final class SeleniumEngineRegistrar implements EngineRegistrar {

    /** Settings key for the driver profile name. Matched by {@code VOIDBuilder}. */
    static final String PROFILE_KEY = "profile";

    @Override
    public String name() {
        return SeleniumEngine.ID;
    }

    @Override
    public UIEngine create(EngineBootstrap bootstrap) {
        if (bootstrap instanceof EngineBootstrap.WithSettings ws) {
            return new SeleniumEngine(resolveProfile(ws.settings()));
        }
        throw new IllegalArgumentException(
                "SeleniumEngine requires EngineBootstrap.WithSettings; got: "
                + bootstrap.getClass().getSimpleName());
    }

    private static DriverFactory.Profile resolveProfile(Properties settings) {
        String name = settings.getProperty(PROFILE_KEY);
        if (name == null || name.isBlank()) return DriverFactory.Profile.DEFAULT;
        try {
            return DriverFactory.Profile.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown DriverFactory.Profile '" + name + "'. Valid: "
                    + Arrays.toString(DriverFactory.Profile.values()));
        }
    }
}
