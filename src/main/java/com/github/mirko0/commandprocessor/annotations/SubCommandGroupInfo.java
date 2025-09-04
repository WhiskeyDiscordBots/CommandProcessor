package com.github.mirko0.commandprocessor.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
@Documented
public @interface SubCommandGroupInfo {
    String name();
    LocalizedEntry[] localizedNames() default {};
    String description() default "";
    LocalizedEntry[] localizedDescriptions() default {};
}
