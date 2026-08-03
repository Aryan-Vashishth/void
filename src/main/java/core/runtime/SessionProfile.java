package core.runtime;

import java.util.Objects;

/**
 * Kernel-neutral session configuration profile.
 *
 * <p>A {@code SessionProfile} selects the named configuration layer that a domain's
 * executor uses during initialization. Each engine adapter maps the profile name to
 * its own configuration source (e.g. {@code DEFAULT} -> {@code driver.properties} for
 * the Selenium adapter, {@code CI} -> {@code driver-ci.properties}).</p>
 *
 * <p>The four standard constants cover the profiles shipped with the Web domain.
 * Additional profiles can be created with {@link #of(String)} for project-specific
 * or engine-specific configuration sets.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 *   VOID session = VOID.builder()
 *           .profile(SessionProfile.DEFAULT)
 *           .start();
 *
 *   // Custom project profile
 *   VOID admin = VOID.builder()
 *           .profile(SessionProfile.of("admin-chrome"))
 *           .start();
 * </pre>
 *
 * <p>Replaces the Selenium-specific {@code SeleniumDriverFactory.Profile} on the public API
 * (runtime-redesign I6.4, F4 gate resolution). {@code SeleniumDriverFactory.Profile} remains
 * as an internal Selenium adapter concern; the deprecated bridge methods on
 * {@code VOIDBuilder} and {@code VOID} are removed in 1.0.</p>
 */
public final class SessionProfile {

    public static final SessionProfile DEFAULT = of("DEFAULT");
    public static final SessionProfile LOCAL   = of("LOCAL");
    public static final SessionProfile CI      = of("CI");
    public static final SessionProfile GRID    = of("GRID");

    private final String name;

    private SessionProfile(String name) {
        this.name = Objects.requireNonNull(name, "profile name must not be null")
                           .toUpperCase().trim();
    }

    /**
     * Creates a profile with the given name.
     *
     * <p>The name is normalized to upper-case so that {@code of("ci")} and
     * {@code of("CI")} produce the same profile.</p>
     */
    public static SessionProfile of(String name) {
        return new SessionProfile(name);
    }

    /** Returns the normalized profile name. Used by engine adapters for dispatch. */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof SessionProfile other && name.equals(other.name));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "SessionProfile(" + name + ")";
    }
}
