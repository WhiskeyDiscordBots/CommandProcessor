package com.github.mirko0.commandprocessor.annotations;

import com.github.mirko0.commandprocessor.enumeration.Response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommandHandler {
    String name();
    LocalizedEntry[] localizedNames() default {};
    String description() default "";
    LocalizedEntry[] localizedDescriptions() default {};
    Response defaultResponse() default Response.NONE;
}
