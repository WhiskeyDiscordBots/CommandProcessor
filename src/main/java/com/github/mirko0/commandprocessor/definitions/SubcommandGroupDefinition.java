package com.github.mirko0.commandprocessor.definitions;

import com.github.mirko0.commandprocessor.annotations.LocalizedEntry;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A parsed subcommand group.
 *
 * <p>Read-only once registration has completed; the setters exist for the parser.
 *
 * @since 0.0.1
 */
public class SubcommandGroupDefinition {
    private String name;
    private LocalizedEntry[] localizedNames;
    private String description;
    private LocalizedEntry[] localizedDescriptions;
    private List<SubcommandDefinition> subcommands = new ArrayList<>();

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** @return the subcommands in this group, in listing order */
    @NotNull
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

    @Override
    public String toString() {
        return "SubcommandGroupDefinition[" + name + " x" + subcommands.size() + "]";
    }
}
