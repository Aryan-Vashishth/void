package core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type or method as <b>beta</b> — the API may change without notice
 * between minor versions.
 *
 * <h3>Usage rules</h3>
 * <ul>
 *   <li>Beta APIs must <b>not</b> be used inside stable modules.</li>
 *   <li>Stable APIs may depend on stable APIs only.</li>
 *   <li>Beta APIs may change, be renamed, or be removed in any release.</li>
 * </ul>
 *
 * <p>Once an API graduates from beta, this annotation is removed and the API
 * becomes subject to normal backward-compatibility guarantees.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Beta {

    /** Version in which the API was introduced as beta. */
    String since() default "2.0";

    /** Optional note describing the beta contract or expected graduation timeline. */
    String note() default "API may change without notice";
}

