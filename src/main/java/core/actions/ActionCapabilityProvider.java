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

}
