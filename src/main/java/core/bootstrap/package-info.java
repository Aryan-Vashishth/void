/**
 * {@code core.bootstrap} — Framework lifecycle initialisation.
 *
 * <p>Contains {@link core.bootstrap.FrameworkBootstrap}, the one-time, idempotent init
 * gate that validates classpath configs and seeds the utils configuration
 * store before any driver or test logic executes.</p>
 */
package core.bootstrap;

