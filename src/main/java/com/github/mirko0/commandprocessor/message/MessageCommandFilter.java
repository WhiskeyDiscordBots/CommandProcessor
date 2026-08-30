package com.github.mirko0.commandprocessor.message;

import com.github.mirko0.commandprocessor.definitions.MessageCommandDefinition;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Decides whether a matched trigger should actually run.
 *
 * <p>This exists because of {@link PrefixMode#NONE} and {@link PrefixMode#OPTIONAL}. A bare trigger
 * named {@code uno} fires on any message whose first word is "uno", in every channel of every guild
 * the bot can see — and "uno" is a common English word. Without a filter that is a keyword hijack.
 *
 * <p>For a game bot the filter is usually the whole answer:
 *
 * <pre>{@code
 * (event, definition) -> definition.getPrefixMode() == PrefixMode.REQUIRED
 *         || games.findByChannel(event.getChannel()) != null;
 * }</pre>
 *
 * <p>{@code !uno} still works anywhere; bare {@code uno} only does something in a channel that has a
 * game running.
 *
 * <p>Called after the trigger matched and before the handler runs. Returning false is silent — no
 * reply, nothing above debug in the log — because rejecting most traffic is the normal case, not an
 * error.
 *
 * <p><strong>Must not block.</strong> It runs on a JDA event thread, for every message that matches
 * a trigger.
 *
 * @since 0.3.0
 */
@FunctionalInterface
public interface MessageCommandFilter {

    /** Allows everything. The default when none is configured. */
    MessageCommandFilter ALLOW_ALL = (event, definition) -> true;

    /**
     * @param  event      the message that matched
     * @param  definition the command it matched
     * @return true to run the handler
     */
    boolean allows(@NotNull MessageReceivedEvent event, @NotNull MessageCommandDefinition definition);
}
