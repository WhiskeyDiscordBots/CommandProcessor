package com.github.mirko0.commandprocessor.help;

import com.github.mirko0.commandprocessor.CommandCategory;
import com.github.mirko0.commandprocessor.definitions.CommandDefinition;
import com.github.mirko0.commandprocessor.definitions.SubcommandDefinition;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the help command's screens as Components V2.
 *
 * <p>A pure function of a {@link HelpModel.View} and a {@link HelpCustomId} — no JDA instance, no
 * network, no state. That is what makes it testable without a Discord connection, and it is why the
 * same code renders both the initial reply and every subsequent click.
 *
 * @since 0.1.0
 */
public final class HelpRenderer {

    /** Discord allows 25 options in a select menu; a category list longer than that is paged. */
    static final int CATEGORIES_PER_PAGE = 25;

    /** Commands listed on one page of a category. Also the select-menu cap. */
    static final int COMMANDS_PER_PAGE = 20;

    private static final Color ACCENT = new Color(0x5865F2);
    private static final int SELECT_DESCRIPTION_MAX = SelectOption.DESCRIPTION_MAX_LENGTH;

    private HelpRenderer() {
    }

    /**
     * Renders whichever screen {@code id} names, falling back to the root when the target no longer
     * exists — a category that lost its last visible command, or a command the caller may not see.
     *
     * @param  view what this caller is allowed to see
     * @param  id   the screen to render
     * @return the top-level components, ready for {@code useComponentsV2()}
     */
    @NotNull
    public static List<MessageTopLevelComponent> render(@NotNull HelpModel.View view, @NotNull HelpCustomId id) {
        if (view.isEmpty()) return List.of(empty());

        return switch (id.view()) {
            case CATEGORY -> {
                CommandCategory category = view.category(id.arg());
                yield category == null ? root(view, id) : category(view, category, id);
            }
            case COMMAND -> {
                CommandDefinition command = view.find(id.arg());
                yield command == null ? root(view, id) : command(view, command);
            }
            default -> root(view, id);
        };
    }

    // ------------------------------------------------------------------ root

    private static List<MessageTopLevelComponent> root(HelpModel.View view, HelpCustomId id) {
        List<CommandCategory> categories = view.categories();
        int pages = pageCount(categories.size(), CATEGORIES_PER_PAGE);
        int page = clamp(id.page(), pages);
        List<CommandCategory> shown = page(categories, page, CATEGORIES_PER_PAGE);

        StringBuilder body = new StringBuilder("## Commands\n")
                .append(view.commandCount()).append(" command")
                .append(view.commandCount() == 1 ? "" : "s")
                .append(" across ").append(categories.size()).append(" categor")
                .append(categories.size() == 1 ? "y" : "ies")
                .append(". Pick one below.\n");

        for (CommandCategory category : shown) {
            int count = view.commandsIn(category).size();
            body.append('\n').append(category.emoji() == null ? "" : category.emoji().getFormatted() + " ")
                    .append("**").append(category.displayName()).append("** — ")
                    .append(count).append(count == 1 ? " command" : " commands");
            if (category.description() != null) {
                body.append('\n').append("-# ").append(category.description());
            }
        }

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of(body.toString()));
        children.add(Separator.createDivider(Separator.Spacing.SMALL));
        children.add(categorySelect(view, shown));
        pagination(children, id.withPage(page), pages, HelpView.ROOT, "");

        return List.of(Container.of(children).withAccentColor(ACCENT));
    }

    private static ActionRow categorySelect(HelpModel.View view, List<CommandCategory> categories) {
        StringSelectMenu.Builder menu = StringSelectMenu
                .create(HelpCustomId.of(HelpView.CATEGORY).encode())
                .setPlaceholder("Choose a category");

        for (CommandCategory category : categories) {
            SelectOption option = SelectOption.of(category.displayName(), category.id())
                    .withDescription(shorten(category.description() != null
                            ? category.description()
                            : view.commandsIn(category).size() + " commands"));
            if (category.emoji() != null) option = option.withEmoji(category.emoji());
            menu.addOptions(option);
        }
        return ActionRow.of(menu.build());
    }

    // -------------------------------------------------------------- category

    private static List<MessageTopLevelComponent> category(HelpModel.View view, CommandCategory category,
                                                           HelpCustomId id) {
        List<CommandDefinition> commands = view.commandsIn(category);
        int pages = pageCount(commands.size(), COMMANDS_PER_PAGE);
        int page = clamp(id.page(), pages);
        List<CommandDefinition> shown = page(commands, page, COMMANDS_PER_PAGE);

        StringBuilder body = new StringBuilder("## ")
                .append(category.emoji() == null ? "" : category.emoji().getFormatted() + " ")
                .append(category.displayName()).append('\n');
        if (category.description() != null) body.append(category.description()).append('\n');

        for (CommandDefinition command : shown) {
            body.append("\n`/").append(command.getName()).append('`');
            if (!command.getAliases().isEmpty()) {
                body.append("  ").append(command.getAliases().stream()
                        .map(alias -> "`" + alias + "`")
                        .collect(Collectors.joining(" ")));
            }
            body.append('\n').append("-# ").append(command.getDescription());
        }

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of(body.toString()));
        children.add(Separator.createDivider(Separator.Spacing.SMALL));
        children.add(commandSelect(category, shown));
        pagination(children, id.withPage(page), pages, HelpView.CATEGORY, category.id());
        children.add(ActionRow.of(backButton("All categories", HelpCustomId.of(HelpView.ROOT))));

        return List.of(Container.of(children).withAccentColor(ACCENT));
    }

    private static ActionRow commandSelect(CommandCategory category, List<CommandDefinition> commands) {
        StringSelectMenu.Builder menu = StringSelectMenu
                .create(HelpCustomId.of(HelpView.COMMAND, category.id()).encode())
                .setPlaceholder("Show details for a command");

        for (CommandDefinition command : commands) {
            menu.addOptions(SelectOption.of("/" + command.getName(), command.getName())
                    .withDescription(shorten(command.getDescription())));
        }
        return ActionRow.of(menu.build());
    }

    // --------------------------------------------------------------- command

    private static List<MessageTopLevelComponent> command(HelpModel.View view, CommandDefinition command) {
        CommandCategory category = view.categoryOf(command);

        StringBuilder body = new StringBuilder("## /").append(command.getName()).append('\n')
                .append(command.getDescription()).append('\n');

        if (!command.getAliases().isEmpty()) {
            body.append("\n**Also known as** ")
                    .append(command.getAliases().stream()
                            .map(alias -> "`" + alias + "`")
                            .collect(Collectors.joining(", ")))
                    .append('\n').append("-# Aliases are not slash commands. They work in the help "
                            + "menu and, where enabled, as message commands.\n");
        }

        if (!command.getPermissions().isEmpty()) {
            body.append("\n**Requires** ")
                    .append(command.getPermissions().stream()
                            .map(Permission::getName)
                            .collect(Collectors.joining(", ")))
                    .append('\n');
        }

        List<SubcommandDefinition> subcommands = command.getAllSubcommands();
        if (!subcommands.isEmpty()) {
            body.append("\n**Subcommands**\n");
            for (SubcommandDefinition sub : subcommands) {
                body.append("`/").append(command.getName()).append(' ')
                        .append(sub.getGroupName() == null ? "" : sub.getGroupName() + " ")
                        .append(sub.getName());
                sub.getOptionNames().forEach(option -> body.append(" <").append(option).append('>'));
                body.append("`\n").append("-# ").append(sub.getDescription()).append('\n');
            }
        }

        if (command.isGuildScoped()) {
            body.append("\n-# Available in this server only.\n");
        }

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of(body.toString()));
        children.add(Separator.createDivider(Separator.Spacing.SMALL));
        children.add(ActionRow.of(
                backButton(category == null ? "Back" : category.displayName(),
                        category == null
                                ? HelpCustomId.of(HelpView.ROOT)
                                : HelpCustomId.of(HelpView.CATEGORY, category.id())),
                Button.secondary(HelpCustomId.of(HelpView.ROOT).encode(), "All categories")));

        return List.of(Container.of(children).withAccentColor(ACCENT));
    }

    // ----------------------------------------------------------------- parts

    private static Container empty() {
        return Container.of(TextDisplay.of("## Commands\nThere are no commands you can use here."))
                .withAccentColor(ACCENT);
    }

    private static Button backButton(String label, HelpCustomId target) {
        return Button.secondary(target.encode(), "← " + label);
    }

    /**
     * Adds a previous/next row, but only when there is more than one page.
     *
     * <p>Every id in one message must be unique, disabled ones included — Discord rejects the whole
     * message otherwise — so the two buttons always carry different page numbers.
     */
    private static void pagination(List<ContainerChildComponent> children, HelpCustomId id,
                                   int pages, HelpView view, String arg) {
        if (pages <= 1) return;

        int page = id.page();
        Button previous = Button.secondary(
                new HelpCustomId(view, arg, Math.max(0, page - 1)).encode(), "◀");
        Button next = Button.secondary(
                new HelpCustomId(view, arg, Math.min(pages - 1, page + 1)).encode(), "▶");

        if (page == 0) previous = previous.asDisabled();
        if (page == pages - 1) next = next.asDisabled();

        children.add(ActionRow.of(previous,
                Button.secondary("cph:noop:" + page, "Page " + (page + 1) + " / " + pages).asDisabled(),
                next));
    }

    private static <T> List<T> page(List<T> all, int page, int size) {
        int from = Math.min(page * size, all.size());
        return all.subList(from, Math.min(from + size, all.size()));
    }

    private static int pageCount(int total, int size) {
        return Math.max(1, (total + size - 1) / size);
    }

    private static int clamp(int page, int pages) {
        return Math.min(Math.max(page, 0), pages - 1);
    }

    /** Select-option descriptions are capped by Discord; an over-long one rejects the message. */
    private static String shorten(String text) {
        if (text == null) return null;
        String single = text.replace('\n', ' ').trim();
        return single.length() <= SELECT_DESCRIPTION_MAX
                ? single
                : single.substring(0, SELECT_DESCRIPTION_MAX - 1) + "…";
    }
}
