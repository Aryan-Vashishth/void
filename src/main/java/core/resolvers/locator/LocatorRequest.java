package core.resolvers.locator;

import java.util.Arrays;

/**
 * Value object replacing the {@code (fileName, key, args)} primitive triple
 * that used to be passed around the resolver layer.
 *
 * <p>{@code fileName == null} is the canonical signal for a "hardcoded" template
 * (the {@code key} itself is the template).</p>
 *
 * @param fileName external locator bundle name (may be {@code null} for hardcoded templates)
 * @param key      locator key inside the bundle, or the template itself when {@code fileName == null}
 * @param args     formatting arguments (never {@code null}; defaulted to an empty array)
 */
public record LocatorRequest(String fileName, String key, Object[] args) {

    public LocatorRequest {
        if (args == null) args = new Object[0];
    }

    /** Construct a request with no formatting arguments. */
    public static LocatorRequest of(String fileName, String key) {
        return new LocatorRequest(fileName, key, new Object[0]);
    }

    /** Construct a request from raw parameters, normalising {@code null} args to an empty array. */
    public static LocatorRequest of(String fileName, String key, Object... args) {
        return new LocatorRequest(fileName, key, args == null ? new Object[0] : args);
    }

    /** {@code true} when {@link #fileName()} is {@code null} — i.e. {@link #key()} is the template. */
    public boolean isHardcoded() {
        return fileName == null;
    }

    /** Return a copy with replacement arguments. */
    public LocatorRequest withArgs(Object... newArgs) {
        return new LocatorRequest(fileName, key, newArgs == null ? new Object[0] : newArgs);
    }

    @Override
    public String toString() {
        return "LocatorRequest[file=" + fileName + ", key=" + key + ", args=" + Arrays.toString(args) + "]";
    }
}

