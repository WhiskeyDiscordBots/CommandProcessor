package com.github.mirko0.commandprocessor;

import com.github.mirko0.commandprocessor.annotations.CommandEvent;
import com.github.mirko0.commandprocessor.annotations.CommandOption;
import com.github.mirko0.commandprocessor.enumeration.Response;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * Handles Discord slash command interactions by processing and routing them to appropriate command handlers.
 * This class extends JDA's ListenerAdapter to intercept slash command events and manages command execution
 * based on predefined command definitions.
 */
public class SlashCommandHandler extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(SlashCommandHandler.class);
    private final Map<String, CommandDefinition> commandMap;

    /**
     * Constructs a new SlashCommandHandler with the specified command definitions.
     *
     * @param commandMap A map containing command names and their corresponding definitions
     */
    public SlashCommandHandler(Map<String, CommandDefinition> commandMap) {
        this.commandMap = commandMap;
    }

    /**
     * Handles incoming slash command interactions by routing them to appropriate command handlers.
     * Supports base commands, subcommands, and subcommand groups.
     *
     * @param event The slash command interaction event from Discord
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmdName = event.getName().toLowerCase();
        CommandDefinition cmdDef = commandMap.get(cmdName);
        if (cmdDef == null) return;

        String subName = event.getSubcommandName();
        String groupName = event.getSubcommandGroup();

        if (groupName != null && subName != null) {
            handleGroupSubcommand(event, cmdDef, groupName, subName);
        }else if (subName != null) {
            handleSubcommand(event, cmdDef, subName);
        }else if (cmdDef.getHandlerMethod() != null) {
            handleBaseCommand(event, cmdDef);
        }
    }

    /**
     * Handles commands that are part of a subcommand group.
     */
    private void handleGroupSubcommand(SlashCommandInteractionEvent event, CommandDefinition cmdDef,
                                       String groupName, String subName) {
        SubcommandGroupDefinition groupDef = cmdDef.getGroups().get(groupName.toLowerCase());
        if (groupDef != null) {
            SubcommandDefinition subDef = groupDef.getSubcommands().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(subName))
                    .findFirst()
                    .orElse(null);
            if (subDef != null) {
                handleDefaultResponse(event, subDef.getDefaultResponse());
                invoke(cmdDef.getInstance(), subDef.getHandlerMethod(), event);
            }
        }
    }

    /**
     * Handles direct subcommands (not part of a group).
     */
    private void handleSubcommand(SlashCommandInteractionEvent event, CommandDefinition cmdDef, String subName) {
        SubcommandDefinition subDef = cmdDef.getSubcommands().stream()
                .filter(s -> s.getName().equalsIgnoreCase(subName))
                .findFirst()
                .orElse(null);
        if (subDef != null) {
            handleDefaultResponse(event, subDef.getDefaultResponse());
            invoke(cmdDef.getInstance(), subDef.getHandlerMethod(), event);
        }
    }

    /**
     * Handles base commands (not subcommands).
     */
    private void handleBaseCommand(SlashCommandInteractionEvent event, CommandDefinition cmdDef) {
        handleDefaultResponse(event, cmdDef.getDefaultResponse());
        invoke(cmdDef.getInstance(), cmdDef.getHandlerMethod(), event);
    }

    /**
     * Processes the default response type for a command.
     *
     * @param event    The slash command event
     * @param response The type of response to handle
     */
    private void handleDefaultResponse(SlashCommandInteractionEvent event, Response response) {
        switch (response) {
            case NONE -> {
            } // No action needed
            case DEFER -> event.deferReply().queue();
            case EPHEMERAL -> event.deferReply(true).queue();
        }
    }

    /**
     * Invokes the command handler method with the appropriate parameters.
     * Handles parameter injection for command events and options.
     *
     * @param instance The command instance to invoke the method on
     * @param method   The method to invoke
     * @param event    The slash command event
     */
    private void invoke(Object instance, Method method, SlashCommandInteractionEvent event) {
        try {
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(CommandEvent.class)) {
                    args[i] = event;
                }else if (parameters[i].isAnnotationPresent(CommandOption.class)) {
                    processCommandOption(parameters[i], event, args, i);
                }
            }

            method.setAccessible(true);
            method.invoke(instance, args);
        } catch (Exception e) {
            event.reply("Error while executing command.").setEphemeral(true).queue();
            log.error("Command execution failed", e);
        }
    }

    /**
     * Processes command options and converts them to appropriate types.
     *
     * @param parameter The parameter to process
     * @param event     The slash command event
     * @param args      The argument array to populate
     * @param index     The index in the argument array
     */
    private void processCommandOption(Parameter parameter, SlashCommandInteractionEvent event,
                                      Object[] args, int index) {
        CommandOption opt = parameter.getAnnotation(CommandOption.class);
        OptionMapping optionMapping = event.getOption(opt.name());
        if (optionMapping == null) {
            args[index] = null;
            return;
        }

        args[index] = switch (opt.type()) {
            case STRING, SUB_COMMAND, SUB_COMMAND_GROUP, UNKNOWN -> optionMapping.getAsString();
            case INTEGER -> optionMapping.getAsLong();
            case BOOLEAN -> optionMapping.getAsBoolean();
            case USER -> optionMapping.getAsUser();
            case CHANNEL -> optionMapping.getAsChannel();
            case ROLE -> optionMapping.getAsRole();
            case MENTIONABLE -> optionMapping.getAsMentionable();
            case NUMBER -> optionMapping.getAsDouble();
            case ATTACHMENT -> optionMapping.getAsAttachment();
        };
    }
}