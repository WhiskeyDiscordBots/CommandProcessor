package com.github.mirko0.commandprocessor.internal;

import com.github.mirko0.commandprocessor.QCommandBase;
import com.github.mirko0.commandprocessor.annotations.MessageCommandInfo;
import com.github.mirko0.commandprocessor.annotations.MessageHandler;
import com.github.mirko0.commandprocessor.definitions.MessageCommandDefinition;
import com.github.mirko0.commandprocessor.message.MessageCommandConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Trigger matching must not depend on the host's locale.
 *
 * <p>Every assertion here passes on an ordinary JVM and failed before the fix on a Turkish one:
 * {@code "I".toLowerCase()} is {@code "ı"} there, dotless, so a player typing {@code IHAVEUNO}
 * matched nothing while the index — built from an annotation value that was already lowercase — was
 * untouched. The mismatch was silent and host-dependent, which is the worst combination to debug.
 */
class TriggerNormalisationTest {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    private final Locale original = Locale.getDefault();

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(original);
    }

    @Test
    @DisplayName("A typed trigger and an indexed trigger agree under a Turkish default locale")
    void key_underTurkishLocale_stillFoldsAsciiI() {
        Locale.setDefault(TURKISH);

        assertAll(
                () -> assertEquals("ihaveuno", MessageParsing.key("IHAVEUNO", false)),
                () -> assertEquals("ihaveuno", MessageParsing.key("IHaveUno", false)),
                () -> assertEquals("uno", MessageParsing.key("UNO", false)),
                () -> assertEquals(MessageParsing.key("IHAVEUNO", false),
                        MessageParsing.key("ihaveuno", false)));
    }

    @Test
    @DisplayName("Case-sensitive configuration is left alone in every locale")
    void key_caseSensitive_isUntouched() {
        Locale.setDefault(TURKISH);
        assertEquals("IHaveUno", MessageParsing.key("IHaveUno", true));
    }

    @Test
    @DisplayName("The index keys an uppercase annotation trigger where the dispatcher will look")
    void index_underTurkishLocale_keysWhereTheDispatcherLooks() {
        Locale.setDefault(TURKISH);

        Map<String, MessageCommandDefinition> triggers = TriggerIndex
                .of(List.of(new ShoutingCommand()), MessageCommandConfig.withPrefix("!"))
                .triggers();

        assertAll(
                () -> assertTrue(triggers.containsKey("ihaveuno"),
                        () -> "indexed as " + triggers.keySet()),
                () -> assertNotNull(triggers.get(MessageParsing.key("IHAVEUNO", false))));
    }

    @MessageCommandInfo(names = {"IHaveUno"})
    static class ShoutingCommand implements QCommandBase {

        @MessageHandler
        public void onMessage() {
        }
    }
}
