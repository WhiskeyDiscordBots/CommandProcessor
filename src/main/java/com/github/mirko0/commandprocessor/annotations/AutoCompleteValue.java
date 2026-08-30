package com.github.mirko0.commandprocessor.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects what the user has typed into the option so far, as a {@link String}.
 *
 * <p>Empty rather than null before they have typed anything, so a handler can filter without a null
 * check. Discord sends the raw partial input even for an {@code INTEGER} or {@code NUMBER} option,
 * which is why this is always a {@code String}.
 *
 * @since 0.2.0
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoCompleteValue {
}
