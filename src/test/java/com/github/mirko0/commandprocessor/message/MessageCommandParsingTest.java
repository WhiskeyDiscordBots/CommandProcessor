package com.github.mirko0.commandprocessor.message;

import com.github.mirko0.commandprocessor.CommandProcessor;
import com.github.mirko0.commandprocessor.QCommandBase;
import com.github.mirko0.commandprocessor.annotations.CommandEvent;
import com.github.mirko0.commandprocessor.annotations.CommandHandler;
import com.github.mirko0.commandprocessor.annotations.CommandInfo;
import com.github.mirko0.commandprocessor.annotations.MessageArg;
import com.github.mirko0.commandprocessor.annotations.MessageArgs;
import com.github.mirko0.commandprocessor.annotations.MessageCommandInfo;
import com.github.mirko0.commandprocessor.annotations.MessageEvent;
import com.github.mirko0.commandprocessor.annotations.MessageHandler;
import com.github.mirko0.commandprocessor.definitions.MessageCommandDefinition;
import com.github.mirko0.commandprocessor.internal.MessageCommandParser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageCommandParsingTest {

    @CommandInfo(name = "uno", description = "Start a game of UNO!")
    @MessageCommandInfo(names = {"uno", "startuno", "UNO", "  "}, prefix = PrefixMode.OPTIONAL)
    static class Uno implements QCommandBase {
        @CommandHandler
        void slash(@CommandEvent SlashCommandInteractionEvent e) { }

        @MessageHandler
        void message(@MessageEvent MessageReceivedEvent e, @MessageArgs Arguments args) { }
    }

    @Test
    void parse_triggers_areLowercasedAndDeduplicated() {
        MessageCommandDefinition definition = MessageCommandParser.parse(new Uno());
        assertAll(
                () -> assertEquals(List.of("uno", "startuno"), definition.getTriggers()),
                () -> assertEquals("uno", definition.getName()),
                () -> assertEquals(PrefixMode.OPTIONAL, definition.getPrefixMode()));
    }

    @Test
    void parse_classWithoutMessageCommandInfo_returnsNull() {
        @CommandInfo(name = "slashonly", description = "Slash only.")
        class SlashOnly implements QCommandBase {
            @CommandHandler void run(@CommandEvent SlashCommandInteractionEvent e) { }
        }
        assertNull(MessageCommandParser.parse(new SlashOnly()));
    }

    /** A trigger is matched against one word, so a multi-word one could never fire. */
    @MessageCommandInfo(names = {"two words"})
    static class MultiWordTrigger implements QCommandBase {
        @MessageHandler void run(@MessageEvent MessageReceivedEvent e) { }
    }

    @Test
    void parse_triggerContainingWhitespace_isRefused() {
        assertNull(MessageCommandParser.parse(new MultiWordTrigger()));
    }

    @MessageCommandInfo(names = "orphan")
    static class NoHandler implements QCommandBase {
    }

    @Test
    void parse_triggersButNoHandler_isRefused() {
        assertNull(MessageCommandParser.parse(new NoHandler()));
    }

    @MessageCommandInfo(names = "cfg")
    static class Named implements QCommandBase {
        @MessageHandler void base(@MessageEvent MessageReceivedEvent e) { }

        @MessageHandler(name = "set") void set(@MessageEvent MessageReceivedEvent e) { }

        @MessageHandler(name = "GET") void get(@MessageEvent MessageReceivedEvent e) { }
    }

    @Test
    void parse_namedHandlers_areKeyedLowercase() {
        MessageCommandDefinition definition = MessageCommandParser.parse(new Named());
        assertAll(
                () -> assertNotNull(definition.getBaseHandler()),
                () -> assertEquals(2, definition.getNamedHandlers().size()),
                () -> assertTrue(definition.getNamedHandlers().containsKey("set")),
                () -> assertTrue(definition.getNamedHandlers().containsKey("get")));
    }

    @MessageCommandInfo(names = "modonly", permissions = Permission.BAN_MEMBERS, deleteTrigger = true,
            guildOnly = false)
    static class Flags implements QCommandBase {
        @MessageHandler void run(@MessageEvent MessageReceivedEvent e, @MessageArg(0) String first) { }
    }

    @Test
    void parse_flags_areCarriedOntoTheDefinition() {
        MessageCommandDefinition definition = MessageCommandParser.parse(new Flags());
        assertAll(
                () -> assertEquals(java.util.EnumSet.of(Permission.BAN_MEMBERS), definition.getPermissions()),
                () -> assertTrue(definition.isDeleteTrigger()),
                () -> assertFalse(definition.isGuildOnly()));
    }

    /** Message commands are opt-in; nothing is built or attached without a config. */
    @Test
    void processor_withoutAConfig_buildsNoTriggerIndex() {
        CommandProcessor processor = new CommandProcessor(List.of(new Uno()), false);
        assertTrue(processor.getMessageTriggers().isEmpty());
    }

    @Test
    void processor_withAConfig_indexesEveryTrigger() {
        CommandProcessor processor = CommandProcessor.builder()
                .commands(List.of(new Uno()))
                .helpCommand(false)
                .messageCommands(MessageCommandConfig.withPrefix("!"))
                .build();

        Map<String, MessageCommandDefinition> triggers = processor.getMessageTriggers();
        assertAll(
                () -> assertEquals(2, triggers.size()),
                () -> assertTrue(triggers.containsKey("uno")),
                () -> assertTrue(triggers.containsKey("startuno")));
    }

    @MessageCommandInfo(names = "uno")
    static class ClashingTrigger implements QCommandBase {
        @MessageHandler void run(@MessageEvent MessageReceivedEvent e) { }
    }

    @Test
    void processor_twoCommandsClaimingOneTrigger_keepsTheFirst() {
        CommandProcessor processor = CommandProcessor.builder()
                .commands(List.of(new Uno(), new ClashingTrigger()))
                .helpCommand(false)
                .messageCommands(MessageCommandConfig.withPrefix("!"))
                .build();

        assertEquals(Uno.class, processor.getMessageTriggers().get("uno").getInstance().getClass());
    }

    /**
     * The deprecated single-argument overload, kept covered because it is still on the API and a
     * consumer written against 0.3.0 still calls it. The mention-aware form is in
     * {@code InvocationFormTest}.
     */
    @Test
    @SuppressWarnings("deprecation")
    void prefixMode_decidesWhichFormsAreAccepted() {
        assertAll(
                () -> assertTrue(PrefixMode.REQUIRED.accepts(true)),
                () -> assertFalse(PrefixMode.REQUIRED.accepts(false)),
                () -> assertTrue(PrefixMode.NONE.accepts(false)),
                () -> assertFalse(PrefixMode.NONE.accepts(true)),
                () -> assertTrue(PrefixMode.OPTIONAL.accepts(true)),
                () -> assertTrue(PrefixMode.OPTIONAL.accepts(false)));
    }

    /** Only these two can fire on ordinary conversation, so only these two need a filter. */
    @Test
    void prefixMode_bareWordCapability() {
        assertAll(
                () -> assertFalse(PrefixMode.REQUIRED.allowsBareWord()),
                () -> assertTrue(PrefixMode.OPTIONAL.allowsBareWord()),
                () -> assertTrue(PrefixMode.NONE.allowsBareWord()));
    }

    @Test
    void config_defaults() {
        MessageCommandConfig config = MessageCommandConfig.withPrefix("?");
        assertAll(
                () -> assertEquals("?", config.prefix()),
                () -> assertTrue(config.allowMentionPrefix()),
                () -> assertTrue(config.ignoreBots()),
                () -> assertTrue(config.replyOnError()),
                () -> assertFalse(config.caseSensitive()),
                () -> assertEquals(MessageCommandFilter.ALLOW_ALL, config.filter()));
    }
}
