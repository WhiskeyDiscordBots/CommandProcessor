package com.github.mirko0.commandprocessor.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects everything after the trigger as
 * {@link com.github.mirko0.commandprocessor.message.Arguments}.
 *
 * <p>Never null — an invocation with no arguments gets an empty one — so a handler can read it
 * without a null check.
 *
 * @since 0.3.0
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageArgs {
}
