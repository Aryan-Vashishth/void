package core.actions;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.Element;
import elements.meta.ElementRole;

import java.util.Objects;

/**
 * Abstract base class for element-bound actions implementing the Template Method pattern.
 *
 * <p>Owns the action lifecycle: {@link #resolve(UIEngine)} → {@link #execute(UIEngine, LocatorDescriptor)}.
 * Provides fluent APIs ({@link #safely()}, {@link #debug()}, {@link #reliable()}, {@link #raw()}) and
 * immutability guarantees (all fluent methods return new instances via {@link HookChainAction}).</p>
 *
 * <h3>Template Method Pattern</h3>
 * <pre>
 *   perform(engine)
 *     → resolve(engine)                 [final — shared resolution]
 *     → execute(engine, descriptor)     [abstract — subclass responsibility]
 * </pre>
 *
 * <h3>Key Invariants</h3>
 * <ul>
 *   <li><b>Final template method:</b> {@link #perform(UIEngine)} is final. Subclasses override only {@link #execute}.</li>
 *   <li><b>Primitive operation:</b> {@link #execute(UIEngine, LocatorDescriptor)} is abstract.</li>
 *   <li><b>Final fluent APIs:</b> {@link #safely()}, {@link #debug()}, {@link #reliable()}, {@link #raw()} are final.</li>
 *   <li><b>Overridable defaults:</b> {@link #defaultSafeProfile()}, {@link #defaultDebugProfile()},
 *       {@link #defaultReliableProfile()} are protected and overridable (subclasses override only when behavior differs).</li>
 *   <li><b>Immutability:</b> Every fluent call returns a new action instance. Never mutates {@code this}.</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>
 *   // Framework creates anonymous subclass:
 *   new ElementAction(element, ElementRole.TRIGGER, ActionCapability.CLICKABLE, safeProfile) {
 *       protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
 *           engine.click(descriptor);
 *       }
 *   }
 * </pre>
 */
@Beta(since = "0.2", note = "Phase 13 refactor — ElementAction ownership model")
public abstract class ElementAction implements Action {

    protected final Element element;
    protected final ElementRole role;
    protected final ActionCapability capability;

    /**
     * Constructs an element-bound action with element, role, and capability.
     *
     * @param element the target element
     * @param role the locator role to resolve (TRIGGER, INPUT, TEXT, etc.)
     * @param capability the capability category for profile resolution
     * @throws NullPointerException if element or role is null
     */
    protected ElementAction(Element element,
                           ElementRole role,
                           ActionCapability capability) {
        this.element = Objects.requireNonNull(element, "element must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.capability = capability != null ? capability : ActionCapability.UNKNOWN;
    }

    /**
     * Final template method — orchestrates the action lifecycle.
     *
     * <p>Calls {@link #resolve} then passes the descriptor to {@link #execute}.
     * Subclasses must override only {@link #execute}.</p>
     *
     * @param engine the UI engine for execution
     */
    @Override
    public final void perform(UIEngine engine) {
        LocatorDescriptor descriptor = resolve(engine);
        execute(engine, descriptor);
    }

    /**
     * Primitive operation — subclasses implement core behavior.
     *
     * <p>Called by {@link #perform} after descriptor resolution.
     * Do NOT override {@link #perform}; override this method instead.</p>
     *
     * @param engine the UI engine for execution
     * @param descriptor the resolved locator descriptor
     */
    protected abstract void execute(UIEngine engine, LocatorDescriptor descriptor);

    /**
     * Resolves the target element's locator descriptor.
     *
     * <p>This is shared across all hooks and the action itself.
     * Called exactly once per {@link #perform} invocation.</p>
     *
     * @param engine the UI engine to resolve against
     * @return the resolved descriptor
     */
    @Override
    public final LocatorDescriptor resolve(UIEngine engine) {
        return engine.resolve(element, role);
    }

    /**
     * Returns the action's capability for profile resolution.
     *
     * @return the capability category
     */
    @Override
    public final ActionCapability capability() {
        return capability;
    }

    /**
     * Applies the framework's SAFE profile to this action.
     *
     * <p>Final — subclasses cannot override. Uses {@link ActionProfiles#DEFAULT_SAFE}.</p>
     *
     * @return a new action with SAFE profile hooks
     */
    @Override
    public final Action safely() {
        return using(defaultSafeProfile());
    }

    /**
     * Applies the framework's DEBUG profile to this action.
     *
     * <p>Final — subclasses cannot override. Uses {@link Profiles#DEBUG}.</p>
     *
     * @return a new action with DEBUG profile hooks
     */
    @Override
    public final Action debug() {
        return using(Profiles.DEBUG);
    }

    /**
     * Applies the RELIABLE profile to this action.
     *
     * <p>Final — subclasses cannot override. Uses {@link #defaultReliableProfile()}.</p>
     *
     * @return a new action with RELIABLE profile hooks
     */
    public final Action reliable() {
        return using(defaultReliableProfile());
    }

    /**
     * Applies the RAW profile (no additional hooks) to this action.
     *
     * <p>Final — subclasses cannot override. Returns {@code this} (no decoration).</p>
     *
     * @return this action unchanged
     */
    @Override
    public final Action raw() {
        return this;
    }

    /**
     * Default safe profile for this action.
     *
     * <p>Called by {@link #safely()}. Base implementation returns {@link ActionProfiles#DEFAULT_SAFE}.
     * Concrete subclasses override this to declare their own safe profile
     * (e.g., {@code ClickAction} returns {@link ActionProfiles#CLICKABLE_SAFE}).
     * Adding a new action type requires no change here — override locally.</p>
     *
     * @return the safe profile for this action
     */
    protected ActionProfile defaultSafeProfile() {
        return ActionProfiles.DEFAULT_SAFE;
    }

    /**
     * Default debug profile for this action.
     *
     * <p>Called by {@link #debug()}.
     * Override in subclasses only when behavior differs from the framework default.
     * Default implementation returns {@link Profiles#DEBUG}.</p>
     *
     * @return the debug profile
     */
    protected ActionProfile defaultDebugProfile() {
        return Profiles.DEBUG;
    }

    /**
     * Default reliable profile for this action.
     *
     * <p>Called by {@link #reliable()}. Base implementation returns {@link ActionProfiles#DEFAULT_RELIABLE}.
     * Concrete subclasses override this to declare their own reliable profile.
     * Adding a new action type requires no change here — override locally.</p>
     *
     * @return the reliable profile for this action
     */
    protected ActionProfile defaultReliableProfile() {
        return ActionProfiles.DEFAULT_RELIABLE;
    }

    /**
     * Element label for trace/logging output.
     *
     * @return element name or class simplename
     */
    @Override
    public String elementLabel() {
        if (element instanceof Enum<?> e) return e.name();
        return element.getClass().getSimpleName();
    }

    /**
     * Operation label for trace/logging output (derived from class name).
     *
     * <p>Strips the {@code "Action"} suffix and lowercases the first character:
     * {@code ClickAction} → {@code "click"}, {@code SearchAndSelectAction} → {@code "searchAndSelect"}.
     * Anonymous subclasses (no simple name) return {@code "perform"}.
     * Adding a new concrete action subclass requires no change here.</p>
     *
     * @return operation name derived from the concrete class name, or {@code "perform"}
     */
    @Override
    public String operationLabel() {
        String name = getClass().getSimpleName();
        if (name.endsWith("Action")) {
            name = name.substring(0, name.length() - 6);
        }
        return name.isEmpty() ? "perform" : Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}

