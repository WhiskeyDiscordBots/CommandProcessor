package com.github.mirko0.commandprocessor.message;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What a message command was given after its trigger.
 *
 * <p>A value type rather than a {@code String[]} so it can offer accessors that never throw on an
 * index nobody typed — people get commands wrong constantly, and every handler would otherwise open
 * with a length check.
 *
 * <p>Splitting is on runs of whitespace. <strong>There is no quote handling</strong>: quoting rules
 * are a rabbit hole, most commands want either tokens or the whole tail, and {@link #remainder()}
 * covers the second case exactly. Half-implemented quoting would be worse than none.
 *
 * <p>Immutable.
 *
 * @since 0.3.0
 */
public final class Arguments {

    private static final Arguments EMPTY = new Arguments(List.of(), "", null);

    private final List<String> tokens;
    private final String raw;
    private final Message message;

    private Arguments(List<String> tokens, String raw, Message message) {
        this.tokens = tokens;
        this.raw = raw;
        this.message = message;
    }

    /**
     * Splits {@code raw} into arguments.
     *
     * @param  raw     everything after the trigger, unsplit
     * @param  message the message they came from, used to resolve mentions; may be null
     * @return the arguments, never null
     */
    @NotNull
    public static Arguments of(@Nullable String raw, @Nullable Message message) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) return message == null ? EMPTY : new Arguments(List.of(), "", message);

        return new Arguments(List.of(trimmed.split("\\s+")), trimmed, message);
    }

    /** @return an empty set of arguments */
    @NotNull
    public static Arguments empty() {
        return EMPTY;
    }

    /** @return how many arguments were given */
    public int size() {
        return tokens.size();
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    /**
     * @param  index zero-based
     * @return the argument, or empty when there is none at that position
     */
    @NotNull
    public Optional<String> get(int index) {
        return index < 0 || index >= tokens.size() ? Optional.empty() : Optional.of(tokens.get(index));
    }

    /**
     * @param  index    zero-based
     * @param  fallback returned when there is no argument at that position
     * @return the argument or {@code fallback}
     */
    public String getOrDefault(int index, String fallback) {
        return get(index).orElse(fallback);
    }

    /** @return every argument, unmodifiable */
    @NotNull
    public List<String> all() {
        return tokens;
    }

    /** @return everything after the trigger, unsplit and trimmed */
    @NotNull
    public String remainder() {
        return raw;
    }

    /**
     * Everything from one argument onwards, unsplit.
     *
     * <p>For the common "say the rest of this" shape, where only the leading arguments are
     * structured.
     *
     * @param  index zero-based
     * @return the joined remainder, or empty when the index is past the end
     */
    @NotNull
    public String remainderFrom(int index) {
        if (index < 0 || index >= tokens.size()) return "";
        return String.join(" ", tokens.subList(index, tokens.size()));
    }

    /**
     * @param  index zero-based
     * @return the argument as a whole number, or empty when absent or not a number
     */
    @NotNull
    public Optional<Long> asLong(int index) {
        return get(index).flatMap(value -> {
            try {
                return Optional.of(Long.parseLong(value));
            } catch (NumberFormatException notANumber) {
                return Optional.empty();
            }
        });
    }

    /**
     * @param  index zero-based
     * @return the argument as a decimal, or empty when absent or not a number
     */
    @NotNull
    public Optional<Double> asDouble(int index) {
        return get(index).flatMap(value -> {
            try {
                return Optional.of(Double.parseDouble(value));
            } catch (NumberFormatException notANumber) {
                return Optional.empty();
            }
        });
    }

    /**
     * Resolves an argument to a user, from a mention or a raw id.
     *
     * <p>Only users the message already carries — a mention Discord resolved, or the author. The
     * dispatcher runs on an event thread and never blocks on a fetch, so an id for someone not in
     * the message is empty rather than retrieved.
     *
     * @param  index zero-based
     * @return the user, or empty
     */
    @NotNull
    public Optional<User> user(int index) {
        if (message == null) return Optional.empty();

        return get(index).flatMap(token -> {
            String id = token.replaceAll("[<@!>]", "");
            if (id.isEmpty() || !id.chars().allMatch(Character::isDigit)) return Optional.empty();

            List<User> candidates = new ArrayList<>(message.getMentions().getUsers());
            candidates.add(message.getAuthor());
            return candidates.stream().filter(user -> user.getId().equals(id)).findFirst();
        });
    }

    /**
     * Drops the first argument.
     *
     * <p>Used when a named {@code @MessageHandler} has claimed it, so the handler sees only what
     * follows its own name.
     *
     * @return the remaining arguments
     */
    @NotNull
    public Arguments skipFirst() {
        if (tokens.isEmpty()) return this;

        String remaining = remainderFrom(1);
        return new Arguments(tokens.subList(1, tokens.size()), remaining, message);
    }

    @Override
    public String toString() {
        return "Arguments" + tokens;
    }
}
