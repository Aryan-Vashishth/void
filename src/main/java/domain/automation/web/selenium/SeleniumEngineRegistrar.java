package domain.automation.web.selenium;

import domain.automation.web.selenium.driver.SeleniumDriverFactory;
import core.engine.EngineBootstrap;
import domain.automation.web.engine.EngineRegistrar;
import core.engine.Executor;
import domain.automation.web.engine.UIEngine;

import java.util.Arrays;
import java.util.Properties;

/**
 * {@link EngineRegistrar} SPI entry for the Selenium WebDriver engine.
 *
 * <p>Discovered automatically via {@code META-INF/services/core.engine.EngineRegistrar}.
 * No edits to {@code core.engine.UIEngineFactory} are needed to activate this engine.</p>
 *
 * <p>Reads the {@value PROFILE_KEY} setting from the bootstrap settings to select a
 * {@link SeleniumDriverFactory.Profile}. Absent or blank defaults to
 * {@link SeleniumDriverFactory.Profile#DEFAULT}.</p>
 */
public final class SeleniumEngineRegistrar implements EngineRegistrar {

    /** Settings key for the driver profile name. Matched by {@code VOIDBuilder}. */
    static final String PROFILE_KEY = "profile";

    @Override
    public String name() {
        return SeleniumEngine.ID;
    }

    @Override
    public Executor create(EngineBootstrap bootstrap) {
        if (bootstrap instanceof EngineBootstrap.WithSettings ws) {
            return new SeleniumEngine(resolveProfile(ws.settings()));
        }
        throw new IllegalArgumentException(
                "SeleniumEngine requires EngineBootstrap.WithSettings; got: "
                + bootstrap.getClass().getSimpleName());
    }

    private static SeleniumDriverFactory.Profile resolveProfile(Properties settings) {
        String name = settings.getProperty(PROFILE_KEY);
        if (name == null || name.isBlank()) return SeleniumDriverFactory.Profile.DEFAULT;
        try {
            return SeleniumDriverFactory.Profile.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown SeleniumDriverFactory.Profile '" + name + "'. Valid: "
                    + Arrays.toString(SeleniumDriverFactory.Profile.values()));
        }
    }
}