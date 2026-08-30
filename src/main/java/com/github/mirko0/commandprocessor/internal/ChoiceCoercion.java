package com.github.mirko0.commandprocessor.internal;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Turns whatever an autocomplete handler returned into choices Discord will accept.
 *
 * <p>A handler may return {@link Command.Choice} for full control, or plain {@link String} and
 * {@link Number} values for the common case where the label is the value. Conversion is driven by
 * the option's type, because a choice on an {@code INTEGER} option carries a number and Discord
 * rejects a string there.
 *
 * <p><strong>Internal.</strong> Not part of the public API and may change in any release.
 *
 * @since 0.2.0
 */
public final class ChoiceCoercion {

    private static final Logger log = LoggerFactory.getLogger(ChoiceCoercion.class);

    private ChoiceCoercion() {
    }

    /**
     * @param  returned what the handler returned; null or a non-collection yields no choices
     * @param  type     the focused option's type
     * @param  origin   the handler, for the log
     * @return at most {@link OptionData#MAX_CHOICES} choices, never null
     */
    public static List<Command.Choice> coerce(Object returned, OptionType type, String origin) {
        if (returned == null) return List.of();

        if (!(returned instanceof Collection<?> collection)) {
            log.error("Autocomplete handler {} returned {}; it must return a Collection of Choice, "
                            + "String or Number. No suggestions were sent.",
                    origin, returned.getClass().getName());
            return List.of();
        }

        List<Command.Choice> choices = new ArrayList<>();
        for (Object element : collection) {
            if (choices.size() == OptionData.MAX_CHOICES) {
                log.warn("Autocomplete handler {} returned {} suggestions; Discord accepts {}. "
                                + "The rest were dropped - filter on what the user has typed.",
                        origin, collection.size(), OptionData.MAX_CHOICES);
                break;
            }
            Command.Choice choice = toChoice(element, type, origin);
            if (choice != null) choices.add(choice);
        }
        return choices;
    }

    private static Command.Choice toChoice(Object element, OptionType type, String origin) {
        if (element == null) return null;
        if (element instanceof Command.Choice choice) return choice;

        String label = String.valueOf(element);
        return switch (type) {
            case INTEGER -> numeric(element, label, origin, true);
            case NUMBER -> numeric(element, label, origin, false);
            default -> text(label, origin);
        };
    }

    /**
     * A string choice, with the label trimmed and an over-long value refused.
     *
     * <p>The two limits are not the same kind of problem. A label is what the user reads, so
     * trimming it loses nothing that matters. A <em>value</em> is what Discord sends back when they
     * pick it — truncating that hands the command an identifier that resolves to nothing, so the
     * suggestion is dropped instead.
     *
     * <p>Both are built before the {@link Command.Choice} rather than after: JDA validates in the
     * constructor, so an over-long label thrown there would take the whole response down.
     */
    private static Command.Choice text(String label, String origin) {
        if (label.length() > OptionData.MAX_CHOICE_VALUE_LENGTH) {
            log.warn("Autocomplete handler {} returned a {}-character suggestion; Discord allows {} "
                            + "for a value. It was dropped rather than sent truncated, which would "
                            + "resolve to nothing when picked.",
                    origin, label.length(), OptionData.MAX_CHOICE_VALUE_LENGTH);
            return null;
        }
        return new Command.Choice(trimLabel(label, origin), label);
    }

    /**
     * A numeric choice, because Discord rejects a string value on an INTEGER or NUMBER option.
     *
     * <p>A handler returning strings for a numeric option is the ordinary case — the values usually
     * come from a database as text — so they are parsed rather than refused.
     */
    private static Command.Choice numeric(Object element, String label, String origin, boolean asLong) {
        try {
            String name = trimLabel(label, origin);
            if (asLong) {
                long value = element instanceof Number number ? number.longValue() : Long.parseLong(label.trim());
                return new Command.Choice(name, value);
            }
            double value = element instanceof Number number ? number.doubleValue() : Double.parseDouble(label.trim());
            return new Command.Choice(name, value);
        } catch (NumberFormatException notANumber) {
            log.warn("Autocomplete handler {} returned \"{}\" for a numeric option; it is not a number "
                    + "and was dropped.", origin, label);
            return null;
        }
    }

    /** A label Discord will accept. Cosmetic, so it is trimmed rather than refused. */
    private static String trimLabel(String label, String origin) {
        if (label.length() <= OptionData.MAX_CHOICE_NAME_LENGTH) return label;

        log.warn("Autocomplete handler {} returned a label of {} characters; Discord allows {}. "
                        + "It was trimmed.",
                origin, label.length(), OptionData.MAX_CHOICE_NAME_LENGTH);
        return label.substring(0, OptionData.MAX_CHOICE_NAME_LENGTH);
    }
}
