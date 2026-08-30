package com.github.mirko0.commandprocessor.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Supplies the suggestions for an option declared with
 * {@link CommandOption#autoComplete() autoComplete = true}.
 *
 * <p>Declared on a method of the same command class, so it has the same injected dependencies the
 * handler has:
 *
 * <pre>{@code
 * @CommandHandler
 * public void play(@CommandEvent SlashCommandInteractionEvent event,
 *                  @CommandOption(type = OptionType.STRING, name = "track",
 *                          description = "What to play.", autoComplete = true)
 *                  String track) { ... }
 *
 * @AutoCompleteHandler(option = "track")
 * public List<String> trackSuggestions(@AutoCompleteValue String typed) {
 *     return library.search(typed);
 * }
 * }</pre>
 *
 * <h2>Return value</h2>
 * Any {@link java.util.Collection}. Elements may be
 * {@link net.dv8tion.jda.api.interactions.commands.Command.Choice Choice} for full control over the
 * label, or {@link String}/{@link Number} for the common case where the label is the value. Elements
 * are converted to match the option's type, so a {@code List<String>} works for an
 * {@code INTEGER} option as long as the strings parse. {@code null} means no suggestions.
 *
 * <p>Discord accepts at most 25; anything beyond that is dropped with a warning.
 *
 * <h2>Parameters</h2>
 * {@link AutoCompleteEvent @AutoCompleteEvent} for the event, {@link AutoCompleteValue @AutoCompleteValue}
 * for what the user has typed so far, and {@link CommandOption @CommandOption} to read another
 * option they have already filled in — which is how one option's suggestions depend on another.
 * All are optional; a handler may take no parameters at all.
 *
 * <h2>Timing</h2>
 * <strong>Discord allows three seconds and an autocomplete response cannot be deferred.</strong> The
 * method runs on a JDA event thread, so it must not block on I/O it cannot bound. Cache, or return
 * fewer results.
 *
 * @since 0.2.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoCompleteHandler {

    /**
     * The option this supplies suggestions for.
     *
     * @return the option name, matched case-insensitively
     */
    String option();

    /**
     * Restricts this handler to one subcommand.
     *
     * <p>Needed whenever two subcommands share an option name and want different suggestions. Empty
     * matches every subcommand of the command, which is the common case.
     *
     * @return the subcommand name, or empty for any
     */
    String subcommand() default "";

    /**
     * Restricts this handler to one subcommand group.
     *
     * @return the group name, or empty for any
     */
    String group() default "";
}
