package com.github.mirko0.commandprocessor.message;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgumentsTest {

    @Test
    void of_splitsOnRunsOfWhitespace() {
        Arguments args = Arguments.of("  set   volume\t5 ", null);
        assertEquals(List.of("set", "volume", "5"), args.all());
    }

    @Test
    void of_nullOrBlank_isEmpty() {
        assertAll(
                () -> assertTrue(Arguments.of(null, null).isEmpty()),
                () -> assertTrue(Arguments.of("   ", null).isEmpty()),
                () -> assertEquals(0, Arguments.empty().size()));
    }

    /** People type commands wrong constantly; an index nobody typed must not throw. */
    @Test
    void get_indexOutOfRange_isEmptyNotAnException() {
        Arguments args = Arguments.of("one", null);
        assertAll(
                () -> assertEquals(Optional.empty(), args.get(5)),
                () -> assertEquals(Optional.empty(), args.get(-1)),
                () -> assertEquals("fallback", args.getOrDefault(5, "fallback")));
    }

    @Test
    void remainder_isEverythingAfterTheTrigger() {
        assertEquals("say  something here",
                Arguments.of("  say  something here  ", null).remainder());
    }

    @Test
    void remainderFrom_joinsTheTail() {
        Arguments args = Arguments.of("set message hello there world", null);
        assertAll(
                () -> assertEquals("hello there world", args.remainderFrom(2)),
                () -> assertEquals("", args.remainderFrom(99)),
                () -> assertEquals("", args.remainderFrom(-1)));
    }

    @Test
    void asLong_parsesOrReturnsEmpty() {
        Arguments args = Arguments.of("42 nope", null);
        assertAll(
                () -> assertEquals(Optional.of(42L), args.asLong(0)),
                () -> assertEquals(Optional.empty(), args.asLong(1)),
                () -> assertEquals(Optional.empty(), args.asLong(9)));
    }

    @Test
    void asDouble_parsesOrReturnsEmpty() {
        Arguments args = Arguments.of("1.5 nope", null);
        assertAll(
                () -> assertEquals(Optional.of(1.5d), args.asDouble(0)),
                () -> assertEquals(Optional.empty(), args.asDouble(1)));
    }

    @Test
    void user_withoutAMessage_isEmpty() {
        assertEquals(Optional.empty(), Arguments.of("<@123>", null).user(0));
    }

    /** A named handler claims the first argument, so it sees only what follows its own name. */
    @Test
    void skipFirst_dropsTheLeadingArgumentAndTheRemainder() {
        Arguments args = Arguments.of("set volume 5", null).skipFirst();
        assertAll(
                () -> assertEquals(List.of("volume", "5"), args.all()),
                () -> assertEquals("volume 5", args.remainder()));
    }

    @Test
    void skipFirst_onEmpty_staysEmpty() {
        assertTrue(Arguments.empty().skipFirst().isEmpty());
    }

    @Test
    void all_isUnmodifiable() {
        assertFalse(Arguments.of("a b", null).all().isEmpty());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> Arguments.of("a b", null).all().add("c"));
    }
}
