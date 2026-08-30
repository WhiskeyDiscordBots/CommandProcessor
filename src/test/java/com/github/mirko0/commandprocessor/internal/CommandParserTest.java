package com.github.mirko0.commandprocessor.internal;

import com.github.mirko0.commandprocessor.CommandCategory;
import com.github.mirko0.commandprocessor.QCommandBase;
import com.github.mirko0.commandprocessor.annotations.Category;
import com.github.mirko0.commandprocessor.annotations.CommandEvent;
import com.github.mirko0.commandprocessor.annotations.CommandHandler;
import com.github.mirko0.commandprocessor.annotations.CommandInfo;
import com.github.mirko0.commandprocessor.annotations.CommandOption;
import com.github.mirko0.commandprocessor.annotations.Group;
import com.github.mirko0.commandprocessor.annotations.SubCommandGroupInfo;
import com.github.mirko0.commandprocessor.annotations.SubCommandHandler;
import com.github.mirko0.commandprocessor.definitions.CommandDefinition;
import com.github.mirko0.commandprocessor.definitions.SubcommandDefinition;
import com.github.mirko0.commandprocessor.example.PingCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Everything the parser has ever got wrong, as tests.
 *
 * <p>No Discord token and no network: JDA's command-data builders construct standalone, which is
 * what makes the whole parser testable.
 */
class CommandParserTest {

    private static SlashCommandData dataOf(CommandDefinition definition) {
        return (SlashCommandData) definition.getCommandData();
    }

    // ------------------------------------------------------------------ groups

    @Nested
    @DisplayName("subcommand groups")
    class Groups {

        @CommandInfo(name = "logs", description = "Logs.",
                groups = @SubCommandGroupInfo(name = "channel", description = "Channel config"))
        static class ThreeInOneGroup implements QCommandBase {
            @Group("channel") @SubCommandHandler(name = "set", description = "Set it.")
            void set(@CommandEvent SlashCommandInteractionEvent e) { }

            @Group("channel") @SubCommandHandler(name = "clear", description = "Clear it.")
            void clear(@CommandEvent SlashCommandInteractionEvent e) { }

            @Group("channel") @SubCommandHandler(name = "show", description = "Show it.")
            void show(@CommandEvent SlashCommandInteractionEvent e) { }
        }

        /**
         * The group data used to be rebuilt and re-added per member, so JDA rejected the command
         * with "Cannot have multiple subcommand groups with the same name" and the exception took
         * the entire registration down with it.
         */
        @Test
        void parse_severalSubcommandsInOneGroup_addsTheGroupOnce() {
            CommandDefinition definition = CommandParser.parseCommand(new ThreeInOneGroup());

            List<SubcommandGroupData> groups = dataOf(definition).getSubcommandGroups();
            assertAll(
                    () -> assertEquals(1, groups.size(), "one group"),
                    () -> assertEquals(3, groups.get(0).getSubcommands().size(), "all three members"),
                    () -> assertEquals(3, definition.getGroups().get("channel").getSubcommands().size()));
        }

        @Test
        void parse_groupedSubcommand_recordsItsGroupName() {
            CommandDefinition definition = CommandParser.parseCommand(new ThreeInOneGroup());

            SubcommandDefinition sub = definition.getGroups().get("channel").getSubcommands().get(0);
            assertEquals("channel", sub.getGroupName());
        }

        @CommandInfo(name = "orphan", description = "Orphan.")
        static class UndeclaredGroup implements QCommandBase {
            @Group("nope") @SubCommandHandler(name = "sub", description = "Sub.")
            void sub(@CommandEvent SlashCommandInteractionEvent e) { }
        }

        @Test
        void parse_subcommandInUndeclaredGroup_isSkippedNotRegistered() {
            CommandDefinition definition = CommandParser.parseCommand(new UndeclaredGroup());

            assertAll(
                    () -> assertTrue(dataOf(definition).getSubcommandGroups().isEmpty()),
                    () -> assertTrue(definition.getSubcommands().isEmpty()));
        }
    }

    // -------------------------------------------------------------- discovery

    @Nested
    @DisplayName("handler discovery")
    class Discovery {

        abstract static class Base implements QCommandBase {
            @SubCommandHandler(name = "inherited", description = "On the base class.", order = 5)
            void inherited(@CommandEvent SlashCommandInteractionEvent e) { }
        }

        @CommandInfo(name = "child", description = "Child.")
        static class Child extends Base {
            @SubCommandHandler(name = "zulu", description = "Last by name, first by order.", order = -1)
            void zulu(@CommandEvent SlashCommandInteractionEvent e) { }

            @SubCommandHandler(name = "alpha", description = "First by name.")
            void alpha(@CommandEvent SlashCommandInteractionEvent e) { }
        }

        /** getDeclaredMethods() alone never saw a handler declared on a superclass. */
        @Test
        void parse_handlerOnSuperclass_isRegistered() {
            List<String> names = subcommandNames(CommandParser.parseCommand(new Child()));
            assertTrue(names.contains("inherited"), () -> "expected the inherited handler in " + names);
        }

        /** Reflection gives no order, so without a tie-break the listing changed between builds. */
        @Test
        void parse_subcommands_areOrderedByOrderThenName() {
            assertEquals(List.of("zulu", "alpha", "inherited"),
                    subcommandNames(CommandParser.parseCommand(new Child())));
        }

        @CommandInfo(name = "base", description = "Base.")
        static class Real implements QCommandBase {
            @CommandHandler void run(@CommandEvent SlashCommandInteractionEvent e) { }
        }

        /** Stands in for a Spring CGLIB proxy: a generated subclass of the real command. */
        static class Proxy extends Real { }

        @Test
        void parse_proxiedSubclass_stillFindsCommandInfo() {
            CommandDefinition definition = CommandParser.parseCommand(new Proxy());
            assertAll(
                    () -> assertNotNull(definition, "a proxied bean used to parse to null and vanish"),
                    () -> assertEquals("base", definition.getName()));
        }

        @Test
        void parse_classWithoutCommandInfo_returnsNull() {
            assertNull(CommandParser.parseCommand(new QCommandBase() { }));
        }

        private List<String> subcommandNames(CommandDefinition definition) {
            return dataOf(definition).getSubcommands().stream()
                    .map(net.dv8tion.jda.api.interactions.commands.build.SubcommandData::getName)
                    .toList();
        }
    }

    // ----------------------------------------------------------------- limits

    @Nested
    @DisplayName("Discord's limits")
    class Limits {

        private static final String LONG_DESCRIPTION =
                "This description is deliberately far longer than the one hundred characters that "
                        + "Discord is willing to accept, so the parser has to trim it.";

        @CommandInfo(name = "messy", description = "")
        static class Messy implements QCommandBase {
            @SubCommandHandler(name = "long", description = LONG_DESCRIPTION)
            void longOne(@CommandEvent SlashCommandInteractionEvent e,
                         @CommandOption(type = OptionType.STRING, name = "optional",
                                 description = "Optional, declared first.") String optional,
                         @CommandOption(type = OptionType.STRING, name = "needed", required = true,
                                 description = "Required, declared second.",
                                 choices = {"a:Label A", "b:Label: with colon"}) String needed) { }
        }

        @Test
        void parse_blankCommandDescription_isSubstitutedRatherThanThrowing() {
            CommandDefinition definition = CommandParser.parseCommand(new Messy());
            assertFalse(dataOf(definition).getDescription().isBlank());
        }

        @Test
        void parse_subcommandDescriptionOver100Chars_isTrimmed() {
            CommandDefinition definition = CommandParser.parseCommand(new Messy());
            assertEquals(100, dataOf(definition).getSubcommands().get(0).getDescription().length());
        }

        /** JDA throws when a required option follows an optional one; that killed registration. */
        @Test
        void parse_requiredOptionAfterOptional_isMovedInFront() {
            List<OptionData> options = dataOf(CommandParser.parseCommand(new Messy()))
                    .getSubcommands().get(0).getOptions();
            assertEquals(List.of("needed", "optional"),
                    options.stream().map(OptionData::getName).toList());
        }

        @Test
        void parse_choiceWithExtraColon_fallsBackToValueAsLabel() {
            List<OptionData> options = dataOf(CommandParser.parseCommand(new Messy()))
                    .getSubcommands().get(0).getOptions();
            OptionData needed = options.get(0);
            assertAll(
                    () -> assertEquals(2, needed.getChoices().size()),
                    () -> assertEquals("Label A", needed.getChoices().get(0).getName()),
                    () -> assertEquals("b", needed.getChoices().get(1).getName()));
        }

        @CommandInfo(name = "unnamed", description = "Unnamed option.")
        static class UnnamedOption implements QCommandBase {
            @CommandHandler
            void run(@CommandEvent SlashCommandInteractionEvent e,
                     @CommandOption(type = OptionType.STRING, name = "", description = "No name.")
                     String broken) { }
        }

        /** An empty option name made JDA throw "Name may not be empty" out of the constructor. */
        @Test
        void parse_optionWithNoName_isSkippedNotFatal() {
            CommandDefinition definition = CommandParser.parseCommand(new UnnamedOption());
            assertAll(
                    () -> assertNotNull(definition),
                    () -> assertTrue(dataOf(definition).getOptions().isEmpty()));
        }

        @CommandInfo(name = "toomany", description = "Too many choices.")
        static class TooManyChoices implements QCommandBase {
            @CommandHandler
            void run(@CommandEvent SlashCommandInteractionEvent e,
                     @CommandOption(type = OptionType.STRING, name = "pick", description = "Pick.",
                             choices = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
                                     "13", "14", "15", "16", "17", "18", "19", "20", "21", "22",
                                     "23", "24", "25", "26", "27"})
                     String pick) { }
        }

        @Test
        void parse_over25Choices_isTruncatedToDiscordsLimit() {
            CommandDefinition definition = CommandParser.parseCommand(new TooManyChoices());
            assertEquals(25, dataOf(definition).getOptions().get(0).getChoices().size());
        }

        @CommandInfo(name = "wrongtype", description = "Choices on a user option.")
        static class ChoicesOnUser implements QCommandBase {
            @CommandHandler
            void run(@CommandEvent SlashCommandInteractionEvent e,
                     @CommandOption(type = OptionType.USER, name = "who", description = "Who.",
                             choices = {"a:A"})
                     net.dv8tion.jda.api.entities.User who) { }
        }

        @Test
        void parse_choicesOnATypeThatCannotHaveThem_areIgnoredNotFatal() {
            CommandDefinition definition = CommandParser.parseCommand(new ChoicesOnUser());
            assertTrue(dataOf(definition).getOptions().get(0).getChoices().isEmpty());
        }
    }

    // ------------------------------------------------------------- metadata

    @Nested
    @DisplayName("metadata")
    class Metadata {

        @Category("Moderation")
        @CommandInfo(name = "ban", description = "Ban a member.",
                aliases = {"Yeet", "ban", "  "},
                permissions = {Permission.BAN_MEMBERS, Permission.MODERATE_MEMBERS},
                contexts = {InteractionContextType.GUILD, InteractionContextType.BOT_DM},
                guildId = "123")
        static class Ban implements QCommandBase {
            @CommandHandler void run(@CommandEvent SlashCommandInteractionEvent e) { }
        }

        @Test
        void parse_category_isNormalisedToLowercase() {
            assertEquals("moderation", CommandParser.parseCommand(new Ban()).getCategoryId());
        }

        @Test
        void parse_noCategory_fallsBackToTheDefault() {
            assertEquals(CommandCategory.DEFAULT_ID,
                    CommandParser.parseCommand(new Discovery.Real()).getCategoryId());
        }

        @Test
        void parse_aliases_areLowercasedDeduplicatedAndExcludeTheCommandName() {
            assertEquals(List.of("yeet"), CommandParser.parseCommand(new Ban()).getAliases());
        }

        @Test
        void parse_permissions_areRecordedForTheHelpCommand() {
            assertEquals(java.util.EnumSet.of(Permission.BAN_MEMBERS, Permission.MODERATE_MEMBERS),
                    CommandParser.parseCommand(new Ban()).getPermissions());
        }

        @Test
        void parse_guildId_marksTheCommandGuildScoped() {
            assertTrue(CommandParser.parseCommand(new Ban()).isGuildScoped());
        }

        /** The singular contextType could only carry one; a command is often valid in several. */
        @Test
        void parse_severalContexts_areAllApplied() {
            assertEquals(2, dataOf(CommandParser.parseCommand(new Ban())).getContexts().size());
        }

        @Test
        void parse_sampleCommand_recordsSubcommandOptionNames() {
            CommandDefinition definition = CommandParser.parseCommand(new PingCommand());
            SubcommandDefinition echo = definition.getGroups().get("test").getSubcommands().stream()
                    .filter(sub -> sub.getName().equals("echo"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(List.of("message"), echo.getOptionNames());
        }
    }
}
