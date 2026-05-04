package core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type or method as <b>internal</b> — exists for framework infrastructure only.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Internal APIs exist to support framework mechanics (adapters, bridges, helpers).</li>
 *   <li>They may be changed, moved, or removed in any release without notice.</li>
 *   <li>External consumers must <b>not</b> depend on internal APIs.</li>
 * </ul>
 *
 * <p>Use this for: migration bridges, helper classes, adapter layers,
 * and anything that exists only to glue framework internals together.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Internal {}

