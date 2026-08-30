package com.github.mirko0.commandprocessor;

import com.github.mirko0.commandprocessor.definitions.AutoCompleteDefinition;
import com.github.mirko0.commandprocessor.definitions.CommandDefinition;
import com.github.mirko0.commandprocessor.definitions.SubcommandDefinition;
import com.github.mirko0.commandprocessor.definitions.SubcommandGroupDefinition;
import com.github.mirko0.commandprocessor.enumeration.Response;
import com.github.mirko0.commandprocessor.internal.ChoiceCoercion;
import com.github.mirko0.commandprocessor.internal.ParameterBinder;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles Discord slash command interactions by processing and routing them to appropriate command handlers.
 * This class extends JDA's ListenerAdapter to intercept slash command events and manages command execution
 * based on predefined command definitions.
 */
public class SlashCommandHandler extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(SlashCommandHandler.class);

    /** Shown to the user when a handler throws. */
    private static final String FAILURE_MESSAGE = "Error while executing command.";

    /** Shown when Discord delivers a command this build does not have a handler for. */
    private static final String UNKNOWN_MESSAGE =
            "That command is no longer available. Your Discord client may be showing an old version "
                    + "of it - try again in a minute.";

    /**
     * Errors ignored while reporting a command failure back to the user.
     *
     * <p>The interaction may already be gone by the time this runs - which is often exactly why the
     * command failed - and failing to report a failure is not worth a second stack trace.
     */
    private static final ErrorHandler IGNORE_GONE = new ErrorHandler()
            .ignore(ErrorResponse.UNKNOWN_INTERACTION,
                    ErrorResponse.UNKNOWN_WEBHOOK,
                    ErrorResponse.UNKNOWN_MESSAGE);

    private final Map<String, CommandDefinition> commandMap;

    /** Options already reported as having no autocomplete handler, so the warning fires once. */
    private final Set<String> warnedMissingAutoComplete = ConcurrentHashMap.newKeySet();

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
        SubcommandDefinition subDef = groupDef == null ? null : groupDef.getSubcommands().stream()
                .filter(s -> s.getName().equalsIgnoreCase(subName))
                .findFirst()
                .orElse(null);
        if (subDef == null) {
            reportUnknown(event, cmdDef.getName() + " " + groupName + " " + subName);
            return;
        }
        boolean acknowledged = handleDefaultResponse(event, subDef.getDefaultResponse());
        invoke(cmdDef.getInstance(), subDef.getHandlerMethod(), event, acknowledged);
    }

    /**
     * Handles direct subcommands (not part of a group).
     */
    private void handleSubcommand(SlashCommandInteractionEvent event, CommandDefinition cmdDef, String subName) {
        SubcommandDefinition subDef = cmdDef.getSubcommands().stream()
                .filter(s -> s.getName().equalsIgnoreCase(subName))
                .findFirst()
                .orElse(null);
        if (subDef == null) {
            reportUnknown(event, cmdDef.getName() + " " + subName);
            return;
        }
        boolean acknowledged = handleDefaultResponse(event, subDef.getDefaultResponse());
        invoke(cmdDef.getInstance(), subDef.getHandlerMethod(), event, acknowledged);
    }

    /**
     * Answers an invocation this build has no handler for.
     *
     * <p>Reachable in normal operation: Discord caches command definitions client-side, so a
     * subcommand removed in a deployment stays clickable until a user's client refreshes. Falling
     * silent left them looking at "The application did not respond".
     */
    private void reportUnknown(SlashCommandInteractionEvent event, String path) {
        log.warn("No handler for '/{}'. It is registered with Discord but missing from this build.", path);
        event.reply(UNKNOWN_MESSAGE).setEphemeral(true).queue(null, IGNORE_GONE);
    }

    /**
     * Handles base commands (not subcommands).
     */
    private void handleBaseCommand(SlashCommandInteractionEvent event, CommandDefinition cmdDef) {
        boolean acknowledged = handleDefaultResponse(event, cmdDef.getDefaultResponse());
        invoke(cmdDef.getInstance(), cmdDef.getHandlerMethod(), event, acknowledged);
    }

    /**
     * Processes the default response type for a command.
     *
     * @param event    The slash command event
     * @param response The type of response to handle
     * @return true when this acknowledged the interaction, so the handler and the failure path must
     *         use the hook rather than replying again
     */
    private boolean handleDefaultResponse(SlashCommandInteractionEvent event, Response response) {
        if (response == null) return false;
        switch (response) {
            case DEFER -> {
                event.deferReply().queue();
                return true;
            }
            case EPHEMERAL -> {
                event.deferReply(true).queue();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Invokes the command handler method with the appropriate parameters.
     *
     * @param instance     The command instance to invoke the method on
     * @param method       The method to invoke
     * @param event        The slash command event
     * @param acknowledged Whether the interaction was already acknowledged before the handler ran
     */
    private void invoke(Object instance, Method method, SlashCommandInteractionEvent event, boolean acknowledged) {
        try {
            method.invoke(instance, ParameterBinder.bind(method,
                    ParameterBinder.Context.ofInteraction(event, event, null)));
        } catch (Exception e) {
            // method.invoke wraps whatever the handler threw. Logging the wrapper puts
            // "InvocationTargetException" on the line monitoring alerts on and buries the real cause.
            log.error("Command '/{}' failed for user {} in guild {}",
                    event.getFullCommandName(), event.getUser().getId(),
                    event.getGuild() == null ? "DM" : event.getGuild().getId(), unwrap(e));
            reportFailure(event, acknowledged);
        }
    }

    /**
     * Tells the user the command failed, using whichever route the interaction still allows.
     *
     * <p>Replying unconditionally is wrong whenever a default response has already deferred: that
     * reply fails with {@code 40060 Interaction has already been acknowledged}, and the user is left
     * watching "thinking..." until it times out with no error anywhere they can see.
     */
    private void reportFailure(SlashCommandInteractionEvent event, boolean acknowledged) {
        try {
            if (acknowledged || event.isAcknowledged()) {
                event.getHook().editOriginal(FAILURE_MESSAGE).queue(null, IGNORE_GONE);
            }else {
                event.reply(FAILURE_MESSAGE).setEphemeral(true).queue(null, IGNORE_GONE);
            }
        } catch (RuntimeException failure) {
            log.warn("Could not tell the user that the command failed.", failure);
        }
    }

    // ------------------------------------------------------------- autocomplete

    /**
     * Answers an autocomplete request from the option's
     * {@link com.github.mirko0.commandprocessor.annotations.AutoCompleteHandler}.
     *
     * <p>Discord allows three seconds and an autocomplete response cannot be deferred, so the
     * handler runs inline on this event thread. A handler that blocks costs the user their
     * suggestions.
     *
     * <p>Always replies, even with nothing: an unanswered request leaves the client spinning and
     * then showing an error, which is indistinguishable from a bot that is down.
     */
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        CommandDefinition cmdDef = commandMap.get(event.getName().toLowerCase());
        if (cmdDef == null) return;

        AutoCompleteQuery focused = event.getFocusedOption();
        AutoCompleteDefinition handler = cmdDef.findAutoComplete(
                event.getSubcommandGroup(), event.getSubcommandName(), focused.getName());

        if (handler == null) {
            warnOnceAboutMissingHandler(event, focused);
            event.replyChoices().queue(null, IGNORE_GONE);
            return;
        }

        event.replyChoices(suggestions(cmdDef, handler, event, focused)).queue(null, IGNORE_GONE);
    }

    private List<Command.Choice> suggestions(CommandDefinition cmdDef, AutoCompleteDefinition handler,
                                             CommandAutoCompleteInteractionEvent event,
                                             AutoCompleteQuery focused) {
        Method method = handler.getHandlerMethod();
        String origin = cmdDef.getInstance().getClass().getSimpleName() + "." + method.getName();
        try {
            Object returned = method.invoke(cmdDef.getInstance(), ParameterBinder.bind(method,
                    ParameterBinder.Context.ofInteraction(event, event, focused.getValue())));
            return ChoiceCoercion.coerce(returned, focused.getType(), origin);
        } catch (Exception e) {
            // Suggestions are a convenience: a broken provider must not stop the user typing.
            log.error("Autocomplete handler {} failed for option '{}' of '/{}'",
                    origin, focused.getName(), event.getFullCommandName(), unwrap(e));
            return List.of();
        }
    }

    /**
     * Says once that an option asked for autocomplete and nothing answers it.
     *
     * <p>Once per option: this fires on every keystroke, and the condition cannot change while the
     * process runs.
     */
    private void warnOnceAboutMissingHandler(CommandAutoCompleteInteractionEvent event, AutoCompleteQuery focused) {
        String key = event.getFullCommandName() + ":" + focused.getName();
        if (!warnedMissingAutoComplete.add(key)) return;

        log.warn("No @AutoCompleteHandler for option '{}' of '/{}'. Discord is being sent an empty "
                + "list.", focused.getName(), event.getFullCommandName());
    }

    /** The cause, not the reflection wrapper around it. */
    private static Throwable unwrap(Exception thrown) {
        return thrown instanceof InvocationTargetException wrapped && wrapped.getCause() != null
                ? wrapped.getCause()
                : thrown;
    }
}
