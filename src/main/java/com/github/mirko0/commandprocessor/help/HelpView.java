package com.github.mirko0.commandprocessor.help;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The screens the help command can show.
 *
 * <p>Codes are one character because they travel inside a custom id alongside a command name, and
 * Discord caps that id at 100 characters.
 *
 * @since 0.1.0
 */
public enum HelpView {

    /** The category overview: every category the caller can see, with counts. */
    ROOT("r"),

    /** One category: its commands, paged. */
    CATEGORY("c"),

    /** One command: description, aliases, permissions, subcommands and options. */
    COMMAND("d");

    private final String code;

    HelpView(String code) {
        this.code = code;
    }

    /** @return the short code used inside a custom id */
    @NotNull
    public String code() {
        return code;
    }

    /**
     * @param  code a code produced by {@link #code()}
     * @return the matching view, or null for an unrecognised code — a component from an older
     *         deployment must fail politely rather than crash the router
     */
    @Nullable
    public static HelpView fromCode(@Nullable String code) {
        if (code == null) return null;
        for (HelpView view : values()) {
            if (view.code.equals(code)) return view;
        }
        return null;
    }
}
