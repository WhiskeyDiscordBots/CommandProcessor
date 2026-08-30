package com.github.mirko0.commandprocessor.annotations;

import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandOption {
    OptionType type();
    String name();
    LocalizedEntry[] localizedNames() default {};
    String description() default "";
    LocalizedEntry[] localizedDescriptions() default {};
    boolean required() default false;
    boolean autoComplete() default false;
    String[] choices() default {}; // simple string choices
}
