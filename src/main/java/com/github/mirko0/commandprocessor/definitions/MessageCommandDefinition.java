package com.github.mirko0.commandprocessor.definitions;

import com.github.mirko0.commandprocessor.QCommandBase;
import com.github.mirko0.commandprocessor.message.PrefixMode;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed {@link com.github.mirko0.commandprocessor.annotations.MessageCommandInfo}: its triggers,
 * how it may be invoked, and the handlers to call.
 *
 * <p>Read-only once registration has completed.
 *
 * @since 0.3.0
 */
public class MessageCommandDefinition {

    private String name;
    private List<String> triggers = List.of();
    private PrefixMode prefixMode = PrefixMode.REQUIRED;
    private boolean guildOnly = true;
    private boolean deleteTrigger;
    private EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
    private QCommandBase instance;
    private Method baseHandler;
    private Map<String, Method> namedHandlers = Map.of();

    /** @return the canonical trigger, used in logs and error messages */
    @NotNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** @return every trigger, canonical first; unmodifiable */
    @NotNull
    public List<String> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<String> triggers) {
        this.triggers = triggers == null ? List.of() : List.copyOf(triggers);
    }

    @NotNull
    public PrefixMode getPrefixMode() {
        return prefixMode;
    }

    public void setPrefixMode(PrefixMode prefixMode) {
        this.prefixMode = prefixMode == null ? PrefixMode.REQUIRED : prefixMode;
    }

    public boolean isGuildOnly() {
        return guildOnly;
    }

    public void setGuildOnly(boolean guildOnly) {
        this.guildOnly = guildOnly;
    }

    public boolean isDeleteTrigger() {
        return deleteTrigger;
    }

    public void setDeleteTrigger(boolean deleteTrigger) {
        this.deleteTrigger = deleteTrigger;
    }

    /**
     * Permissions the invoking member must hold.
     *
     * <p>Unlike a slash command's, these are enforced here — Discord has no gate on someone typing
     * a word in a channel.
     *
     * @return an unmodifiable set, empty when the command is open to everyone
     */
    @NotNull
    public EnumSet<Permission> getPermissions() {
        return EnumSet.copyOf(permissions);
    }

    public void setPermissions(Permission[] permissions) {
        this.permissions = permissions == null || permissions.length == 0
                ? EnumSet.noneOf(Permission.class)
                : EnumSet.copyOf(Arrays.asList(permissions));
    }

    public QCommandBase getInstance() {
        return instance;
    }

    public void setInstance(QCommandBase instance) {
        this.instance = instance;
    }

    /** @return the unnamed handler, or null when every handler is named */
    @Nullable
    public Method getBaseHandler() {
        return baseHandler;
    }

    public void setBaseHandler(Method baseHandler) {
        this.baseHandler = baseHandler;
    }

    /** @return handlers keyed by the first argument they claim; unmodifiable */
    @NotNull
    public Map<String, Method> getNamedHandlers() {
        return namedHandlers;
    }

    public void setNamedHandlers(Map<String, Method> namedHandlers) {
        this.namedHandlers = namedHandlers == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(namedHandlers));
    }

    /**
     * Whether this command answers at all.
     *
     * @return false when the class declared triggers but no {@code @MessageHandler}
     */
    public boolean hasHandler() {
        return baseHandler != null || !namedHandlers.isEmpty();
    }

    @Override
    public String toString() {
        return "MessageCommandDefinition[" + name + " " + prefixMode + " " + triggers + "]";
    }
}
