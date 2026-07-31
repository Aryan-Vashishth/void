package domain.automation.web.vocabulary.actions;

import core.actions.Action;
import core.actions.ActionCapability;
import core.actions.ActionProfiles;
import core.annotations.Internal;
import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.vocabulary.element.UIElement;
import domain.automation.web.vocabulary.role.ElementRole;

import java.util.function.BiConsumer;

/**
 * Internal factory for creating element-bound {@link Action}s with a custom operation lambda.
 *
 * <p>This class is an implementation detail — not part of the public DSL.
 * Production capability interfaces emit concrete action subclasses directly
 * (e.g., {@code new ClickAction(this)}). This factory is retained for test
 * infrastructure and edge cases that require a custom operation without a
 * dedicated subclass. See ADR-012.</p>
 */
@Internal
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
    public static Action of(UIElement element, ElementRole role,
                            BiConsumer<UIEngine, LocatorDescriptor> op) {
        ActionCapability capability = capabilityFor(element);

        // Create anonymous subclass of ElementAction
        Action base = new ElementAction(element, role, capability) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
                op.accept(engine, descriptor);
            }
        };

        return ActionProfiles.applyConfiguredDefault(base);
    }

    private static ActionCapability capabilityFor(UIElement element) {
        return element.capability();
    }
}

