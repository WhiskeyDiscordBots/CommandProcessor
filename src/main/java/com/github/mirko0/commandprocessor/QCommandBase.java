package com.github.mirko0.commandprocessor;

/**
 * Marks a class as a command.
 *
 * <p>A marker only — everything about the command is declared with annotations, and the handler
 * methods are found by reflection. Implementing this is what makes a class discoverable, which
 * matters most under a dependency-injection container: {@code List<QCommandBase>} then collects
 * every command bean with no registry to maintain.
 *
 * <pre>{@code
 * @CommandInfo(name = "ping", description = "Pong!")
 * public class PingCommand implements QCommandBase {
 *
 *     @CommandHandler(defaultResponse = Response.EPHEMERAL)
 *     public void execute(@CommandEvent SlashCommandInteractionEvent event) {
 *         event.getHook().editOriginal("Pong!").queue();
 *     }
 * }
 * }</pre>
 *
 * @since 0.0.1
 */
public interface QCommandBase {
}
