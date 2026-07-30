/**
 * Web-domain element role markers and enum type discovery.
 *
 * <p>{@link elements.meta.ElementRole} defines the locator roles available to
 * {@link elements.api.UIElement} implementors (PRIMARY, INPUT, TRIGGER, etc.).
 * {@link elements.meta.EnumClassRegistry} provides runtime discovery of
 * enum types for dynamic element population.</p>
 *
 * <h3>Domain ownership (ADR-021 addendum, runtime-redesign I6.2)</h3>
 * <p>All types in this package are <strong>Web-domain vocabulary</strong> (role
 * layer). Physical relocation to {@code domain.automation.web.vocabulary.role}
 * is gated on the I6.4 Class Migration Matrix execution. ADR-021 addendum.</p>
 */
package elements.meta;

