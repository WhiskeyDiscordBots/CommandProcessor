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
public @interface CommandHandler {

    Response defaultResponse() default Response.NONE;

}
