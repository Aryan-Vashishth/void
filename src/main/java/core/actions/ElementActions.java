package core.actions;

import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.Element;
import elements.api.capability.Clickable;
import elements.api.capability.Selectable;
import elements.api.capability.Typeable;
import elements.meta.ElementRole;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Internal helper for creating element-bound {@link Action}s that support
 * descriptor resolution (enabling {@link Action#withHooks}).
 *
 * <p>This class is an implementation detail — not part of the public DSL.
 * Capability interfaces use it to emit actions; users interact with {@link Action} only.</p>
 */
public final class ElementActions {

    private ElementActions() {}

    /**
     * Creates an {@link Action} bound to an element and role.
     *
     * <p>The returned action resolves its {@link LocatorDescriptor} at execution time
     * via {@link UIEngine#resolve}, and exposes it through {@link Action#resolve}
     * so that {@link Action#withHooks} can pass it to hooks.</p>
     *
     * @param element the element this action targets
     * @param role    the locator role to resolve (INPUT, TRIGGER, TEXT, etc.)
     * @param op      the action logic — receives engine and resolved descriptor
     * @return an Action that supports both perform() and resolve()
     */
    public static Action of(Element element, ElementRole role,
                            BiConsumer<UIEngine, LocatorDescriptor> op) {
        Action base = new ElementBoundAction(element, role, op, capabilityFor(element, role));
        return ActionProfiles.applyConfiguredDefault(base);
    }

    private static ActionCapability capabilityFor(Element element, ElementRole role) {
        if (element instanceof Selectable) return ActionCapability.SELECTABLE;
        if (element instanceof Typeable) return ActionCapability.TYPEABLE;
        if (element instanceof Clickable) return ActionCapability.CLICKABLE;

        if (role == ElementRole.INPUT) return ActionCapability.TYPEABLE;
        if (role == ElementRole.LIST) return ActionCapability.SELECTABLE;
        if (role == ElementRole.TRIGGER) return ActionCapability.CLICKABLE;
        return ActionCapability.UNKNOWN;
    }

    private static final class ElementBoundAction implements Action {
        private final Element element;
        private final ElementRole role;
        private final BiConsumer<UIEngine, LocatorDescriptor> op;
        private final ActionCapability capability;

        private ElementBoundAction(Element element,
                                   ElementRole role,
                                   BiConsumer<UIEngine, LocatorDescriptor> op,
                                   ActionCapability capability) {
            this.element = Objects.requireNonNull(element, "element must not be null");
            this.role = Objects.requireNonNull(role, "role must not be null");
            this.op = Objects.requireNonNull(op, "op must not be null");
            this.capability = capability == null ? ActionCapability.UNKNOWN : capability;
        }

        @Override
        public void perform(UIEngine engine) {
            op.accept(engine, resolve(engine));
        }

        @Override
        public LocatorDescriptor resolve(UIEngine engine) {
            return engine.resolve(element, role);
        }

        @Override
        public ActionCapability capability() {
            return capability;
        }
    }
}

