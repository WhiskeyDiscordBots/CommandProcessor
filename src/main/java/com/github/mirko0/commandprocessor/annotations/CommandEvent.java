package com.github.mirko0.commandprocessor.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to inject the SlashCommandInteractionEvent into command handler methods.
 * This annotation should be applied to SlashCommandInteractionEvent parameters in command
 * handler or subcommand handler methods.
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;CommandHandler
 * public void execute(@CommandEvent SlashCommandInteractionEvent event) {
 *     event.reply("Hello!").queue();
 * }
 * </pre>
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandEvent {
}