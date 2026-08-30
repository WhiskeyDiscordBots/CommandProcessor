package com.github.mirko0.commandprocessor.annotations;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a Discord slash command. Applied to a class implementing
 * {@link com.github.mirko0.commandprocessor.QCommandBase}.
 *
 * <p>The class is scanned for a {@link CommandHandler} method, or for {@link SubCommandHandler}
 * methods when the command has subcommands, and the corresponding
 * {@link net.dv8tion.jda.api.interactions.commands.build.SlashCommandData} is built from what is
 * declared here.
 *
 * @since 0.0.1
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandInfo {

    /**
     * The name of the command as it will appear in Discord.
     *
     * <p>Lowercased, and trimmed to Discord's limit of 32 characters with a warning if it is longer.
     *
     * @return The command name
     */
    String name();

    /**
     * Alternative names for this command.
     *
     * <p><strong>Not registered with Discord</strong> — a slash command has exactly one name and
     * there is no alias concept in the API. These are used by the built-in help command so
     * {@code /help ping} also finds a command that used to be called {@code latency}, and by the
     * message-command dispatcher when that feature is enabled.
     *
     * @return additional names this command answers to
     * @since  0.1.0
     */
    String[] aliases() default {};

    /**
     * Localized names for the command in different languages.
     *
     * @return Array of localized command names
     */
    LocalizedEntry[] localizedNames() default {};

    /**
     * The description of the command that will be shown in Discord.
     *
     * <p>Required by Discord. A blank description is replaced with a placeholder and logged as an
     * error rather than aborting registration; over-long ones are trimmed to 100 characters with a
     * warning.
     *
     * @return The command description
     */
    String description() default "";

    /**
     * Localized descriptions for the command in different languages.
     *
     * @return Array of localized command descriptions
     */
    LocalizedEntry[] localizedDescriptions() default {};

    /**
     * Indicates whether the command contains age-restricted content.
     *
     * @return true if the command is NSFW, false otherwise
     */
    boolean nsfw() default false;

    /**
     * The contexts in which the command can be used.
     *
     * <p>Empty leaves JDA's default in place. A command is routinely valid in more than one context,
     * for example {@code {GUILD, BOT_DM}}.
     *
     * @return the interaction contexts
     * @since  0.1.0
     */
    InteractionContextType[] contexts() default {};

    /**
     * Specifies the context in which the command can be used.
     *
     * @return The interaction context type
     * @deprecated since 0.1.0, use {@link #contexts()} — a command is often valid in more than one
     *             context and this member can only carry one. Honoured only while
     *             {@code contexts()} is empty. Removal is planned for 1.0.0.
     */
    @Deprecated(since = "0.1.0", forRemoval = true)
    InteractionContextType contextType() default InteractionContextType.UNKNOWN;

    /**
     * How the command may be installed.
     *
     * <p>Empty leaves JDA's default in place.
     *
     * @return the integration types
     * @since  0.1.0
     */
    IntegrationType[] integrationTypes() default {};

    /**
     * Defines the integration type for the command.
     *
     * @return The integration type
     * @deprecated since 0.1.0, use {@link #integrationTypes()} — a command can support more than one
     *             installation type and this member can only carry one. Honoured only while
     *             {@code integrationTypes()} is empty. Removal is planned for 1.0.0.
     */
    @Deprecated(since = "0.1.0", forRemoval = true)
    IntegrationType integrationType() default IntegrationType.UNKNOWN;

    /**
     * Permissions a member must hold to use the command.
     *
     * <p>Applied to Discord as the command's default member permissions, <em>and</em> used by the
     * built-in help command to decide whether to list the command for the member asking. Discord
     * enforces the former; nothing enforces it inside the handler, so a command that must not run
     * without a permission still checks it itself.
     *
     * @return Array of required permissions
     */
    Permission[] permissions() default {};

    /**
     * Subcommand groups registered to this command.
     *
     * @return Array of subcommand group information
     */
    SubCommandGroupInfo[] groups() default {};

    /**
     * The ID of the guild where the command should be registered.
     * If empty, the command will be registered globally.
     *
     * <p>Guild scoping controls where a command is <em>registered</em>, not who may use it: every
     * member of that guild can see it. Anything owner-only checks the caller as well.
     *
     * @return The guild ID or an empty string for global commands
     */
    String guildId() default "";

    /**
     * The ID of the module this command belongs to.
     *
     * @return The module ID
     * @deprecated since 0.1.0, use {@link Category @Category} — categories carry a display name, a
     *             description, an emoji and an order, registered in one place, where this was a
     *             bare string with no meaning attached. Used as the category id when no
     *             {@code @Category} is present. Removal is planned for 1.0.0.
     */
    @Deprecated(since = "0.1.0", forRemoval = true)
    String moduleId() default "";
}
