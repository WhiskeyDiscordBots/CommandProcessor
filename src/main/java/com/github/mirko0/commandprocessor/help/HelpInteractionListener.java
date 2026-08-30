package com.github.mirko0.commandprocessor.help;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Drives the buttons and select menus on a {@code /help} message.
 *
 * <p>Stateless. Everything needed to render the next screen is in the component's custom id and the
 * selected value, so a help message stays usable indefinitely and survives a restart, and nothing
 * accumulates in memory per user.
 *
 * <p>Registered automatically alongside the command dispatcher, and only when the built-in help
 * command is enabled. Components not carrying the {@code cph:} prefix are ignored, so it never
 * interferes with a consumer's own component router.
 *
 * @since 0.1.0
 */
public final class HelpInteractionListener extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(HelpInteractionListener.class);

    /** The message may be gone, or the interaction expired. Neither is worth a stack trace. */
    private static final ErrorHandler IGNORE_GONE = new ErrorHandler()
            .ignore(ErrorResponse.UNKNOWN_INTERACTION,
                    ErrorResponse.UNKNOWN_WEBHOOK,
                    ErrorResponse.UNKNOWN_MESSAGE);

    private final HelpModel model;

    public HelpInteractionListener(@NotNull HelpModel model) {
        this.model = model;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        handle(event, event.getComponentId(), List.of());
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        handle(event, event.getComponentId(), event.getValues());
    }

    /**
     * @param values the select menu's chosen values; the first supplies the target category or
     *               command, which is why a menu's own id carries no argument
     */
    private void handle(GenericComponentInteractionCreateEvent event, String rawId, List<String> values) {
        if (!HelpCustomId.isHelpComponent(rawId)) return;

        HelpCustomId decoded = HelpCustomId.decode(rawId);
        if (decoded == null) {
            // A component from an older deployment, or the disabled page counter. Acknowledge so the
            // client does not sit on "interaction failed", and change nothing.
            event.deferEdit().queue(null, IGNORE_GONE);
            return;
        }

        HelpCustomId target = values.isEmpty()
                ? decoded
                : new HelpCustomId(decoded.view(), values.get(0), 0);

        event.deferEdit().queue(null, IGNORE_GONE);
        try {
            event.getHook()
                    .editOriginalComponents(HelpRenderer.render(model.viewFor(event), target))
                    .useComponentsV2(true)
                    .queue(null, IGNORE_GONE);
        } catch (RuntimeException failure) {
            log.error("Failed to render the help view {} for user {}",
                    target, event.getUser().getId(), failure);
        }
    }
}
