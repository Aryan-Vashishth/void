package core.engine;

import java.util.Properties;

/**
 * Opaque initialization token passed from {@link core.runtime.VOIDBuilder} to
 * {@link UIEngineFactory}.
 *
 * <p>Carries engine-owned settings as an opaque {@link Properties} map. The factory hands
 * the bootstrap to the registered engine, which interprets the settings.
 * {@code EngineBootstrap} itself has no knowledge of setting semantics -- key definitions
 * are owned by each {@link EngineRegistrar} implementation.</p>
 *
 * <p>Currently only {@link WithSettings} is used. Additional variants may be introduced
 * when new engine types require distinct initialization shapes.</p>
 */
public sealed interface EngineBootstrap
        permits EngineBootstrap.WithSettings {

    /** Opaque, engine-owned settings. Key-value pairs whose meaning is defined by the registrar. */
    record WithSettings(Properties settings) implements EngineBootstrap {}

    /** Creates a bootstrap carrying opaque engine settings. */
    static EngineBootstrap withSettings(Properties settings) { return new WithSettings(settings); }
}
