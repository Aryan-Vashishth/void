/**
 * Web-domain capability interfaces -- structural contract and action-emission layer.
 *
 * <p>Each interface in this package declares what locator keys an element type exposes,
 * its {@link elements.meta.ElementRole} mapping, and default methods that emit deferred
 * {@link core.actions.Action} objects (intent only -- never execution).</p>
 *
 * <h3>Domain ownership (ADR-021, runtime-redesign I3.3)</h3>
 * <p>All types in this package are <b>Web-domain vocabulary</b>. They are not kernel types.
 * The kernel ({@code core.actions} and its siblings) references capabilities solely via
 * the neutral {@link core.actions.ActionCapability} contract. Concrete capability types
 * ({@code Clickable}, {@code Typeable}, etc.) must never be imported by kernel packages.</p>
 *
 * <p>The open-set extension point for capabilities is {@link core.actions.ActionCapability#of(String)}.
 * A second domain adding its own capabilities does so through that factory, not by extending
 * the interfaces here.</p>
 *
 * <p>Hierarchy: {@code Target (core.target) → UIElement (elements.api) → Capability (this package)}</p>
 */
package elements.api.capability;

