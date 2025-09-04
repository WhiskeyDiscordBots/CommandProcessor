# Command Processor

## Overview
Command Processor is a lightweight, annotation-driven Java library designed to streamline the creation and management of Discord slash commands for bots built with JDA (Java Discord API). It provides a clean, modular approach to defining commands, subcommands, and subcommand groups, with support for localized names and descriptions to cater to diverse user bases. The library is flexible, supporting both single JDA instances and sharded bot setups, making it ideal for Discord bot developers seeking a simple yet powerful command framework.

## Features
- **Annotation-Based Command Definition**: Define slash commands, subcommands, and groups using intuitive annotations like `@CommandInfo`, `@SubCommandHandler`, and `@CommandOption`.
- **Localization Support**: Easily add localized names and descriptions for commands, subcommands, groups, and options using the `@LocalizedEntry` annotation, supporting Discord’s locale system (e.g., English, Spanish, French).
- **Lightweight**: No external dependencies, minimizing bloat and ensuring compatibility with various project setups.

## Installation
Add Command Processor to your project by including it from your local Maven repository after publishing:

**Maven**:
```xml
<dependency>
    <groupId>com.github.mirko0</groupId>
    <artifactId>quartermaster-commands</artifactId>
    <version>0.0.2</version>
</dependency>
```
**Gradle**:
```groovy
implementation 'com.github.mirko0:quartermaster-commands:0.0.2'
```
To publish the library to your local Maven repository, run:
```bash
./gradlew publishToMavenLocal
```

## Usage
Define a command using annotations and register it with CommandProcessor:
```java
CommandProcessor processor = new CommandProcessor(jda, commands);
```

```java
@CommandInfo(name = "ping", description = "Pong!",
        groups = {
                @SubCommandGroupInfo(name = "test", description = "Test group")
        }
)
public class PingCommand implements QCommandBase {

    @Autowired
    private JDA jda;

    @CommandHandler
    public void execute(
            @CommandEvent SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        event.getHook().editOriginal("Ping: " + event.getJDA().getRestPing().complete() + "ms | Websocket: " + event.getJDA().getGatewayPing() + "ms").queue();
    }

    @SubCommandHandler(name = "pong", description = "Can you pong?")
    public void pong(@CommandEvent SlashCommandInteractionEvent event) {
        event.reply("Pong!").queue();
    }

    @Group("test")
    @SubCommandHandler(name = "groupchild", description = "This command is a child of Group \"test\"")
    public void groupchild(
            @CommandEvent SlashCommandInteractionEvent event,
            @CommandOption(type = OptionType.STRING, name = "message", description = "Message to send") String message
    ) {
        event.reply("Its fun! + " + message).queue();
    }

}
```

<img width="267" height="142" alt="image" src="https://github.com/user-attachments/assets/c0d3b210-bd7f-4990-ba08-0d7c2227f26e" />
