package com.github.mirko0.commandprocessor.message;

import com.github.mirko0.commandprocessor.definitions.MessageCommandDefinition;
import com.github.mirko0.commandprocessor.internal.MessageParsing;
import com.github.mirko0.commandprocessor.internal.ParameterBinder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Map;

/**
 * Routes a chat message to a {@link com.github.mirko0.commandprocessor.annotations.MessageHandler}.
 *
 * <p>Attached only when the processor was given a {@link MessageCommandConfig}, so a bot that does
 * not want message commands never reaches this class.
 *
 * <p>Matching is one hash lookup on the first word of the message. The trigger is compared to that
 * whole token, never as a prefix of it — the predecessor bot used {@code startsWith("uno")}, which
 * fires on "unofficial results are in".
 *
 * @since 0.3.0
 */
public final class MessageCommandHandler extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(MessageCommandHandler.class);

    /** The message may be gone by the time we answer; that is not worth a stack trace. */
    private static final ErrorHandler IGNORE_GONE = new ErrorHandler()
            .ignore(ErrorResponse.UNKNOWN_MESSAGE,
                    ErrorResponse.MISSING_PERMISSIONS,
                    ErrorResponse.MISSING_ACCESS,
                    ErrorResponse.CANNOT_SEND_TO_USER);

    private final Map<String, MessageCommandDefinition> triggers;
    private final MessageCommandConfig config;

    /**
     * @param triggers every trigger, already normalised to the config's case rules
     * @param config   how commands are recognised
     */
    public MessageCommandHandler(@NotNull Map<String, MessageCommandDefinition> triggers,
                                 @NotNull MessageCommandConfig config) {
        this.triggers = triggers;
        this.config = config;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (triggers.isEmpty()) return;
        if (config.ignoreBots() && (event.getAuthor().isBot() || event.isWebhookMessage())) return;
        if (event.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong()) return;

        String content = event.getMessage().getContentRaw().trim();
        if (content.isEmpty()) return;

        MessageInvocation invocation = MessageParsing.parse(content, config.prefix(),
                config.allowMentionPrefix(), event.getJDA().getSelfUser().getId());
        if (invocation == null) return;

        MessageCommandDefinition definition = triggers.get(key(invocation.trigger()));
        if (definition == null) return;
        if (!definition.getPrefixMode().accepts(invocation.prefixed(), invocation.mentioned())) return;
        if (definition.isGuildOnly() && !event.isFromGuild()) return;

        if (!config.filter().allows(event, definition, invocation)) return;
        if (!hasPermission(event, definition)) return;

        run(event, definition, invocation, Arguments.of(invocation.rest(), event.getMessage()));
    }

    private String key(String trigger) {
        return MessageParsing.key(trigger, config.caseSensitive());
    }

    // -------------------------------------------------------------- execution

    /**
     * Whether the author may run this command.
     *
     * <p>Enforced here, unlike a slash command's permissions: Discord has no gate on someone typing
     * a word in a channel, so nothing else would check.
     */
    private boolean hasPermission(MessageReceivedEvent event, MessageCommandDefinition definition) {
        EnumSet<Permission> required = definition.getPermissions();
        if (required.isEmpty()) return true;

        Member member = event.getMember();
        if (member == null) return false;

        boolean allowed = event.getChannel() instanceof GuildChannel channel
                ? member.hasPermission(channel, required)
                : member.hasPermission(required);

        if (!allowed) {
            event.getMessage().reply("You need " + describe(required) + " to use that.")
                    .queue(null, IGNORE_GONE);
        }
        return allowed;
    }

    private static String describe(EnumSet<Permission> permissions) {
        StringBuilder text = new StringBuilder();
        permissions.forEach(permission -> text.append(text.isEmpty() ? "" : ", ").append(permission.getName()));
        return text.toString();
    }

    private void run(MessageReceivedEvent event, MessageCommandDefinition definition,
                     MessageInvocation invocation, Arguments arguments) {
        Method handler = definition.getBaseHandler();
        Arguments forHandler = arguments;

        // A named handler claims the first argument, so "!config set volume 5" reaches the "set"
        // handler with [volume, 5] rather than making it re-parse its own name.
        String first = arguments.get(0).map(this::key).orElse(null);
        Method named = first == null ? null : definition.getNamedHandlers().get(first);
        if (named != null) {
            handler = named;
            forHandler = arguments.skipFirst();
        }

        if (handler == null) {
            // Named handlers only, and none matched. Silent: the user typed something this command
            // does not do, which is not an error the bot should shout about.
            return;
        }

        try {
            handler.invoke(definition.getInstance(), ParameterBinder.bind(handler,
                    ParameterBinder.Context.ofMessage(event, forHandler, invocation)));
        } catch (Exception failure) {
            Throwable cause = failure instanceof InvocationTargetException wrapped && wrapped.getCause() != null
                    ? wrapped.getCause()
                    : failure;
            log.error("Message command '{}' failed for user {} in guild {}",
                    definition.getName(), event.getAuthor().getId(),
                    event.isFromGuild() ? event.getGuild().getId() : "DM", cause);

            if (config.replyOnError()) {
                event.getMessage().reply("Something went wrong running that.").queue(null, IGNORE_GONE);
            }
            return;
        }

        // Outside the try on purpose: the command has already run successfully by this point, and a
        // failure to tidy up after it is not a failure of the command. Reporting one as the other
        // told the user their command had not worked when it had.
        deleteTrigger(event, definition);
    }

    /**
     * Deletes the invoking message when the command asked for it.
     *
     * <p>Needs {@code MESSAGE_MANAGE}. Without it the deletion is skipped rather than failing the
     * command, which has already run successfully by this point.
     */
    private void deleteTrigger(MessageReceivedEvent event, MessageCommandDefinition definition) {
        if (!definition.isDeleteTrigger()) return;

        Message message = event.getMessage();
        if (event.isFromGuild() && event.getChannel() instanceof GuildChannel channel
                && !event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_MANAGE)) {
            log.warn("'{}' asked to delete its trigger message but the bot lacks Manage Messages in "
                    + "#{}. Skipped.", definition.getName(), channel.getName());
            return;
        }
        message.delete().queue(null, IGNORE_GONE);
    }
}
