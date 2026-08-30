package com.github.mirko0.commandprocessor.help;

import com.github.mirko0.commandprocessor.CommandCategory;
import com.github.mirko0.commandprocessor.QCommandBase;
import com.github.mirko0.commandprocessor.annotations.Category;
import com.github.mirko0.commandprocessor.annotations.CommandEvent;
import com.github.mirko0.commandprocessor.annotations.CommandHandler;
import com.github.mirko0.commandprocessor.annotations.CommandInfo;
import com.github.mirko0.commandprocessor.definitions.CommandDefinition;
import com.github.mirko0.commandprocessor.internal.CommandParser;
import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.selections.SelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The renderer is a pure function, so it is tested directly — no JDA instance, no interaction.
 */
class HelpRendererTest {

    @CommandInfo(name = "ban", description = "Ban a member.", aliases = "yeet")
    @Category("moderation")
    static class Ban implements QCommandBase {
        @CommandHandler void run(@CommandEvent SlashCommandInteractionEvent e) { }
    }

    @CommandInfo(name = "kick", description = "Kick a member.")
    @Category("moderation")
    static class Kick implements QCommandBase {
        @CommandHandler void run(@CommandEvent SlashCommandInteractionEvent e) { }
    }

    private static HelpModel.View view() {
        CommandCategory moderation = CommandCategory.of("moderation", "Moderation")
                .withDescription("Keep the peace").withEmoji("🔨");

        Map<CommandCategory, List<CommandDefinition>> byCategory = new LinkedHashMap<>();
        byCategory.put(moderation, List.of(
                CommandParser.parseCommand(new Ban()),
                CommandParser.parseCommand(new Kick())));
        return new HelpModel.View(byCategory);
    }

    // ------------------------------------------------------------ structure

    @Test
    void render_root_offersACategorySelectMenu() {
        List<MessageTopLevelComponent> rendered = HelpRenderer.render(view(), HelpCustomId.of(HelpView.ROOT));

        List<SelectMenu> menus = menusIn(rendered);
        assertAll(
                () -> assertEquals(1, menus.size()),
                () -> assertEquals(1, ((StringSelectMenu) menus.get(0)).getOptions().size()),
                () -> assertTrue(textIn(rendered).contains("Moderation")));
    }

    @Test
    void render_category_listsItsCommandsAndOffersDetail() {
        List<MessageTopLevelComponent> rendered = HelpRenderer.render(
                view(), HelpCustomId.of(HelpView.CATEGORY, "moderation"));

        String text = textIn(rendered);
        assertAll(
                () -> assertTrue(text.contains("/ban"), text),
                () -> assertTrue(text.contains("/kick"), text),
                () -> assertEquals(2, ((StringSelectMenu) menusIn(rendered).get(0)).getOptions().size()));
    }

    @Test
    void render_command_showsAliasesAndDescription() {
        List<MessageTopLevelComponent> rendered = HelpRenderer.render(
                view(), HelpCustomId.of(HelpView.COMMAND, "ban"));

        String text = textIn(rendered);
        assertAll(
                () -> assertTrue(text.contains("/ban"), text),
                () -> assertTrue(text.contains("yeet"), "aliases should be listed: " + text),
                () -> assertTrue(text.contains("Ban a member."), text));
    }

    /** A category emptied by permission filtering must not strand the user on a blank screen. */
    @Test
    void render_categoryThatIsNotVisible_fallsBackToRoot() {
        List<MessageTopLevelComponent> rendered = HelpRenderer.render(
                view(), HelpCustomId.of(HelpView.CATEGORY, "does-not-exist"));

        assertTrue(textIn(rendered).contains("Commands"));
    }

    @Test
    void render_commandThatIsNotVisible_fallsBackToRoot() {
        List<MessageTopLevelComponent> rendered = HelpRenderer.render(
                view(), HelpCustomId.of(HelpView.COMMAND, "nuke"));

        assertTrue(textIn(rendered).contains("Commands"));
    }

    @Test
    void render_nothingVisible_saysSoRatherThanRenderingAnEmptyMenu() {
        HelpModel.View empty = new HelpModel.View(new LinkedHashMap<>());

        List<MessageTopLevelComponent> rendered = HelpRenderer.render(empty, HelpCustomId.of(HelpView.ROOT));
        assertAll(
                () -> assertTrue(menusIn(rendered).isEmpty()),
                () -> assertTrue(textIn(rendered).contains("no commands")));
    }

    // --------------------------------------------------------------- limits

    /**
     * Discord rejects an entire message with 50035 COMPONENT_CUSTOM_ID_DUPLICATED when two
     * components share an id — disabled ones included — so nothing sends at all.
     */
    @Test
    void render_everyView_hasNoDuplicateCustomIds() {
        HelpModel.View view = paged(60);

        for (HelpCustomId id : List.of(
                HelpCustomId.of(HelpView.ROOT),
                new HelpCustomId(HelpView.ROOT, "", 1),
                HelpCustomId.of(HelpView.CATEGORY, "bulk"),
                new HelpCustomId(HelpView.CATEGORY, "bulk", 1),
                new HelpCustomId(HelpView.CATEGORY, "bulk", 2),
                HelpCustomId.of(HelpView.COMMAND, "cmd07"))) {

            List<String> ids = customIdsIn(HelpRenderer.render(view, id));
            Set<String> unique = new HashSet<>(ids);
            assertEquals(ids.size(), unique.size(), () -> "duplicate custom id in view " + id + ": " + ids);
        }
    }

    @Test
    void render_moreCommandsThanFitOnAPage_paginates() {
        HelpModel.View view = paged(60);

        List<MessageTopLevelComponent> first = HelpRenderer.render(
                view, HelpCustomId.of(HelpView.CATEGORY, "bulk"));
        List<Button> buttons = buttonsIn(first);

        assertAll(
                () -> assertTrue(buttons.stream().anyMatch(b -> "▶".equals(b.getLabel())), "a next button"),
                () -> assertTrue(buttons.stream()
                                .anyMatch(b -> "◀".equals(b.getLabel()) && b.isDisabled()),
                        "previous disabled on the first page"),
                () -> assertEquals(HelpRenderer.COMMANDS_PER_PAGE,
                        ((StringSelectMenu) menusIn(first).get(0)).getOptions().size()));
    }

    @Test
    void render_pageBeyondTheLast_clampsInsteadOfRenderingNothing() {
        HelpModel.View view = paged(60);

        List<MessageTopLevelComponent> rendered = HelpRenderer.render(
                view, new HelpCustomId(HelpView.CATEGORY, "bulk", 99));

        assertFalse(menusIn(rendered).isEmpty(), "a clamped page still lists commands");
    }

    /** Discord rejects a select option whose description is over its limit. */
    @Test
    void render_longDescriptions_areShortenedForSelectOptions() {
        HelpModel.View view = paged(3);

        for (SelectMenu menu : menusIn(HelpRenderer.render(view, HelpCustomId.of(HelpView.CATEGORY, "bulk")))) {
            for (SelectOption option : ((StringSelectMenu) menu).getOptions()) {
                String description = option.getDescription();
                if (description == null) continue;
                assertTrue(description.length() <= SelectOption.DESCRIPTION_MAX_LENGTH,
                        () -> "description too long: " + description);
            }
        }
    }

    @Test
    void render_everyView_staysWithinTheButtonIdLimit() {
        for (String id : customIdsIn(HelpRenderer.render(paged(60), HelpCustomId.of(HelpView.CATEGORY, "bulk")))) {
            assertTrue(id.length() <= Button.ID_MAX_LENGTH, () -> "id too long: " + id);
        }
    }

    // ----------------------------------------------------------- test helpers

    /** A category holding {@code count} generated commands, for the paging assertions. */
    private static HelpModel.View paged(int count) {
        CommandCategory bulk = CommandCategory.of("bulk", "Bulk");

        List<CommandDefinition> commands = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            CommandDefinition definition = new CommandDefinition();
            definition.setName(String.format("cmd%02d", index));
            definition.setCategoryId("bulk");
            definition.setDescription("A command with a description long enough to need shortening "
                    + "when it is put into a select menu option, which Discord caps well below this.");
            commands.add(definition);
        }

        Map<CommandCategory, List<CommandDefinition>> byCategory = new LinkedHashMap<>();
        byCategory.put(bulk, commands);
        return new HelpModel.View(byCategory);
    }

    private static List<Component> flatten(List<MessageTopLevelComponent> rendered) {
        List<Component> all = new ArrayList<>();
        for (MessageTopLevelComponent top : rendered) {
            all.add(top);
            if (top instanceof Container container) {
                for (Component child : container.getComponents()) {
                    all.add(child);
                    if (child instanceof ActionRow row) all.addAll(row.getComponents());
                }
            }
        }
        return all;
    }

    private static String textIn(List<MessageTopLevelComponent> rendered) {
        StringBuilder text = new StringBuilder();
        flatten(rendered).stream()
                .filter(TextDisplay.class::isInstance)
                .forEach(component -> text.append(((TextDisplay) component).getContent()).append('\n'));
        return text.toString();
    }

    private static List<SelectMenu> menusIn(List<MessageTopLevelComponent> rendered) {
        return flatten(rendered).stream().filter(SelectMenu.class::isInstance).map(SelectMenu.class::cast).toList();
    }

    private static List<Button> buttonsIn(List<MessageTopLevelComponent> rendered) {
        return flatten(rendered).stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
    }

    private static List<String> customIdsIn(List<MessageTopLevelComponent> rendered) {
        List<String> ids = new ArrayList<>();
        for (Component component : flatten(rendered)) {
            if (component instanceof Button button && button.getCustomId() != null) ids.add(button.getCustomId());
            if (component instanceof SelectMenu menu && menu.getCustomId() != null) ids.add(menu.getCustomId());
        }
        return ids;
    }
}
