package com.github.mirko0.commandprocessor.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The matching rules, tested directly — this is the part that must not regress.
 */
class MessageParsingTest {

    private static final String SELF = "1234567890";

    private static MessageParsing.Invocation parse(String content) {
        return MessageParsing.parse(content, "!", true, SELF);
    }

    @Test
    void parse_prefixedCommand_splitsTriggerAndRest() {
        MessageParsing.Invocation invocation = parse("!uno start now");
        assertAll(
                () -> assertEquals("uno", invocation.trigger()),
                () -> assertEquals("start now", invocation.rest()),
                () -> assertTrue(invocation.prefixed()));
    }

    @Test
    void parse_bareWord_isNotMarkedPrefixed() {
        MessageParsing.Invocation invocation = parse("uno");
        assertAll(
                () -> assertEquals("uno", invocation.trigger()),
                () -> assertEquals("", invocation.rest()),
                () -> assertFalse(invocation.prefixed()));
    }

    /**
     * The predecessor bot matched with {@code content.startsWith("uno")}, so it fired on ordinary
     * conversation. The trigger is the whole first token or nothing.
     */
    @Test
    void parse_wordMerelyStartingWithATrigger_yieldsThatWholeWord() {
        assertEquals("unofficial", parse("unofficial results are in").trigger());
    }

    @Test
    void parse_extraWhitespace_isIgnored() {
        MessageParsing.Invocation invocation = parse("   !uno    start   now   ");
        assertAll(
                () -> assertEquals("uno", invocation.trigger()),
                () -> assertEquals("start   now", invocation.rest()));
    }

    @Test
    void parse_emptyOrPrefixOnly_isNotACommand() {
        assertAll(
                () -> assertNull(parse("")),
                () -> assertNull(parse("   ")),
                () -> assertNull(parse("!")),
                () -> assertNull(MessageParsing.parse(null, "!", true, SELF)));
    }

    @Nested
    @DisplayName("mention prefix")
    class Mention {

        @Test
        void parse_leadingMentionOfTheBot_countsAsPrefixed() {
            MessageParsing.Invocation invocation = parse("<@" + SELF + "> uno start");
            assertAll(
                    () -> assertEquals("uno", invocation.trigger()),
                    () -> assertEquals("start", invocation.rest()),
                    () -> assertTrue(invocation.prefixed()));
        }

        /** Older clients send the nickname form. */
        @Test
        void parse_nicknameMentionForm_isAccepted() {
            assertTrue(parse("<@!" + SELF + "> uno").prefixed());
        }

        @Test
        void parse_mentionOfSomeoneElse_isNotAPrefix() {
            MessageParsing.Invocation invocation = parse("<@999> uno");
            assertAll(
                    () -> assertEquals("<@999>", invocation.trigger()),
                    () -> assertFalse(invocation.prefixed()));
        }

        @Test
        void parse_mentionAlone_isNotACommand() {
            assertNull(parse("<@" + SELF + ">"));
        }

        @Test
        void parse_mentionDisabled_isTreatedAsOrdinaryText() {
            MessageParsing.Invocation invocation =
                    MessageParsing.parse("<@" + SELF + "> uno", "!", false, SELF);
            assertFalse(invocation.prefixed());
        }
    }

    @Nested
    @DisplayName("prefix configuration")
    class Prefixes {

        @Test
        void parse_multiCharacterPrefix_isStripped() {
            MessageParsing.Invocation invocation = MessageParsing.parse(">>uno go", ">>", false, SELF);
            assertAll(
                    () -> assertEquals("uno", invocation.trigger()),
                    () -> assertTrue(invocation.prefixed()));
        }

        @Test
        void parse_prefixWithSpaceAfterIt_stillFindsTheTrigger() {
            assertEquals("uno", MessageParsing.parse("! uno go", "!", false, SELF).trigger());
        }

        @Test
        void parse_emptyPrefix_neverMarksAMessagePrefixed() {
            assertFalse(MessageParsing.parse("uno", "", false, SELF).prefixed());
        }

        @Test
        void parse_noSelfId_stillHandlesThePrefixPath() {
            assertEquals("uno", MessageParsing.parse("!uno", "!", true, null).trigger());
        }
    }
}
