package com.github.mirko0.commandprocessor.internal;

import com.github.mirko0.commandprocessor.annotations.AutoCompleteEvent;
import com.github.mirko0.commandprocessor.annotations.AutoCompleteValue;
import com.github.mirko0.commandprocessor.annotations.CommandEvent;
import com.github.mirko0.commandprocessor.annotations.CommandOption;
import com.github.mirko0.commandprocessor.annotations.MessageArg;
import com.github.mirko0.commandprocessor.annotations.MessageArgs;
import com.github.mirko0.commandprocessor.annotations.MessageEvent;
import com.github.mirko0.commandprocessor.message.Arguments;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Builds the argument array for a handler method from an interaction.
 *
 * <p>Shared by the slash-command dispatcher and the autocomplete dispatcher, which need the same
 * option binding against two different event types. Both extend
 * {@link CommandInteractionPayload}, which is all the option lookup needs.
 *
 * <p><strong>Internal.</strong> Not part of the public API and may change in any release.
 *
 * @since 0.2.0
 */
public final class ParameterBinder {

    private static final Logger log = LoggerFactory.getLogger(ParameterBinder.class);

    private ParameterBinder() {
    }

    /**
     * What a handler can be given.
     *
     * @param event        the event itself, injected for {@code @CommandEvent},
     *                     {@code @AutoCompleteEvent} and {@code @MessageEvent}
     * @param payload      the same event as an option carrier, or null for a message command
     * @param focusedValue what the user has typed into the focused option, or null outside autocomplete
     * @param arguments    the message command's arguments, or null outside a message command
     */
    public record Context(Object event, CommandInteractionPayload payload, String focusedValue,
                          Arguments arguments) {

        /** A slash-command or autocomplete invocation. */
        public static Context ofInteraction(Object event, CommandInteractionPayload payload, String focusedValue) {
            return new Context(event, payload, focusedValue, null);
        }

        /** A message-command invocation, which carries arguments instead of resolved options. */
        public static Context ofMessage(Object event, Arguments arguments) {
            return new Context(event, null, null, arguments);
        }
    }

    /**
     * @param  method  the handler
     * @param  context what is available to inject
     * @return the arguments, positionally; a parameter with no recognised annotation is null
     */
    public static Object[] bind(Method method, Context context) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            args[i] = valueFor(parameters[i], context, method);
        }
        return args;
    }

    private static Object valueFor(Parameter parameter, Context context, Method method) {
        if (parameter.isAnnotationPresent(CommandEvent.class)
                || parameter.isAnnotationPresent(AutoCompleteEvent.class)
                || parameter.isAnnotationPresent(MessageEvent.class)) {
            return eventFor(parameter, context, method);
        }
        if (parameter.isAnnotationPresent(MessageArgs.class)) {
            return context.arguments() == null ? Arguments.empty() : context.arguments();
        }
        if (parameter.isAnnotationPresent(MessageArg.class)) {
            return positional(parameter, context);
        }
        if (parameter.isAnnotationPresent(AutoCompleteValue.class)) {
            return context.focusedValue() == null ? "" : context.focusedValue();
        }
        if (parameter.isAnnotationPresent(CommandOption.class)) {
            return context.payload() == null
                    ? null
                    : optionFor(parameter.getAnnotation(CommandOption.class), context);
        }
        return null;
    }

    /**
     * One positional message argument, converted to the parameter's type.
     *
     * <p>Null for an argument that is absent or does not convert, which is the ordinary case rather
     * than an error: people type commands wrong. That is also why the parameter has to be a
     * reference type — a primitive would throw on unboxing every time someone made a typo.
     */
    private static Object positional(Parameter parameter, Context context) {
        Arguments arguments = context.arguments();
        if (arguments == null) return null;

        int index = parameter.getAnnotation(MessageArg.class).value();
        Class<?> type = parameter.getType();

        if (type == String.class) return arguments.get(index).orElse(null);
        if (type == Long.class) return arguments.asLong(index).orElse(null);
        if (type == Integer.class) return arguments.asLong(index).map(Long::intValue).orElse(null);
        if (type == Double.class) return arguments.asDouble(index).orElse(null);
        if (type == Boolean.class) return arguments.get(index).map(Boolean::parseBoolean).orElse(null);
        if (type == User.class) return arguments.user(index).orElse(null);

        log.error("@MessageArg parameter '{}' is of type {}, which is not supported. Use String, "
                        + "Long, Integer, Double, Boolean or User, or take @MessageArgs and resolve it "
                        + "yourself. It will be null.",
                parameter.getName(), type.getSimpleName());
        return null;
    }

    /**
     * The event, when the parameter can actually hold it.
     *
     * <p>Passing an autocomplete event to a parameter declared as a slash event would fail inside
     * {@code Method.invoke} with an {@code IllegalArgumentException} that names neither the command
     * nor the parameter. Checked here so the log says which method is wrong.
     */
    private static Object eventFor(Parameter parameter, Context context, Method method) {
        if (parameter.getType().isInstance(context.event())) return context.event();

        log.error("Parameter '{}' of {}.{} is declared as {} but this interaction is a {}. "
                        + "It will be null.",
                parameter.getName(), method.getDeclaringClass().getSimpleName(), method.getName(),
                parameter.getType().getSimpleName(), context.event().getClass().getSimpleName());
        return null;
    }

    /**
     * Resolves one option.
     *
     * <p>Lowercased to match what {@link CommandParser} registered. Looking the raw name up made any
     * option declared with a capital letter resolve to null on every invocation, which reads exactly
     * like an optional argument the user did not supply.
     *
     * <p>An option the user did not supply is null, so a parameter must never be a primitive.
     */
    private static Object optionFor(CommandOption option, Context context) {
        OptionMapping mapping = context.payload().getOption(option.name().trim().toLowerCase());
        if (mapping == null) return null;

        return switch (option.type()) {
            case STRING, SUB_COMMAND, SUB_COMMAND_GROUP, UNKNOWN -> mapping.getAsString();
            case INTEGER -> mapping.getAsLong();
            case BOOLEAN -> mapping.getAsBoolean();
            case USER -> mapping.getAsUser();
            case CHANNEL -> mapping.getAsChannel();
            case ROLE -> mapping.getAsRole();
            case MENTIONABLE -> mapping.getAsMentionable();
            case NUMBER -> mapping.getAsDouble();
            case ATTACHMENT -> mapping.getAsAttachment();
        };
    }
}
