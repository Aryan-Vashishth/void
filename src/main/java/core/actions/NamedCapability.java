package core.actions;

/**
 * Package-private value type backing {@link ActionCapability#of(String)}.
 * Equality is name-based: two capabilities with the same name are the same capability.
 */
record NamedCapability(String name) implements ActionCapability {}
