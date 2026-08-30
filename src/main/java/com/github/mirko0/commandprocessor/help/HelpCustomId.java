package com.github.mirko0.commandprocessor.help;

import net.dv8tion.jda.api.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The custom id carried by the help command's components: {@code cph:<view>:<arg>:<page>}.
 *
 * <p>All of the help command's state lives in these ids, so nothing is held in memory between
 * clicks. A help message left open for a week still works, and a bot restart does not strand it.
 *
 * <p>The {@code cph:} prefix is namespaced to this library. A consumer's own component router should
 * ignore anything starting with it, and this one ignores everything that does not.
 *
 * @param view the screen to render
 * @param arg  a category id or command name, depending on the view; empty when unused
 * @param page zero-based page within the view
 * @since 0.1.0
 */
public record HelpCustomId(@NotNull HelpView view, @NotNull String arg, int page) {

    /** Marks a component as belonging to the built-in help command. */
    public static final String PREFIX = "cph";

    private static final String SEPARATOR = ":";

    public HelpCustomId {
        if (view == null) throw new IllegalArgumentException("view is required");
        arg = arg == null ? "" : arg;
        if (arg.contains(SEPARATOR)) {
            throw new IllegalArgumentException("argument cannot contain '" + SEPARATOR + "': " + arg);
        }
        if (page < 0) page = 0;
    }

    /** A component id for {@code view} with no argument, on the first page. */
    @NotNull
    public static HelpCustomId of(@NotNull HelpView view) {
        return new HelpCustomId(view, "", 0);
    }

    /** A component id for {@code view} scoped to {@code arg}, on the first page. */
    @NotNull
    public static HelpCustomId of(@NotNull HelpView view, @NotNull String arg) {
        return new HelpCustomId(view, arg, 0);
    }

    /**
     * @return the encoded id
     * @throws IllegalStateException if the result would exceed Discord's limit, which turns a
     *                               silent send failure into an immediate one
     */
    @NotNull
    public String encode() {
        String encoded = PREFIX + SEPARATOR + view.code() + SEPARATOR + arg + SEPARATOR + page;
        if (encoded.length() > Button.ID_MAX_LENGTH) {
            throw new IllegalStateException("help custom id is " + encoded.length() + " characters, over the "
                    + Button.ID_MAX_LENGTH + " limit: " + encoded);
        }
        return encoded;
    }

    /**
     * Parses an id produced by {@link #encode()}.
     *
     * @param  raw the raw component id
     * @return the decoded id, or null for anything this class did not produce — including a
     *         component left on a message from an older deployment
     */
    @Nullable
    public static HelpCustomId decode(@Nullable String raw) {
        if (raw == null) return null;

        String[] parts = raw.split(SEPARATOR, -1);
        if (parts.length != 4 || !PREFIX.equals(parts[0])) return null;

        HelpView view = HelpView.fromCode(parts[1]);
        if (view == null) return null;

        int page;
        try {
            page = Integer.parseInt(parts[3]);
        } catch (NumberFormatException notANumber) {
            return null;
        }
        return new HelpCustomId(view, parts[2], page);
    }

    /** @return true when {@code raw} belongs to the help command at all */
    public static boolean isHelpComponent(@Nullable String raw) {
        return raw != null && raw.startsWith(PREFIX + SEPARATOR);
    }

    /** @return a copy of this id pointing at {@code page} */
    @NotNull
    public HelpCustomId withPage(int page) {
        return new HelpCustomId(view, arg, page);
    }
}
