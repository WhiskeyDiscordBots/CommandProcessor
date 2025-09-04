package com.github.mirko0.commandprocessor;

import com.github.mirko0.commandprocessor.annotations.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Processes and manages Discord slash commands for a Discord bot.
 * Handles command registration, parsing, and management for both global and guild-specific commands.
 */
public class CommandProcessor {
    private static final Logger log = LoggerFactory.getLogger(CommandProcessor.class);
    private static final int MAX_REGISTRATION_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    private final ShardManager shardManager;
    private final JDA jda;
    private final Map<String, CommandDefinition> commandMap;
    private final SlashCommandHandler slashCommandHandler;

    /**
     * Creates a CommandProcessor for a sharded bot.
     *
     * @param shardManager ShardManager instance
     * @param commands     List of commands to register
     */
    public CommandProcessor(ShardManager shardManager, List<QCommandBase> commands) {
        this(commands, null, shardManager);
    }

    /**
     * Creates a CommandProcessor for a non-sharded bot.
     *
     * @param jda      JDA instance
     * @param commands List of commands to register
     */
    public CommandProcessor(JDA jda, List<QCommandBase> commands) {
        this(commands, jda, null);
    }

    /**
     * Main constructor for CommandProcessor.
     *
     * @param commands     List of commands to register
     * @param jda          JDA instance (null if using ShardManager)
     * @param shardManager ShardManager instance (null if using single JDA)
     */
    public CommandProcessor(List<QCommandBase> commands, JDA jda, ShardManager shardManager) {
        this.shardManager = shardManager;
        this.jda = jda;
        this.commandMap = new HashMap<>();
        this.slashCommandHandler = new SlashCommandHandler(commandMap);
        register(commands);
        if (shardManager != null) shardManager.addEventListener(slashCommandHandler);
        else jda.addEventListener(slashCommandHandler);
        log.info("Command processor initialized with {} commands", commandMap.size());
    }

    /**
     * Registers global and guild-specific commands.
     */
    private void register(List<QCommandBase> commands) {
        Map<String, Set<CommandData>> guildCommands = new HashMap<>();
        Set<CommandData> globalCommands = new HashSet<>();

        for (QCommandBase command : commands) {
            CommandDefinition cmdDef = parseCommand(command);
            if (cmdDef == null) continue;

            commandMap.put(cmdDef.getName(), cmdDef);
            if (cmdDef.getGuildId() == null || cmdDef.getGuildId().isEmpty()) {
                globalCommands.add(cmdDef.getCommandData());
            }else {
                guildCommands.computeIfAbsent(cmdDef.getGuildId(), k -> new HashSet<>()).add(cmdDef.getCommandData());
            }
        }

        registerGlobalCommands(globalCommands);
        registerGuildCommands(guildCommands);
    }

    /**
     * Parses command class and creates a CommandDefinition object.
     */
    private CommandDefinition parseCommand(QCommandBase command) {
        Class<?> clazz = command.getClass();
        CommandInfo info = clazz.getAnnotation(CommandInfo.class);
        if (info == null) return null;

        CommandDefinition cmdDef = new CommandDefinition();
        cmdDef.setName(info.name().toLowerCase());
        cmdDef.setInstance(command);
        cmdDef.setGuildId(info.guildId());
        cmdDef.setNSFW(info.nsfw());
        cmdDef.setLocalizedNames(info.localizedNames());
        cmdDef.setLocalizedDescriptions(info.localizedDescriptions());
        cmdDef.setDescription(info.description());

        SlashCommandData data = Commands.slash(
                info.name().toLowerCase().replaceAll("[^a-z0-9-]", "").substring(0, Math.min(32, info.name().length())),
                info.description().substring(0, Math.min(100, info.description().length()))
        );
        data.setNSFW(info.nsfw());

        if (info.localizedNames() != null && info.localizedNames().length > 0) data.setNameLocalizations(ProcessorUtility.getLocalizedEntries(info.localizedNames()));
        if (info.localizedDescriptions() != null && info.localizedDescriptions().length > 0) data.setDescriptionLocalizations(ProcessorUtility.getLocalizedEntries(info.localizedDescriptions()));
        if (info.contextType() != InteractionContextType.UNKNOWN) data.setContexts(info.contextType());
        if (info.integrationType() != IntegrationType.UNKNOWN) data.setIntegrationTypes(info.integrationType());
        if (info.permissions().length > 0) {
            data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(info.permissions()));
        }
        cmdDef.setCommandData(data);

        Map<String, SubcommandGroupDefinition> groupDefMap = parseSubcommandGroups(info);
        List<SubcommandDefinition> subcommands = new ArrayList<>();
        OptionData[] defOptions = null;
        boolean hadSubCommands = false;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(CommandHandler.class)) {
                defOptions = parseOptions(method);
                cmdDef.setHandlerMethod(method);
                cmdDef.setDefaultResponse(method.getAnnotation(CommandHandler.class).defaultResponse());
            }

            SubCommandHandler subAnno = method.getAnnotation(SubCommandHandler.class);
            if (subAnno != null) {
                hadSubCommands = true;
                SubcommandDefinition subDef = new SubcommandDefinition();
                subDef.setName(subAnno.name().toLowerCase());
                subDef.setDescription(subAnno.description());
                subDef.setLocalizedNames(subAnno.localizedNames());
                subDef.setLocalizedDescriptions(subAnno.localizedDescriptions());
                subDef.setHandlerMethod(method);
                subDef.setDefaultResponse(subAnno.defaultResponse());

                OptionData[] opts = parseOptions(method);
                SubcommandData subData = new SubcommandData(subDef.getName(), subAnno.description()).addOptions(opts);
                if (subAnno.localizedNames() != null && subAnno.localizedNames().length > 0) subData.setNameLocalizations(ProcessorUtility.getLocalizedEntries(subAnno.localizedNames()));
                if (subAnno.localizedDescriptions() != null && subAnno.localizedDescriptions().length > 0) subData.setDescriptionLocalizations(ProcessorUtility.getLocalizedEntries(subAnno.localizedDescriptions()));

                Group groupAnno = method.getAnnotation(Group.class);
                if (groupAnno != null) {
                    String groupName = groupAnno.value().toLowerCase();
                    SubcommandGroupDefinition groupDef = groupDefMap.get(groupName);
                    if (groupDef != null) {
                        groupDef.getSubcommands().add(subDef);
                        SubcommandGroupData subGroupData = new SubcommandGroupData(groupName, groupDefMap.get(groupName).getDescription());
                        subGroupData.addSubcommands(subData);
                        if (groupDef.getLocalizedNames() != null && groupDef.getLocalizedNames().length > 0) subGroupData.setNameLocalizations(ProcessorUtility.getLocalizedEntries(groupDef.getLocalizedNames()));
                        if (groupDef.getLocalizedDescriptions() != null && groupDef.getLocalizedDescriptions().length > 0) subGroupData.setDescriptionLocalizations(ProcessorUtility.getLocalizedEntries(groupDef.getLocalizedDescriptions()));
                        data.addSubcommandGroups(subGroupData);
                    }
                }else {
                    subcommands.add(subDef);
                    data.addSubcommands(subData);
                }
            }
        }

        cmdDef.setSubcommands(subcommands);
        cmdDef.setGroups(groupDefMap);
        if (!hadSubCommands && defOptions != null) {
            data.addOptions(defOptions);
        }

        return cmdDef;
    }

    /**
     * Parses subcommand groups from command info.
     */
    private Map<String, SubcommandGroupDefinition> parseSubcommandGroups(CommandInfo info) {
        Map<String, SubcommandGroupDefinition> groupDefMap = new HashMap<>();
        for (SubCommandGroupInfo groupInfo : info.groups()) {
            SubcommandGroupDefinition groupDef = new SubcommandGroupDefinition();
            groupDef.setLocalizedNames(groupInfo.localizedNames());
            groupDef.setLocalizedDescriptions(groupInfo.localizedDescriptions());
            groupDef.setName(groupInfo.name().toLowerCase());
            groupDef.setDescription(groupInfo.description());
            groupDef.setSubcommands(new ArrayList<>());
            groupDefMap.put(groupInfo.name().toLowerCase(), groupDef);
        }
        return groupDefMap;
    }

    /**
     * Registers global commands with retries for each shard.
     */
    private void registerGlobalCommands(Set<CommandData> globalCommands) {
        if (shardManager != null) {
            for (JDA shard : shardManager.getShards()) {
                registerGlobalCommandsForShard(shard, globalCommands, 0);
            }
        }else {
            log.info("Registering {} global commands", globalCommands.size());
            jda.updateCommands().addCommands(globalCommands).queue();
        }
    }

    /**
     * Attempts to register global commands for a specific shard with retries.
     */
    private void registerGlobalCommandsForShard(JDA shard, Set<CommandData> globalCommands, int attempt) {
        if (attempt >= MAX_REGISTRATION_RETRIES) {
            log.error("Failed to register global commands for shard {} after {} attempts",
                    shard.getShardInfo().getShardId(), MAX_REGISTRATION_RETRIES);
            return;
        }

        if (shard.getStatus() == JDA.Status.CONNECTED || shard.getStatus().ordinal() >= JDA.Status.CONNECTED.ordinal()) {
            log.info("Registering {} global commands for shard {}", globalCommands.size(), shard.getShardInfo().getShardId());
            shard.updateCommands().addCommands(globalCommands).queue();
        }else {
            log.warn("Shard {} is not ready (status: {}), retry attempt {}/{}",
                    shard.getShardInfo().getShardId(), shard.getStatus(), attempt + 1, MAX_REGISTRATION_RETRIES);

            // Schedule retry
            shard.getGatewayPool().schedule(() ->
                            registerGlobalCommandsForShard(shard, globalCommands, attempt + 1),
                    RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Registers guild-specific commands.
     */
    private void registerGuildCommands(Map<String, Set<CommandData>> guildCommands) {
        guildCommands.forEach((guildId, commandsData) -> {
            Guild guild = shardManager != null ? shardManager.getGuildById(guildId) : jda.getGuildById(guildId);
            if (guild == null) {
                log.warn("Guild {} not found, cannot register commands", guildId);
                return;
            }

            registerGuildCommandsWithRetry(guild, commandsData, 0);
        });
    }

    /**
     * Attempts to register guild commands with retries.
     */
    private void registerGuildCommandsWithRetry(Guild guild, Set<CommandData> commandsData, int attempt) {
        if (attempt >= MAX_REGISTRATION_RETRIES) {
            log.error("Failed to register commands for guild {} after {} attempts",
                    guild.getId(), MAX_REGISTRATION_RETRIES);
            return;
        }

        if (shardManager != null && guild.getJDA().getStatus().ordinal() < JDA.Status.CONNECTED.ordinal()) {
            log.warn("Shard for guild {} is not ready (status: {}), retry attempt {}/{}",
                    guild.getId(), guild.getJDA().getStatus(), attempt + 1, MAX_REGISTRATION_RETRIES);

            // Schedule retry
            guild.getJDA().getGatewayPool().schedule(() ->
                            registerGuildCommandsWithRetry(guild, commandsData, attempt + 1),
                    RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
            return;
        }

        log.info("Registering {} commands for guild {} ({})", commandsData.size(), guild.getName(), guild.getId());
        guild.updateCommands().addCommands(commandsData).queue();
    }

    /**
     * Parses command options from method parameters.
     */
    private OptionData[] parseOptions(Method method) {
        return Arrays.stream(method.getParameters())
                .filter(p -> p.isAnnotationPresent(CommandOption.class))
                .map(p -> {
                    CommandOption anno = p.getAnnotation(CommandOption.class);
                    OptionData optionData = new OptionData(
                            anno.type(),
                            anno.name().toLowerCase(),
                            anno.description(),
                            anno.required(),
                            anno.autoComplete()
                    );
                    if (anno.localizedNames() != null && anno.localizedNames().length > 0) optionData.setNameLocalizations(ProcessorUtility.getLocalizedEntries(anno.localizedNames()));
                    if (anno.localizedDescriptions() != null && anno.localizedDescriptions().length > 0) optionData.setDescriptionLocalizations(ProcessorUtility.getLocalizedEntries(anno.localizedDescriptions()));
                    if (anno.choices().length > 0) {
                        optionData.addChoices(parseChoices(anno.choices()));
                    }
                    return optionData;
                })
                .toArray(OptionData[]::new);
    }

    /**
     * Parses choice options for command arguments.
     */
    private Command.Choice[] parseChoices(String[] choiceStrings) {
        return Arrays.stream(choiceStrings)
                .map(s -> {
                    String[] parts = s.split(":");
                    if (parts.length == 2) {
                        return new Command.Choice(parts[1], parts[0]);
                    }else {
                        return new Command.Choice(parts[0], parts[0]);
                    }
                })
                .toArray(Command.Choice[]::new);
    }
}