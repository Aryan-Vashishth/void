/**
 * {@code core.annotations} — Stability-tier markers for the VOID framework API surface.
 *
 * <p>VOID uses explicit annotations to communicate the stability guarantees of each
 * public type or method. This enables safe evolution of the framework while giving
 * consumers clear expectations about what may change.</p>
 *
 * <h3>Annotations</h3>
 * <ul>
 *   <li>{@link core.annotations.Beta} — marks a type or method as evolving. Beta APIs
 *       may change, be renamed, or be removed in any release without notice. Must not
 *       be used inside stable modules.</li>
 *   <li>{@link core.annotations.Internal} — marks a type or method as framework
 *       infrastructure only. Internal APIs exist for plumbing (adapters, bridges,
 *       helpers) and must not be depended upon by external consumers.</li>
 * </ul>
 *
 * <h3>Stability tier model</h3>
 * <table>
 *   <tr><th>Tier</th><th>Annotation</th><th>Guarantees</th></tr>
 *   <tr><td>Stable (frozen)</td><td>{@code @Deprecated}</td><td>No changes, no new features</td></tr>
 *   <tr><td>Stable (user-facing)</td><td><i>(none)</i></td><td>Backward-compatible evolution</td></tr>
 *   <tr><td>Beta</td><td>{@code @Beta}</td><td>May change without notice</td></tr>
 *   <tr><td>Internal</td><td>{@code @Internal}</td><td>No guarantees whatsoever</td></tr>
 * </table>
 *
 * <h3>Usage rules</h3>
 * <ol>
 *   <li>Beta APIs must not be used inside stable modules.</li>
 *   <li>Stable APIs may depend on stable APIs only.</li>
 *   <li>Internal APIs are not for external consumption.</li>
 *   <li>Once a Beta API graduates, the annotation is removed and normal
 *       backward-compatibility guarantees apply.</li>
 * </ol>
 *
 * @see core.annotations.Beta
 * @see core.annotations.Internal
 */
package core.annotations;

