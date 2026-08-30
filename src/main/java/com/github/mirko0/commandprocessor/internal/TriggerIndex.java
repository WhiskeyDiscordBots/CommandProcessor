package com.github.mirko0.commandprocessor.internal;

import com.github.mirko0.commandprocessor.QCommandBase;
import com.github.mirko0.commandprocessor.definitions.MessageCommandDefinition;
import com.github.mirko0.commandprocessor.message.MessageCommandConfig;
import com.github.mirko0.commandprocessor.message.MessageCommandFilter;
import com.github.mirko0.commandprocessor.message.PrefixMode;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every message-command trigger, keyed for one hash lookup per message.
 *
 * <p><strong>Internal.</strong> Not part of the public API and may change in any release.
 *
 * @since 0.3.0
 */
public final class TriggerIndex {

    private static final Logger log = LoggerFactory.getLogger(TriggerIndex.class);

    private final Map<String, MessageCommandDefinition> byTrigger;
    private final List<MessageCommandDefinition> definitions;

    private TriggerIndex(Map<String, MessageCommandDefinition> byTrigger,
                         List<MessageCommandDefinition> definitions) {
        this.byTrigger = byTrigger;
        this.definitions = definitions;
    }

    /** An index with nothing in it, used when message commands are disabled. */
    public static TriggerIndex empty() {
        return new TriggerIndex(Map.of(), List.of());
    }

    /**
     * @param commands every registered command; those without {@code @MessageCommandInfo} are skipped
     * @param config   supplies the case-sensitivity rule the keys are built with
     */
    public static TriggerIndex of(Collection<? extends QCommandBase> commands, MessageCommandConfig config) {
        Map<String, MessageCommandDefinition> byTrigger = new LinkedHashMap<>();
        List<MessageCommandDefinition> definitions = new ArrayList<>();

        for (QCommandBase command : commands) {
            MessageCommandDefinition definition = MessageCommandParser.parse(command);
            if (definition == null) continue;

            definitions.add(definition);
            for (String trigger : definition.getTriggers()) {
                String key = config.caseSensitive() ? trigger : trigger.toLowerCase();

                MessageCommandDefinition previous = byTrigger.putIfAbsent(key, definition);
                if (previous != null) {
                    log.warn("Two message commands claim the trigger '{}' ({} and {}). Keeping the "
                                    + "first; the second will never fire.",
                            trigger, previous.getInstance().getClass().getSimpleName(),
                            definition.getInstance().getClass().getSimpleName());
                }
            }
        }

        validate(byTrigger, definitions, config);
        return new TriggerIndex(Collections.unmodifiableMap(byTrigger), List.copyOf(definitions));
    }

    /** The triggers, ready to hand to the dispatcher. */
    public Map<String, MessageCommandDefinition> triggers() {
        return byTrigger;
    }

    public boolean isEmpty() {
        return byTrigger.isEmpty();
    }

    /** True when at least one trigger can fire without the bot being mentioned. */
    public boolean needsMessageContent() {
        return !byTrigger.isEmpty();
    }

    private static void validate(Map<String, MessageCommandDefinition> byTrigger,
                                 List<MessageCommandDefinition> definitions,
                                 MessageCommandConfig config) {
        if (byTrigger.isEmpty()) return;

        boolean anyRequiresPrefix = definitions.stream()
                .anyMatch(definition -> definition.getPrefixMode() == PrefixMode.REQUIRED);

        if (config.prefix().isEmpty() && anyRequiresPrefix && !config.allowMentionPrefix()) {
            log.error("Message commands are enabled with no prefix, no mention prefix, and commands "
                    + "that require a prefix. Those commands can never fire.");
        }

        if (!config.prefix().isEmpty()) {
            char last = config.prefix().charAt(config.prefix().length() - 1);
            if (Character.isLetterOrDigit(last)) {
                log.warn("The message-command prefix \"{}\" ends in a letter or digit, so \"{}help\" "
                                + "and a word that merely starts the same way are hard to tell apart. "
                                + "A punctuation prefix such as \"!\" avoids that.",
                        config.prefix(), config.prefix());
            }
        }

        List<String> bare = definitions.stream()
                .filter(definition -> definition.getPrefixMode().allowsBareWord())
                .map(MessageCommandDefinition::getName)
                .toList();

        if (!bare.isEmpty() && config.filter() == MessageCommandFilter.ALLOW_ALL) {
            log.warn("Message command(s) {} can fire without a prefix and no MessageCommandFilter is "
                            + "set. A bare trigger fires on any message whose first word matches, in "
                            + "every channel the bot can see. Set a filter that narrows when they apply.",
                    bare);
        }
    }

    /**
     * Reports, once, when the bot cannot actually read the messages it is being asked to match.
     *
     * <p>Without {@code MESSAGE_CONTENT} Discord sends an empty body for anything that does not
     * mention the bot, so every prefixed and every bare trigger silently never fires. The symptom is
     * "nothing happens", which is indistinguishable from a typo, a permission problem, or a bug in
     * the consumer's handler — so it is worth being loud about.
     *
     * @param jda    any shard, for its intents
     * @param config the configuration, for whether mention-prefixing alone would be enough
     */
    public void warnAboutIntents(JDA jda, MessageCommandConfig config) {
        if (isEmpty()) return;
        if (jda.getGatewayIntents().contains(GatewayIntent.MESSAGE_CONTENT)) return;

        if (config.allowMentionPrefix()) {
            log.warn("Message commands are enabled without the MESSAGE_CONTENT intent. Discord only "
                    + "sends message content for messages that mention the bot, so commands will work "
                    + "as \"@Bot <command>\" and not otherwise.");
        } else {
            log.error("Message commands are enabled without the MESSAGE_CONTENT intent and without "
                    + "mention-prefixing, so no message command can ever fire. Enable the intent in "
                    + "the Discord developer portal and add GatewayIntent.MESSAGE_CONTENT, or turn on "
                    + "allowMentionPrefix.");
        }
    }
}
