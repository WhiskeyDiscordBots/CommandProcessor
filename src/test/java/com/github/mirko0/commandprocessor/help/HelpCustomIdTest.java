package com.github.mirko0.commandprocessor.help;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpCustomIdTest {

    @Test
    void encode_thenDecode_roundTrips() {
        HelpCustomId id = new HelpCustomId(HelpView.CATEGORY, "moderation", 2);

        HelpCustomId decoded = HelpCustomId.decode(id.encode());
        assertAll(
                () -> assertEquals(HelpView.CATEGORY, decoded.view()),
                () -> assertEquals("moderation", decoded.arg()),
                () -> assertEquals(2, decoded.page()));
    }

    @Test
    void decode_foreignComponentId_returnsNull() {
        assertAll(
                () -> assertNull(HelpCustomId.decode("pg:abc123:oh")),
                () -> assertNull(HelpCustomId.decode(null)),
                () -> assertNull(HelpCustomId.decode("cph:c:only:three:parts:too:many")));
    }

    /** A component left on a message by an older deployment must fail politely, not crash. */
    @Test
    void decode_unknownView_returnsNull() {
        assertNull(HelpCustomId.decode("cph:zzz:arg:0"));
    }

    @Test
    void decode_nonNumericPage_returnsNull() {
        assertNull(HelpCustomId.decode("cph:c:arg:notanumber"));
    }

    @Test
    void isHelpComponent_onlyMatchesOwnPrefix() {
        assertAll(
                () -> assertTrue(HelpCustomId.isHelpComponent("cph:r::0")),
                () -> assertFalse(HelpCustomId.isHelpComponent("cphsomethingelse")),
                () -> assertFalse(HelpCustomId.isHelpComponent("uno-play-1")));
    }

    /** An argument containing the separator would decode into the wrong fields. */
    @Test
    void construct_argumentContainingSeparator_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new HelpCustomId(HelpView.CATEGORY, "a:b", 0));
    }

    @Test
    void construct_negativePage_clampsToZero() {
        assertEquals(0, new HelpCustomId(HelpView.ROOT, "", -4).page());
    }

    /** Discord rejects an over-long id at send time; encode fails immediately instead. */
    @Test
    void encode_overLongId_throws() {
        String huge = "x".repeat(200);
        assertThrows(IllegalStateException.class,
                () -> new HelpCustomId(HelpView.COMMAND, huge, 0).encode());
    }
}
