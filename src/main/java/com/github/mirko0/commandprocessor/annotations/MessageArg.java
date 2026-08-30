package com.github.mirko0.commandprocessor.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects one positional argument, converted to the parameter's type.
 *
 * <pre>{@code
 * @MessageHandler
 * public void callout(@MessageEvent MessageReceivedEvent event,
 *                     @MessageArg(0) User target) { ... }
 * }</pre>
 *
 * <p>Supported parameter types: {@link String}, {@link Long}, {@link Integer}, {@link Double},
 * {@link Boolean} and {@link net.dv8tion.jda.api.entities.User}. A user is resolved from a mention
 * or a raw id, and only from users the message already carries — the dispatcher never blocks on a
 * fetch.
 *
 * <p><strong>Always null when the argument is absent or does not convert</strong>, so the parameter
 * must be a reference type and the handler must check. A missing argument is the normal case: people
 * type commands wrong.
 *
 * @since 0.3.0
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageArg {

    /**
     * @return the zero-based position of the argument
     */
    int value();
}
