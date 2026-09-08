package com.github.mirko0.commandprocessor.message;

import org.jetbrains.annotations.NotNull;

/**
 * How a message command was typed, as opposed to what it was given.
 *
 * <p>A command that behaves differently prefixed and bare needs this, and a game bot usually does.
 * {@code uno} typed on its own in a channel with a game running means "I have one card left";
 * {@code !uno} typed in a channel with no game means "start one". Both reach the same command, and
 * only {@link #prefixed()} separates them.
 *
 * <p>Reachable two ways: as the third argument to
 * {@link MessageCommandFilter#allows(net.dv8tion.jda.api.events.message.MessageReceivedEvent,
 * com.github.mirko0.commandprocessor.definitions.MessageCommandDefinition, MessageInvocation)}, so a
 * filter can reject before the handler runs, and as a handler parameter annotated
 * {@code @MessageArgs} and declared as this type.
 *
 * @param trigger   the matched first token, exactly as the user typed it
 * @param rest      everything after the trigger, trimmed
 * @param prefixed  whether the message carried the configured prefix <em>or</em> a mention of the bot
 * @param mentioned whether it was specifically a mention of the bot
 * @since 0.4.0
 */
public record MessageInvocation(@NotNull String trigger, @NotNull String rest,
                                boolean prefixed, boolean mentioned) {

    /** @return true when the command was typed bare, with neither a prefix nor a mention */
    public boolean bare() {
        return !prefixed;
    }
}
