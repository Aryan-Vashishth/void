/**
 * Capability interfaces — structural + action-emission layer.
 *
 * <p>Each capability interface declares what locator keys an element type
 * exposes, its {@link elements.meta.ElementRole} mapping, and default
 * methods that emit deferred {@link core.actions.Action} objects.</p>
 *
 * <p>Contains NO execution logic. Elements emit Action (intent), engine executes.</p>
 *
 * <p>Hierarchy: {@code Element → Capability}</p>
 */
package elements.api.capability;

