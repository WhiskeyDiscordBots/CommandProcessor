package com.github.mirko0.commandprocessor.definitions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * A parsed {@link com.github.mirko0.commandprocessor.annotations.AutoCompleteHandler}: which option
 * it answers for, how narrowly it is scoped, and the method to call.
 *
 * <p>Read-only once registration has completed.
 *
 * @since 0.2.0
 */
public class AutoCompleteDefinition {

    private String optionName;
    private String subcommandName;
    private String groupName;
    private Method handlerMethod;

    /** @return the option this supplies suggestions for, lowercase */
    @NotNull
    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = normalise(optionName);
    }

    /** @return the subcommand this is restricted to, or null when it matches any */
    @Nullable
    public String getSubcommandName() {
        return subcommandName;
    }

    public void setSubcommandName(String subcommandName) {
        this.subcommandName = blankToNull(subcommandName);
    }

    /** @return the subcommand group this is restricted to, or null when it matches any */
    @Nullable
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = blankToNull(groupName);
    }

    public Method getHandlerMethod() {
        return handlerMethod;
    }

    public void setHandlerMethod(Method handlerMethod) {
        this.handlerMethod = handlerMethod;
    }

    /**
     * Whether this handler answers for the option being completed.
     *
     * @param group  the group of the invocation, or null
     * @param sub    the subcommand of the invocation, or null
     * @param option the focused option's name
     * @return true when this handler applies
     */
    public boolean matches(@Nullable String group, @Nullable String sub, @Nullable String option) {
        if (!optionName.equals(normalise(option))) return false;
        if (groupName != null && !groupName.equals(normalise(group))) return false;
        return subcommandName == null || subcommandName.equals(normalise(sub));
    }

    /**
     * How narrowly this handler is scoped; higher wins when several match.
     *
     * <p>An option name is only unique within a subcommand, so a command with several subcommands
     * sharing an option name needs the more specific handler to take precedence over the catch-all.
     *
     * @return the specificity score
     */
    public int specificity() {
        return (groupName != null ? 2 : 0) + (subcommandName != null ? 1 : 0);
    }

    private static String normalise(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        String normalised = normalise(value);
        return normalised.isEmpty() ? null : normalised;
    }

    @Override
    public String toString() {
        return "AutoCompleteDefinition["
                + (groupName == null ? "" : groupName + " ")
                + (subcommandName == null ? "*" : subcommandName) + " " + optionName + "]";
    }
}
