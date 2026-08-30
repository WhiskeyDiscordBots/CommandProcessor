package com.github.mirko0.commandprocessor.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Handles a message-command invocation.
 *
 * <p>Separate from {@link CommandHandler @CommandHandler} on purpose. A message invocation has no
 * interaction, no deferral, no ephemeral reply and no resolved options, so sharing one method with
 * the slash path means a body full of "which am I?" branching — which is exactly how the
 * predecessor bot ended up with a wrapper holding a null slash event and a null message event, and
 * every handler checking both.
 *
 * <p>Parameters are injected by annotation: {@link MessageEvent @MessageEvent},
 * {@link MessageArgs @MessageArgs} and {@link MessageArg @MessageArg}. All are optional.
 *
 * @since 0.3.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageHandler {

    /**
     * Restricts this handler to invocations whose first argument is this word.
     *
     * <p>Empty — the default — handles the bare command and receives every argument. A named handler
     * matches the first argument exactly and receives the arguments <em>after</em> it, so
     * {@code !config set volume 5} reaches {@code @MessageHandler(name = "set")} with
     * {@code [volume, 5]}.
     *
     * <p>If no named handler matches, the unnamed one runs with the arguments untouched.
     *
     * @return the subcommand word, or empty for the base handler
     */
    String name() default "";
}
