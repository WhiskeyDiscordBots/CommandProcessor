package com.github.mirko0.commandprocessor.message;

import com.github.mirko0.commandprocessor.internal.MessageParsing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a command was typed, and what each {@link PrefixMode} does with it.
 *
 * <p>The distinction matters to any command that means two things. {@code uno} typed bare in a
 * channel with a game running is "I have one card left"; {@code !uno} typed where there is no game
 * is "start one". Both reach the same command, and only the invocation form separates them.
 */
class InvocationFormTest {

    private static final String SELF = "1234567890";

    private static MessageInvocation parse(String content) {
        return MessageParsing.parse(content, "!", true, SELF);
    }

    @Nested
    @DisplayName("A mention is recorded separately from the configured prefix")
    class Mentions {

        @Test
        void mention_isPrefixedAndMentioned() {
            MessageInvocation invocation = parse("<@" + SELF + "> uno");
            assertAll(
                    () -> assertEquals("uno", invocation.trigger()),
                    () -> assertTrue(invocation.prefixed()),
                    () -> assertTrue(invocation.mentioned()));
        }

        @Test
        void configuredPrefix_isPrefixedButNotMentioned() {
            MessageInvocation invocation = parse("!uno");
            assertAll(
                    () -> assertTrue(invocation.prefixed()),
                    () -> assertFalse(invocation.mentioned()));
        }

        @Test
        void bareWord_isNeither() {
            MessageInvocation invocation = parse("uno");
            assertAll(
                    () -> assertFalse(invocation.prefixed()),
                    () -> assertFalse(invocation.mentioned()),
                    () -> assertTrue(invocation.bare()));
        }
    }

    @Nested
    @DisplayName("A mention satisfies every prefix mode")
    class MentionAcceptance {

        /**
         * The regression this exists for: a mention is the one invocation form Discord delivers
         * content for without the privileged MESSAGE_CONTENT intent. Refusing it for a bare-only
         * command made the free path unreachable for exactly the commands most likely to want it.
         */
        @Test
        void none_acceptsAMention_thoughItRefusesThePrefix() {
            assertAll(
                    () -> assertTrue(PrefixMode.NONE.accepts(true, true), "@Bot uno"),
                    () -> assertFalse(PrefixMode.NONE.accepts(true, false), "!uno"),
                    () -> assertTrue(PrefixMode.NONE.accepts(false, false), "uno"));
        }

        @Test
        void required_acceptsAMentionAndThePrefix_butNotABareWord() {
            assertAll(
                    () -> assertTrue(PrefixMode.REQUIRED.accepts(true, true)),
                    () -> assertTrue(PrefixMode.REQUIRED.accepts(true, false)),
                    () -> assertFalse(PrefixMode.REQUIRED.accepts(false, false)));
        }

        @Test
        void optional_acceptsEveryForm() {
            assertAll(
                    () -> assertTrue(PrefixMode.OPTIONAL.accepts(true, true)),
                    () -> assertTrue(PrefixMode.OPTIONAL.accepts(true, false)),
                    () -> assertTrue(PrefixMode.OPTIONAL.accepts(false, false)));
        }
    }

    @Nested
    @DisplayName("The filter's three-argument form")
    class Filtering {

        @Test
        void defaultOverload_delegatesToTheTwoArgumentForm() {
            MessageCommandFilter refusing = (event, definition) -> false;
            assertFalse(refusing.allows(null, null, parse("uno")));
        }

        @Test
        void allowAll_allowsEveryForm() {
            assertAll(
                    () -> assertTrue(MessageCommandFilter.ALLOW_ALL.allows(null, null, parse("uno"))),
                    () -> assertTrue(MessageCommandFilter.ALLOW_ALL.allows(null, null, parse("!uno"))));
        }
    }
}
