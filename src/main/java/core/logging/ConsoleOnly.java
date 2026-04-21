package core.logging;

import java.lang.annotation.*;

/**
 * Marks a flag or method that is <strong>only safe when output goes to a live
 * ANSI-capable terminal</strong>.
 *
 * <p>Enabling a {@code @ConsoleOnly} feature breaks the <em>one ANSI block per
 * line</em> contract that keeps IntelliJ Test History, CI logs, and file appenders
 * clean. You may see duplicate/split entries, raw escape codes, or garbled output.</p>
 *
 * <p><b>Always call the corresponding {@code disable*()} method before CI / file runs.</b></p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface ConsoleOnly {}

