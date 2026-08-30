package com.github.mirko0.commandprocessor.definitions;

import com.github.mirko0.commandprocessor.annotations.LocalizedEntry;
import com.github.mirko0.commandprocessor.enumeration.Response;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

/**
 * A parsed subcommand, grouped or not.
 *
 * <p>Read-only once registration has completed; the setters exist for the parser.
 *
 * @since 0.0.1
 */
public class SubcommandDefinition {
    private String name;
    private String groupName;
    private Method handlerMethod;
    private Response defaultResponse;
    private LocalizedEntry[] localizedNames;
    private String description;
    private LocalizedEntry[] localizedDescriptions;
    private List<String> optionNames = List.of();

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The group this subcommand belongs to.
     *
     * @return the lowercase group name, or null when the subcommand is not grouped
     */
    @Nullable
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

    /**
     * The names of this subcommand's options, in the order they are declared.
     *
     * <p>Used by the help command to show a usage line without re-reflecting over the handler.
     *
     * @return an unmodifiable list, empty when the subcommand takes no options
     * @since  0.1.0
     */
    @NotNull
    public List<String> getOptionNames() {
        return optionNames;
    }

    public void setOptionNames(List<String> optionNames) {
        this.optionNames = optionNames == null ? List.of() : List.copyOf(optionNames);
    }

    @Override
    public String toString() {
        return "SubcommandDefinition[" + (groupName == null ? "" : groupName + " ") + name + "]";
    }
}
