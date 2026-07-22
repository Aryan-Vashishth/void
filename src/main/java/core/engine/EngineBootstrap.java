package core.engine;

import core.driver.DriverFactory;

/**
 * Opaque initialization token passed from {@link core.runtime.VOIDBuilder} to
 * {@link UIEngineFactory}.
 *
 * <p>Decouples the factory contract from Selenium-specific types. The factory hands the
 * bootstrap to the engine it constructs, which knows how to consume it.</p>
 *
 * <p>Currently only {@link FromProfile} is used. Additional variants may be introduced
 * when new engine types are added.</p>
 */
public sealed interface EngineBootstrap
        permits EngineBootstrap.FromProfile {

    /** Engine builds its own driver from the given profile. */
    record FromProfile(DriverFactory.Profile profile) implements EngineBootstrap {}

    /** Creates a bootstrap carrying a driver profile. */
    static EngineBootstrap fromProfile(DriverFactory.Profile profile) { return new FromProfile(profile); }
}
