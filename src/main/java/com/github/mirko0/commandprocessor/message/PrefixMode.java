package com.github.mirko0.commandprocessor.message;

/**
 * Whether a message command's configured prefix is required.
 *
 * @since 0.3.0
 */
public enum PrefixMode {

    /**
     * Only with the prefix: {@code !uno}. The default, and the only mode that cannot fire on
     * ordinary conversation.
     */
    REQUIRED,

    /** With or without: both {@code !uno} and {@code uno}. */
    OPTIONAL,

    /** Only without: {@code uno}. The prefixed form is not registered. */
    NONE;

    /**
     * @param  prefixed whether the message carried the prefix
     * @return whether this mode accepts that
     * @deprecated since 0.4.0, use {@link #accepts(boolean, boolean)}. This overload cannot tell a
     *             configured prefix from a mention of the bot, and so refuses {@code @Bot uno} for a
     *             {@link #NONE} command — the one form of message command that needs no privileged
     *             intent. Removal is not planned.
     */
    @Deprecated(since = "0.4.0")
    public boolean accepts(boolean prefixed) {
        return accepts(prefixed, false);
    }

    /**
     * Whether a message typed this way may invoke a command declared with this mode.
     *
     * <p><strong>A mention always counts.</strong> {@code @Bot uno} reaches a {@link #NONE} command,
     * even though {@code !uno} does not: the mode is about the configured prefix, and a mention is
     * not that prefix. It is also the only invocation form Discord delivers content for without the
     * privileged {@code MESSAGE_CONTENT} intent, so a mode that refused it would make the free path
     * unreachable for exactly the commands most likely to want it.
     *
     * @param  prefixed  whether the message carried the configured prefix or a mention
     * @param  mentioned whether it was specifically a mention of the bot
     * @return whether this mode accepts that
     * @since 0.4.0
     */
    public boolean accepts(boolean prefixed, boolean mentioned) {
        if (mentioned) return true;
        return switch (this) {
            case REQUIRED -> prefixed;
            case NONE -> !prefixed;
            case OPTIONAL -> true;
        };
    }

    /**
     * Whether this mode can fire on a message that carries no prefix.
     *
     * <p>These are the triggers that need a
     * {@link MessageCommandFilter}: a bare word fires on every message in every channel the bot can
     * see, and {@code uno} is a common English word.
     *
     * @return true for {@link #OPTIONAL} and {@link #NONE}
     */
    public boolean allowsBareWord() {
        return this != REQUIRED;
    }
}
