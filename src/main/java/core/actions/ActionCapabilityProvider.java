package core.actions;

/**
 * Marker for capabilities that can describe themselves.
 *
 * <p>Capability interfaces that implement this interface expose their own identity as
 * an {@link ActionCapability} enum constant. No central registry, reflection, or
 * startup registration is required.</p>
 *
 * <p>{@link #capability()} is metadata-only: logging, tracing, metrics, serialization,
 * and diagnostics. Behavioral execution must continue through the capability interface
 * directly — do not dispatch element behavior through the returned enum value.</p>
 *
 * <p>{@link #safeProfile()} declares the framework default safe execution profile for
 * this capability. This is a convention — not an intrinsic property. Override it when
 * the capability's safe behavior differs from {@link ActionProfiles#DEFAULT_SAFE}.</p>
 *
 * <h3>Correct usage (metadata)</h3>
 * <pre>
 *   if (element instanceof ActionCapabilityProvider provider) {
 *       logger.debug("Capability: {}", provider.capability());
 *   }
 * </pre>
 *
 * <h3>Avoid (behavioral dispatch via enum)</h3>
 * <pre>
 *   // Do not do this — replaces polymorphism with a centralized switch
 *   switch (provider.capability()) {
 *       case CLICKABLE  -> ((Clickable) element).click();
 *       case TYPEABLE   -> ((Typeable) element).type(...);
 *   }
 * </pre>
 *
 * @see ActionCapability
 */
public interface ActionCapabilityProvider {

    /**
     * Returns the canonical capability identifier for this element capability.
     *
     * @return the {@link ActionCapability} constant that identifies this capability
     */
    ActionCapability capability();

    /**
     * Returns the framework default safe execution profile for this capability.
     *
     * <p>Default: {@link ActionProfiles#DEFAULT_SAFE} — a shared immutable profile with
     * a single {@code WAIT_FOR_ELEMENT_VISIBLE} before-hook. No capability dispatch,
     * no switch.</p>
     *
     * <p>Override in capability interfaces that require different safe-execution
     * semantics. Return a {@code public static final} interface field — do not
     * construct a new profile on each call.</p>
     */
    default ActionProfile safeProfile() {
        return ActionProfiles.DEFAULT_SAFE;
    }

}
