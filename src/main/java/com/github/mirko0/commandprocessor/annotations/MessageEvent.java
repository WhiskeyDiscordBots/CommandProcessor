package com.github.mirko0.commandprocessor.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the {@link net.dv8tion.jda.api.events.message.MessageReceivedEvent} into a
 * {@link MessageHandler @MessageHandler} method.
 *
 * @since 0.3.0
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageEvent {
}
