package com.github.mirko0.commandprocessor.enumeration;

/**
 * How the library acknowledges an interaction before the handler runs.
 *
 * <p>Discord allows exactly one acknowledgement per interaction, and requires it within three
 * seconds. Declaring it here rather than doing it in the handler keeps the two from drifting apart:
 * a handler that also acknowledges gets {@code 40060 Interaction has already been acknowledged}.
 *
 * @since 0.0.1
 */
public enum Response {

    /** Defer publicly. The handler replies through {@code event.getHook()}. */
    DEFER,

    /** Defer privately. The handler replies through {@code event.getHook()}, visible only to the caller. */
    EPHEMERAL,

    /**
     * Do not acknowledge. <strong>The handler must acknowledge</strong> — with {@code event.reply(...)},
     * {@code event.deferReply(...)} or {@code event.replyModal(...)} — within three seconds, or the
     * user sees "The application did not respond".
     */
    NONE
}
