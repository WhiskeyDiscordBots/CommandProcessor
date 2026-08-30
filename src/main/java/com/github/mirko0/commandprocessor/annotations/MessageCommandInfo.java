package com.github.mirko0.commandprocessor.annotations;

import com.github.mirko0.commandprocessor.message.PrefixMode;
import net.dv8tion.jda.api.Permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a command can also be triggered by a chat message.
 *
 * <pre>{@code
 * @CommandInfo(name = "uno", description = "Start a game of UNO!")
 * @MessageCommandInfo(names = {"uno", "startuno"}, prefix = PrefixMode.OPTIONAL)
 * public class UnoCommand implements QCommandBase {
 *
 *     @CommandHandler
 *     public void slash(@CommandEvent SlashCommandInteractionEvent event) { ... }
 *
 *     @MessageHandler
 *     public void message(@MessageEvent MessageReceivedEvent event, @MessageArgs Arguments args) { ... }
 * }
 * }</pre>
 *
 * <p>Independent of {@link CommandInfo @CommandInfo}: a class may carry either, or both. A class
 * with only this one is a message command with no slash form.
 *
 * <p>Nothing happens unless the processor was given a
 * {@link com.github.mirko0.commandprocessor.message.MessageCommandConfig}. Without one no listener
 * is attached and no trigger index is built.
 *
 * <p><strong>Reading message content requires the privileged {@code MESSAGE_CONTENT} intent</strong>,
 * unless every trigger is reached by mentioning the bot. The processor checks and says so at
 * registration.
 *
 * @since 0.3.0
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MessageCommandInfo {

    /**
     * The words that trigger this command.
     *
     * <p>The first is canonical; the rest are aliases. Matched against the first whitespace-delimited
     * token of the message, never as a prefix of it — so {@code uno} does not fire on
     * {@code unofficial}.
     *
     * @return the triggers; a blank entry is ignored
     */
    String[] names();

    /**
     * Whether the configured prefix is required, optional, or must be absent.
     *
     * @return the prefix mode; {@link PrefixMode#REQUIRED} by default
     */
    PrefixMode prefix() default PrefixMode.REQUIRED;

    /**
     * Whether this command is refused outside a guild.
     *
     * @return true to ignore direct messages
     */
    boolean guildOnly() default true;

    /**
     * Permissions the invoking member must hold.
     *
     * <p>There is no Discord-side gate on a message command — anyone can type anything — so unlike
     * {@link CommandInfo#permissions()} this is enforced by the dispatcher, which refuses the
     * invocation and says so.
     *
     * @return the required permissions
     */
    Permission[] permissions() default {};

    /**
     * Whether to delete the message that triggered the command.
     *
     * <p>Requires {@code MESSAGE_MANAGE}; without it the deletion is skipped and logged rather than
     * failing the command.
     *
     * @return true to delete the trigger message
     */
    boolean deleteTrigger() default false;
}
