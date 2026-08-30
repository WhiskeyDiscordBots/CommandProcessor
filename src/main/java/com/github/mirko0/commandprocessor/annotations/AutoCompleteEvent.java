package com.github.mirko0.commandprocessor.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the
 * {@link net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent} into an
 * {@link AutoCompleteHandler @AutoCompleteHandler} method.
 *
 * <p>The library replies with the returned choices, so the handler must not reply itself.
 *
 * @since 0.2.0
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoCompleteEvent {
}
