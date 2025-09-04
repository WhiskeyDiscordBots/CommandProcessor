package com.github.mirko0.commandprocessor;

import com.github.mirko0.commandprocessor.annotations.LocalizedEntry;
import com.github.mirko0.commandprocessor.enumeration.Response;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class CommandDefinition {
    private String name;
    private LocalizedEntry[] localizedNames;
    private String description;
    private LocalizedEntry[] localizedDescriptions;
    private QCommandBase instance;
    private CommandData commandData;
    private Method handlerMethod;
    private Response defaultResponse;
    private List<SubcommandDefinition> subcommands;
    private Map<String, SubcommandGroupDefinition> groups;
    private String guildId;
    private boolean nsfw;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalizedEntry[] getLocalizedNames() {
        return localizedNames;
    }

    public void setLocalizedNames(LocalizedEntry[] localizedNames) {
        this.localizedNames = localizedNames;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalizedEntry[] getLocalizedDescriptions() {
        return localizedDescriptions;
    }

    public void setLocalizedDescriptions(LocalizedEntry[] localizedDescriptions) {
        this.localizedDescriptions = localizedDescriptions;
    }

    public QCommandBase getInstance() {
        return instance;
    }

    public void setInstance(QCommandBase instance) {
        this.instance = instance;
    }

    public CommandData getCommandData() {
        return commandData;
    }

    public void setCommandData(CommandData commandData) {
        this.commandData = commandData;
    }

    public Method getHandlerMethod() {
        return handlerMethod;
    }

    public void setHandlerMethod(Method handlerMethod) {
        this.handlerMethod = handlerMethod;
    }

    public Response getDefaultResponse() {
        return defaultResponse;
    }

    public void setDefaultResponse(Response defaultResponse) {
        this.defaultResponse = defaultResponse;
    }

    public List<SubcommandDefinition> getSubcommands() {
        return subcommands;
    }

    public void setSubcommands(List<SubcommandDefinition> subcommands) {
        this.subcommands = subcommands;
    }

    public Map<String, SubcommandGroupDefinition> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, SubcommandGroupDefinition> groups) {
        this.groups = groups;
    }
    public String getGuildId() {
        return guildId;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    public void setNSFW(boolean nsfw) {
        this.nsfw = nsfw;
    }
}

class SubcommandDefinition {
    private String name;
    private String groupName;
    private Method handlerMethod;
    private Response defaultResponse;
    private LocalizedEntry[] localizedNames;
    private String description;
    private LocalizedEntry[] localizedDescriptions;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Method getHandlerMethod() {
        return handlerMethod;
    }

    public void setHandlerMethod(Method handlerMethod) {
        this.handlerMethod = handlerMethod;
    }

    public Response getDefaultResponse() {
        return defaultResponse;
    }

    public void setDefaultResponse(Response defaultResponse) {
        this.defaultResponse = defaultResponse;
    }

    public LocalizedEntry[] getLocalizedNames() {
        return localizedNames;
    }

    public void setLocalizedNames(LocalizedEntry[] localizedNames) {
        this.localizedNames = localizedNames;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalizedEntry[] getLocalizedDescriptions() {
        return localizedDescriptions;
    }

    public void setLocalizedDescriptions(LocalizedEntry[] localizedDescriptions) {
        this.localizedDescriptions = localizedDescriptions;
    }
}

class SubcommandGroupDefinition {
    private String name;
    private LocalizedEntry[] localizedNames;
    private String description;
    private LocalizedEntry[] localizedDescriptions;
    private List<SubcommandDefinition> subcommands;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SubcommandDefinition> getSubcommands() {
        return subcommands;
    }

    public void setSubcommands(List<SubcommandDefinition> subcommands) {
        this.subcommands = subcommands;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalizedEntry[] getLocalizedDescriptions() {
        return localizedDescriptions;
    }

    public void setLocalizedDescriptions(LocalizedEntry[] localizedDescriptions) {
        this.localizedDescriptions = localizedDescriptions;
    }

    public LocalizedEntry[] getLocalizedNames() {
        return localizedNames;
    }

    public void setLocalizedNames(LocalizedEntry[] localizedNames) {
        this.localizedNames = localizedNames;
    }
}