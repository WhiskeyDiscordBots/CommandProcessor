package com.github.mirko0.commandprocessor.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Turns message commands on, and says how they are triggered.
 *
 * <p>Passing one of these to the processor is what enables the feature. Without it no listener is
 * attached, no trigger index is built, and a bot that does not want message commands pays nothing.
 *
 * <pre>{@code
 * MessageCommandConfig.builder()
 *         .prefix("!")
 *         .allowMentionPrefix(true)
 *         .filter(new OnlyWhereAGameIsRunning())
 *         .build();
 * }</pre>
 *
 * <p>Immutable and safe to share.
 *
 * @since 0.3.0
 */
public final class MessageCommandConfig {

    private final String prefix;
    private final boolean allowMentionPrefix;
    private final boolean caseSensitive;
    private final boolean ignoreBots;
    private final boolean replyOnError;
    private final MessageCommandFilter filter;

    private MessageCommandConfig(Builder builder) {
        this.prefix = builder.prefix;
        this.allowMentionPrefix = builder.allowMentionPrefix;
        this.caseSensitive = builder.caseSensitive;
        this.ignoreBots = builder.ignoreBots;
        this.replyOnError = builder.replyOnError;
        this.filter = builder.filter;
    }

    /** @return a new builder with a {@code "!"} prefix and every default */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The shortest useful configuration.
     *
     * @param  prefix the command prefix, for example {@code "!"}
     * @return a config with that prefix and every default
     */
    @NotNull
    public static MessageCommandConfig withPrefix(@NotNull String prefix) {
        return builder().prefix(prefix).build();
    }

    /** @return the prefix; may be empty when only mention-prefixed triggers are wanted */
    @NotNull
    public String prefix() {
        return prefix;
    }

    /** @return whether {@code @Bot uno} counts as a prefixed invocation */
    public boolean allowMentionPrefix() {
        return allowMentionPrefix;
    }

    /** @return whether triggers are matched case-sensitively */
    public boolean caseSensitive() {
        return caseSensitive;
    }

    /** @return whether messages from bots and webhooks are ignored */
    public boolean ignoreBots() {
        return ignoreBots;
    }

    /** @return whether a handler that throws gets a reply in the channel */
    public boolean replyOnError() {
        return replyOnError;
    }

    /** @return the filter; {@link MessageCommandFilter#ALLOW_ALL} when none was set */
    @NotNull
    public MessageCommandFilter filter() {
        return filter;
    }

    /**
     * Builds a {@link MessageCommandConfig}.
     *
     * @since 0.3.0
     */
    public static final class Builder {

        private String prefix = "!";
        private boolean allowMentionPrefix = true;
        private boolean caseSensitive = false;
        private boolean ignoreBots = true;
        private boolean replyOnError = true;
        private MessageCommandFilter filter = MessageCommandFilter.ALLOW_ALL;

        private Builder() {
        }

        /**
         * @param  prefix what a prefixed command starts with. Empty is allowed only when every
         *                command uses {@link PrefixMode#NONE} or mention-prefixing
         * @return this builder
         * @throws NullPointerException if {@code prefix} is null
         */
        @NotNull
        public Builder prefix(@NotNull String prefix) {
            this.prefix = Objects.requireNonNull(prefix, "prefix").trim();
            return this;
        }

        /**
         * Whether mentioning the bot counts as the prefix.
         *
         * <p>Worth knowing: <strong>Discord delivers message content without the privileged
         * {@code MESSAGE_CONTENT} intent when the message mentions the app</strong>. So a bot that
         * enables only this has working message commands and no privileged intent at all.
         *
         * @param  allowMentionPrefix true to accept {@code @Bot command}
         * @return this builder
         */
        @NotNull
        public Builder allowMentionPrefix(boolean allowMentionPrefix) {
            this.allowMentionPrefix = allowMentionPrefix;
            return this;
        }

        /**
         * @param  caseSensitive true to require the exact case of the declared trigger
         * @return this builder
         */
        @NotNull
        public Builder caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        /**
         * @param  ignoreBots false to let other bots and webhooks trigger commands. Leaving this on
         *                    is what stops two bots answering each other in a loop
         * @return this builder
         */
        @NotNull
        public Builder ignoreBots(boolean ignoreBots) {
            this.ignoreBots = ignoreBots;
            return this;
        }

        /**
         * @param  replyOnError false to keep a failed handler silent. On by default, because a chat
         *                      command that fails invisibly looks like a bot that ignored you
         * @return this builder
         */
        @NotNull
        public Builder replyOnError(boolean replyOnError) {
            this.replyOnError = replyOnError;
            return this;
        }

        /**
         * @param  filter decides whether a matched trigger runs; null restores allow-all
         * @return this builder
         * @see    MessageCommandFilter
         */
        @NotNull
        public Builder filter(@Nullable MessageCommandFilter filter) {
            this.filter = filter == null ? MessageCommandFilter.ALLOW_ALL : filter;
            return this;
        }

        /** @return the configuration */
        @NotNull
        public MessageCommandConfig build() {
            return new MessageCommandConfig(this);
        }
    }
}
