package core.actions;

import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.Element;
import elements.meta.ElementRole;

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
        return new Action() {
            @Override
            public void perform(UIEngine engine) {
                op.accept(engine, resolve(engine));
            }

            @Override
            public LocatorDescriptor resolve(UIEngine engine) {
                return engine.resolve(element, role);
            }
        };
    }
}

