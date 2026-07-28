package core.engine.selenium;

import core.engine.EngineBootstrap;
import core.engine.EngineRegistrar;
import core.engine.UIEngine;

/**
 * {@link EngineRegistrar} SPI entry for the Selenium WebDriver engine.
 *
 * <p>Discovered automatically via {@code META-INF/services/core.engine.EngineRegistrar}.
 * No edits to {@code core.engine.UIEngineFactory} are needed to activate this engine.</p>
 */
public final class SeleniumEngineRegistrar implements EngineRegistrar {

    @Override
    public String name() {
        return SeleniumEngine.ID;
    }

    @Override
    public UIEngine create(EngineBootstrap bootstrap) {
        if (bootstrap instanceof EngineBootstrap.FromProfile fp) {
            return new SeleniumEngine(fp.profile());
        }
        throw new IllegalArgumentException(
                "SeleniumEngine requires EngineBootstrap.FromProfile; got: "
                + bootstrap.getClass().getSimpleName());
    }
}
