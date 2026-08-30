package com.github.mirko0.commandprocessor.internal;

import com.github.mirko0.commandprocessor.CommandCategory;
import com.github.mirko0.commandprocessor.definitions.CommandDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The categories a set of commands falls into.
 *
 * <p>Built once, after parsing, from the categories the consumer registered plus the ids commands
 * actually used. An id nobody registered still becomes a category — using the id as its display
 * name — because a command silently vanishing from the help menu is worse than an ugly heading.
 *
 * <p><strong>Internal.</strong> Not part of the public API and may change in any release.
 */
public final class CategoryRegistry {

    private static final Logger log = LoggerFactory.getLogger(CategoryRegistry.class);

    private final Map<String, CommandCategory> byId;

    private CategoryRegistry(Map<String, CommandCategory> byId) {
        this.byId = byId;
    }

    /**
     * @param declared the categories the consumer registered, may be null or empty
     * @param commands every parsed command, used to discover ids that were never registered
     */
    public static CategoryRegistry of(Collection<CommandCategory> declared,
                                      Collection<CommandDefinition> commands) {
        Map<String, CommandCategory> byId = new LinkedHashMap<>();
        if (declared != null) {
            for (CommandCategory category : declared) {
                if (category == null) continue;
                CommandCategory previous = byId.put(category.id(), category);
                if (previous != null) {
                    log.warn("Two categories share the id '{}' ('{}' and '{}'). Keeping the second.",
                            category.id(), previous.displayName(), category.displayName());
                }
            }
        }

        List<String> undeclared = new ArrayList<>();
        for (CommandDefinition command : commands) {
            String id = command.getCategoryId();
            if (byId.containsKey(id)) continue;

            byId.put(id, CommandCategory.of(id, defaultDisplayName(id)));
            if (!CommandCategory.DEFAULT_ID.equals(id)) undeclared.add(id + " (/" + command.getName() + ")");
        }

        if (!undeclared.isEmpty()) {
            log.warn("Categories used by commands but never registered: {}. They will show in /help "
                            + "with their id as the name; register them with CommandCategory.of(...) to give "
                            + "them a display name, description, emoji and order.",
                    String.join(", ", undeclared));
        }

        return new CategoryRegistry(byId);
    }

    /** Never null: an unknown id is created on demand rather than dropping the command. */
    public CommandCategory get(String id) {
        String normalised = CommandCategory.normaliseId(id);
        return byId.computeIfAbsent(
                normalised.isEmpty() ? CommandCategory.DEFAULT_ID : normalised,
                key -> CommandCategory.of(key, defaultDisplayName(key)));
    }

    /** Every category, ordered by {@link CommandCategory#BY_ORDER}. */
    public List<CommandCategory> all() {
        List<CommandCategory> all = new ArrayList<>(byId.values());
        all.sort(CommandCategory.BY_ORDER);
        return all;
    }

    /** {@code "voice-moderation"} becomes {@code "Voice Moderation"}. */
    private static String defaultDisplayName(String id) {
        StringBuilder name = new StringBuilder(id.length());
        boolean capitalise = true;
        for (char character : id.toCharArray()) {
            if (character == '-' || character == '_') {
                name.append(' ');
                capitalise = true;
                continue;
            }
            name.append(capitalise ? Character.toUpperCase(character) : character);
            capitalise = false;
        }
        return name.toString();
    }
}
