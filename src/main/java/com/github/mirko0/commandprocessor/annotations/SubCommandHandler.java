package com.github.mirko0.commandprocessor.annotations;

import com.github.mirko0.commandprocessor.enumeration.Response;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommandHandler {
    String name();
    LocalizedEntry[] localizedNames() default {};
    String description() default "";
    LocalizedEntry[] localizedDescriptions() default {};
    Response defaultResponse() default Response.NONE;

    /**
     * Where this subcommand appears relative to its siblings, lowest first.
     *
     * <p>Subcommands are discovered by reflection, and {@code Class#getDeclaredMethods()} makes no
     * guarantee about the order it returns methods in - it can differ between JVMs, between builds
     * and even between runs. Without a tie-breaker the order Discord shows subcommands in is
     * therefore not stable.
     *
     * <p>Subcommands are sorted by this value and then by name, so leaving it alone gives a stable
     * alphabetical listing and setting it gives an explicit one.
     *
     * @return the sort key; ties are broken by subcommand name
     */
    int order() default 0;
}
