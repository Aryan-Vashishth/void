/**
 * Raw-string → Selenium {@code By} parsing.
 *
 * <p>{@link core.resolvers.locator.parser.ByParser} is the single canonical entry point;
 * it delegates to a registered table of {@link core.resolvers.locator.parser.ByPrefixStrategy}
 * entries, with a heuristic fallback for unprefixed strings.</p>
 */
package core.resolvers.locator.parser;

