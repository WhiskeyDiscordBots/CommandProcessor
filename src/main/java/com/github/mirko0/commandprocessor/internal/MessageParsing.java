package com.github.mirko0.commandprocessor.internal;

import com.github.mirko0.commandprocessor.message.MessageInvocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Splits a raw message into a trigger and the rest.
 *
 * <p>Pure: no JDA types, so the matching rules — which are the part that must not regress — can be
 * tested directly rather than through a mocked event.
 *
 * <p><strong>Internal.</strong> Not part of the public API and may change in any release. The value
 * it returns, {@link MessageInvocation}, is public.
 *
 * @since 0.3.0
 */
public final class MessageParsing {

    private MessageParsing() {
    }

    /**
     * The lookup key for a trigger, used to build the index and to look a typed word up in it.
     *
     * <p>Both sides go through here so they cannot drift, and it is
     * {@link Locale#ROOT} rather than the default locale. On a JVM running with a Turkish
     * or Azeri locale {@code "I"} lowercases to {@code "ı"} — dotless — so a player typing
     * {@code IHAVEUNO} would match nothing, while the index, built from an annotation value that is
     * already lowercase, was unaffected. Silent, host-dependent, and invisible in a bug report.
     *
     * @param  trigger       a trigger, or the first word of a message
     * @param  caseSensitive whether the configuration distinguishes case
     * @return the key to store or look up under
     */
    @NotNull
    public static String key(@NotNull String trigger, boolean caseSensitive) {
        return caseSensitive ? trigger : trigger.toLowerCase(Locale.ROOT);
    }

    /**
     * @param  content      the raw message, already trimmed
     * @param  prefix       the configured prefix; empty disables prefix matching
     * @param  allowMention whether a leading mention of the bot counts as a prefix
     * @param  selfId       the bot's user id, for mention matching
     * @return the split, or null when the message cannot be a command at all
     */
    @Nullable
    public static MessageInvocation parse(@Nullable String content, @NotNull String prefix,
                                          boolean allowMention, @Nullable String selfId) {
        if (content == null) return null;

        String body = content.trim();
        if (body.isEmpty()) return null;

        boolean prefixed = false;
        boolean mentioned = false;

        String withoutMention = allowMention ? stripMention(body, selfId) : null;
        if (withoutMention != null) {
            body = withoutMention;
            prefixed = true;
            mentioned = true;
        } else if (!prefix.isEmpty() && body.startsWith(prefix)) {
            body = body.substring(prefix.length()).trim();
            prefixed = true;
        }
        if (body.isEmpty()) return null;

        // The trigger is the whole first token, never a prefix of the message. The predecessor bot
        // used startsWith("uno"), which fires on "unofficial results are in".
        int space = indexOfWhitespace(body);
        return space < 0
                ? new MessageInvocation(body, "", prefixed, mentioned)
                : new MessageInvocation(body.substring(0, space), body.substring(space + 1).trim(),
                        prefixed, mentioned);
    }

    /**
     * Removes a leading mention of the bot.
     *
     * <p>Worth knowing: Discord delivers message content <em>without</em> the privileged
     * {@code MESSAGE_CONTENT} intent when the message mentions the app. A bot that enables only
     * mention-prefixing therefore has working message commands and no privileged intent at all.
     *
     * @return the remainder, or null when the message does not open with a mention of this bot
     */
    @Nullable
    private static String stripMention(String content, String selfId) {
        if (selfId == null || !content.startsWith("<@")) return null;

        int close = content.indexOf('>');
        if (close < 0) return null;

        String id = content.substring(2, close).replace("!", "");
        return id.equals(selfId) ? content.substring(close + 1).trim() : null;
    }

    private static int indexOfWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) return i;
        }
        return -1;
    }
}
