package core.actions;

import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.Element;
import elements.meta.ElementRole;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Internal helper for creating element-bound {@link Action}s that support
 * descriptor resolution (enabling {@link Action#withHooks}).
 *
 * <p>This class is an implementation detail — not part of the public DSL.
 * Capability interfaces use it to emit actions; users interact with {@link Action} only.</p>
 *
 * <p>As of Phase 13, creates anonymous subclasses of {@link ElementAction} that implement
 * the Template Method pattern and own the action lifecycle.</p>
 */
public final class ElementActions {

    private ElementActions() {}

    /**
     * Creates an {@link ElementAction} bound to an element and role.
     *
     * <p>The returned action resolves its {@link LocatorDescriptor} at execution time
     * via {@link UIEngine#resolve}, and exposes it through {@link Action#resolve}
     * so that {@link Action#withHooks} can pass it to hooks.</p>
     *
     * <p>Implements the Template Method pattern: {@code perform()} calls
     * {@link ElementAction#resolve} then delegates to the user's lambda operation.</p>
     *
     * @param element the element this action targets
     * @param role    the locator role to resolve (INPUT, TRIGGER, TEXT, etc.)
     * @param op      the action logic — receives engine and resolved descriptor
     * @return an ElementAction that supports both perform() and resolve()
     */
    public static Action of(Element element, ElementRole role,
                            BiConsumer<UIEngine, LocatorDescriptor> op) {
        ActionCapability capability = capabilityFor(element, role);

        // Create anonymous subclass of ElementAction
        Action base = new ElementAction(element, role, capability) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
                op.accept(engine, descriptor);
            }
        };

        return ActionProfiles.applyConfiguredDefault(base);
    }

    private static ActionCapability capabilityFor(Element element, ElementRole role) {
        if (element instanceof ActionCapabilityProvider p) return p.capability();
        if (role == ElementRole.INPUT)   return ActionCapability.TYPEABLE;
        if (role == ElementRole.LIST)    return ActionCapability.SELECTABLE;
        if (role == ElementRole.TRIGGER) return ActionCapability.CLICKABLE;
        return ActionCapability.UNKNOWN;
    }
}

