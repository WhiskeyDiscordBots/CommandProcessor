package com.github.mirko0.commandprocessor.help;

import com.github.mirko0.commandprocessor.QCommandBase;
import com.github.mirko0.commandprocessor.annotations.CommandEvent;
import com.github.mirko0.commandprocessor.annotations.CommandHandler;
import com.github.mirko0.commandprocessor.annotations.CommandInfo;
import com.github.mirko0.commandprocessor.annotations.CommandOption;
import com.github.mirko0.commandprocessor.enumeration.Response;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in {@code /help} command.
 *
 * <p>Lists every command the caller can actually use, grouped by
 * {@link com.github.mirko0.commandprocessor.annotations.Category @Category}, and lets them drill in
 * with a select menu. Optionally takes a command name or alias to open that command directly.
 *
 * <p>Added automatically unless the processor is constructed with {@code helpCommand = false}. If
 * you want your own {@code /help}, pass {@code false} — two commands cannot share a name.
 *
 * <p>Replies ephemerally: a help menu is a conversation with one person, and the buttons on it edit
 * that person's own message.
 *
 * @since 0.0.1
 */
@CommandInfo(
        name = "help",
        description = "Show the commands you can use.",
        aliases = {"commands"})
public final class HelpCommand implements QCommandBase {

    private final HelpModel model;

    /**
     * @param model supplies the commands and categories to list
     */
    public HelpCommand(@NotNull HelpModel model) {
        this.model = model;
    }

    /**
     * @param event   the invocation
     * @param command an optional command name or alias to open directly
     */
    @CommandHandler(defaultResponse = Response.EPHEMERAL)
    public void execute(@CommandEvent SlashCommandInteractionEvent event,
                        @CommandOption(type = OptionType.STRING, name = "command",
                                description = "Jump straight to one command.")
                        String command) {

        HelpModel.View view = model.viewFor(event);
        HelpCustomId target = command == null || command.isBlank()
                ? HelpCustomId.of(HelpView.ROOT)
                : openFor(view, command);

        event.getHook()
                .editOriginalComponents(HelpRenderer.render(view, target))
                .useComponentsV2(true)
                .queue();
    }

    /**
     * Where {@code command} should land.
     *
     * <p>An unknown name falls back to the overview rather than erroring: the option is a shortcut,
     * and a typo should still leave the user looking at the help they asked for.
     */
    private HelpCustomId openFor(HelpModel.View view, String command) {
        return view.find(command) == null
                ? HelpCustomId.of(HelpView.ROOT)
                : HelpCustomId.of(HelpView.COMMAND, view.find(command).getName());
    }
}
