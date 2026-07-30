package core.engine;

/**
 * Package-private value type backing {@link LocatorStrategy#of(String)}.
 * Equality is name-based: two strategies with the same name are the same strategy.
 */
record NamedStrategy(String name) implements LocatorStrategy {

    NamedStrategy {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("LocatorStrategy name must not be blank");
    }

    @Override
    public String toString() { return name; }
}
