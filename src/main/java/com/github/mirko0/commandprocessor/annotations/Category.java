package com.github.mirko0.commandprocessor.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Groups a command under a category for the built-in help command.
 *
 * <p>The value is a category <em>id</em>, not a display name:
 *
 * <pre>{@code
 * @Category("music")
 * @CommandInfo(name = "play", description = "Play a track.")
 * public class PlayCommand implements QCommandBase { ... }
 * }</pre>
 *
 * <p>What the id means — its display name, description, emoji and position — is registered once,
 * with the processor, so the same category can be described in one place rather than repeated on
 * every command that belongs to it:
 *
 * <pre>{@code
 * new CommandProcessor(commands, true, List.of(
 *         CommandCategory.of("music", "Music").withEmoji("🎵").withOrder(1),
 *         CommandCategory.of("moderation", "Moderation").withOrder(2)));
 * }</pre>
 *
 * <p>An id that was never registered still works: a category is created for it on the fly, using
 * the id as its display name, and a warning names the command so the gap is visible. A command with
 * no {@code @Category} falls into {@link com.github.mirko0.commandprocessor.CommandCategory#DEFAULT_ID}.
 *
 * <p>Ids are matched case-insensitively and are trimmed of surrounding whitespace.
 *
 * @see com.github.mirko0.commandprocessor.CommandCategory
 * @since 0.1.0
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Category {

    /**
     * The category id this command belongs to.
     *
     * @return the category id; blank is treated as absent
     */
    String value();
}
