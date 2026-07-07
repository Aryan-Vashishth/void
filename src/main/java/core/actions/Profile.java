package core.actions;

/**
 * Entry point for building custom action profiles.
 *
 * @deprecated Use {@link ActionProfile#builder()} directly.
 */
@Deprecated(since = "2.4", forRemoval = true)
public final class Profile {

    private Profile() {}

    /** @deprecated Use {@link ActionProfile#builder()} directly. */
    @Deprecated(since = "2.4", forRemoval = true)
    public static ActionProfile.Builder builder() {
        return ActionProfile.builder();
    }
}

