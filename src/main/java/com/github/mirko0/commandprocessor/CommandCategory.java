package com.github.mirko0.commandprocessor;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Objects;

/**
 * A group of commands, as shown by the built-in help command.
 *
 * <p>Commands are assigned to a category by id with
 * {@link com.github.mirko0.commandprocessor.annotations.Category @Category}; this is where that id
 * gets a display name, a description, an emoji and a position. Registering it is optional — an
 * unregistered id still forms a category, using the id as its name — but a registered one reads far
 * better in the help menu.
 *
 * <p>Immutable. The {@code with*} methods return a new instance.
 *
 * <pre>{@code
 * CommandCategory.of("music", "Music")
 *         .withDescription("Play and control audio")
 *         .withEmoji(Emoji.fromUnicode("🎵"))
 *         .withOrder(1);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class CommandCategory {

    /** The id used for commands that declare no category. */
    public static final String DEFAULT_ID = "general";

    /** Sorts by {@link #order()}, then by display name, so a listing is always stable. */
    public static final Comparator<CommandCategory> BY_ORDER =
            Comparator.comparingInt(CommandCategory::order)
                    .thenComparing(category -> category.displayName().toLowerCase());

    private final String id;
    private final String displayName;
    private final String description;
    private final Emoji emoji;
    private final int order;

    private CommandCategory(String id, String displayName, String description, Emoji emoji, int order) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.emoji = emoji;
        this.order = order;
    }

    /**
     * Creates a category.
     *
     * @param  id          the id commands reference with {@code @Category}; matched
     *                     case-insensitively and trimmed
     * @param  displayName what the help menu shows
     * @return a new category with no description, no emoji and order {@code 0}
     * @throws NullPointerException     if either argument is null
     * @throws IllegalArgumentException if either argument is blank
     */
    @NotNull
    public static CommandCategory of(@NotNull String id, @NotNull String displayName) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        String normalisedId = normaliseId(id);
        if (normalisedId.isEmpty()) throw new IllegalArgumentException("id may not be blank");
        if (displayName.isBlank()) throw new IllegalArgumentException("displayName may not be blank");
        return new CommandCategory(normalisedId, displayName.trim(), null, null, 0);
    }

    /**
     * Normalises a raw category id: trimmed and lowercased.
     *
     * @param  id the raw id, may be null
     * @return the normalised id, or an empty string when {@code id} is null or blank
     */
    @NotNull
    public static String normaliseId(@Nullable String id) {
        return id == null ? "" : id.trim().toLowerCase();
    }

    /**
     * @param  description a sentence shown under the category in the help menu, or null for none
     * @return a copy carrying this description
     */
    @NotNull
    public CommandCategory withDescription(@Nullable String description) {
        return new CommandCategory(id, displayName, blankToNull(description), emoji, order);
    }

    /**
     * @param  emoji shown beside the category in the help menu, or null for none
     * @return a copy carrying this emoji
     */
    @NotNull
    public CommandCategory withEmoji(@Nullable Emoji emoji) {
        return new CommandCategory(id, displayName, description, emoji, order);
    }

    /**
     * @param  unicode a unicode emoji such as {@code "🎵"}, or null for none
     * @return a copy carrying this emoji
     */
    @NotNull
    public CommandCategory withEmoji(@Nullable String unicode) {
        return withEmoji(unicode == null || unicode.isBlank() ? null : Emoji.fromUnicode(unicode));
    }

    /**
     * @param  order lower sorts first; ties are broken by display name
     * @return a copy carrying this order
     */
    @NotNull
    public CommandCategory withOrder(int order) {
        return new CommandCategory(id, displayName, description, emoji, order);
    }

    /** @return the id commands reference, lowercase */
    @NotNull
    public String id() {
        return id;
    }

    /** @return the name shown in the help menu */
    @NotNull
    public String displayName() {
        return displayName;
    }

    /** @return the description shown under the category, or null */
    @Nullable
    public String description() {
        return description;
    }

    /** @return the emoji shown beside the category, or null */
    @Nullable
    public Emoji emoji() {
        return emoji;
    }

    /** @return the sort position; lower first */
    public int order() {
        return order;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CommandCategory category && id.equals(category.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "CommandCategory[" + id + " -> " + displayName + "]";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
