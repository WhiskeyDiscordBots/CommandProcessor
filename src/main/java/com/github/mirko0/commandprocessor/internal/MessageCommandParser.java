package com.github.mirko0.commandprocessor.internal;

import com.github.mirko0.commandprocessor.QCommandBase;
import com.github.mirko0.commandprocessor.annotations.MessageCommandInfo;
import com.github.mirko0.commandprocessor.annotations.MessageHandler;
import com.github.mirko0.commandprocessor.definitions.MessageCommandDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a class annotated with {@link MessageCommandInfo} into a {@link MessageCommandDefinition}.
 *
 * <p><strong>Internal.</strong> Not part of the public API and may change in any release.
 *
 * @since 0.3.0
 */
public final class MessageCommandParser {

    private static final Logger log = LoggerFactory.getLogger(MessageCommandParser.class);

    private MessageCommandParser() {
    }

    /**
     * @param  command the command instance
     * @return its message-command definition, or null when the class declares none, declares no
     *         usable trigger, or declares no {@code @MessageHandler}
     */
    public static MessageCommandDefinition parse(QCommandBase command) {
        Class<?> clazz = command.getClass();
        MessageCommandInfo info = Annotations.find(clazz, MessageCommandInfo.class);
        if (info == null) return null;

        String origin = clazz.getSimpleName();
        List<String> triggers = triggers(info, origin);
        if (triggers.isEmpty()) {
            log.error("@MessageCommandInfo on {} declares no usable trigger, so it can never fire.", origin);
            return null;
        }

        MessageCommandDefinition definition = new MessageCommandDefinition();
        definition.setName(triggers.get(0));
        definition.setTriggers(triggers);
        definition.setPrefixMode(info.prefix());
        definition.setGuildOnly(info.guildOnly());
        definition.setDeleteTrigger(info.deleteTrigger());
        definition.setPermissions(info.permissions());
        definition.setInstance(command);

        collectHandlers(definition, Annotations.handlerMethods(clazz), origin);

        if (!definition.hasHandler()) {
            log.error("{} declares @MessageCommandInfo but no @MessageHandler, so typing {} would do "
                    + "nothing. It is not registered.", origin, triggers.get(0));
            return null;
        }
        return definition;
    }

    /** Triggers, lowercased and de-duplicated, with blanks and multi-word entries refused. */
    private static List<String> triggers(MessageCommandInfo info, String origin) {
        List<String> triggers = new ArrayList<>();

        for (String raw : info.names()) {
            if (raw == null || raw.isBlank()) continue;

            String trigger = raw.trim().toLowerCase();
            if (trigger.chars().anyMatch(Character::isWhitespace)) {
                // Matching is on the first token, so a trigger with a space could never equal one.
                log.error("Trigger \"{}\" on {} contains whitespace. A trigger is matched against the "
                        + "first word of a message, so it can never fire. It is ignored.", raw, origin);
                continue;
            }
            if (!triggers.contains(trigger)) triggers.add(trigger);
        }
        return triggers;
    }

    private static void collectHandlers(MessageCommandDefinition definition, List<Method> methods, String origin) {
        Map<String, Method> named = new LinkedHashMap<>();

        for (Method method : methods) {
            MessageHandler anno = method.getAnnotation(MessageHandler.class);
            if (anno == null) continue;

            Annotations.makeAccessible(method, origin);
            String name = anno.name() == null ? "" : anno.name().trim().toLowerCase();

            if (name.isEmpty()) {
                if (definition.getBaseHandler() != null) {
                    log.warn("{} declares more than one unnamed @MessageHandler ('{}' and '{}'). "
                                    + "Keeping '{}'; the other is ignored.",
                            origin, definition.getBaseHandler().getName(), method.getName(),
                            definition.getBaseHandler().getName());
                    continue;
                }
                definition.setBaseHandler(method);
                continue;
            }

            Method previous = named.putIfAbsent(name, method);
            if (previous != null) {
                log.warn("{} declares two @MessageHandler methods for '{}' ('{}' and '{}'). "
                                + "Keeping '{}'.",
                        origin, name, previous.getName(), method.getName(), previous.getName());
            }
        }
        definition.setNamedHandlers(named);
    }
}
