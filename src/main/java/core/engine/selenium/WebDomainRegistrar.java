package core.engine.selenium;

import core.engine.DomainRegistrar;
import core.engine.EngineBootstrap;
import core.engine.Executor;
import core.engine.UIEngineFactory;

import java.util.Properties;

/**
 * Domain registrar for the Web (UI automation) domain.
 *
 * <p>Delegates executor creation to {@link UIEngineFactory}, which handles
 * engine-level dispatch (Selenium by default). Adding a second web-compatible
 * engine (e.g. Playwright) requires only a new {@link core.engine.EngineRegistrar}
 * entry -- no changes here.</p>
 *
 * <p>This class resides in {@code core.engine.selenium} as a temporary home
 * alongside {@link SeleniumEngineRegistrar}. It relocates to
 * {@code domain.automation.web.*} in I6.4 as part of the Class Migration Matrix
 * execution.</p>
 *
 * <p>Registered via {@code META-INF/services/core.engine.DomainRegistrar}.</p>
 */
public final class WebDomainRegistrar implements DomainRegistrar {

    /** Domain identifier for the Web (UI automation) domain. */
    public static final String ID = "web";

    @Override
    public String name() {
        return ID;
    }

    @Override
    public Executor createExecutor(Properties config, EngineBootstrap bootstrap) {
        return UIEngineFactory.create(config, bootstrap);
    }
}
