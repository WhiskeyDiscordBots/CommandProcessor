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
     */
    public boolean accepts(boolean prefixed) {
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
