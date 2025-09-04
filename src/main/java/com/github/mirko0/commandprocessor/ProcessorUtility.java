package com.github.mirko0.commandprocessor;

import com.github.mirko0.commandprocessor.annotations.LocalizedEntry;
import net.dv8tion.jda.api.interactions.DiscordLocale;

import java.util.HashMap;
import java.util.Map;

public class ProcessorUtility {

    public static Map<DiscordLocale, String> getLocalizedEntries(LocalizedEntry[] entries) {
        Map<DiscordLocale, String> map = new HashMap<>();
        for (LocalizedEntry entry : entries) {
            map.put(entry.locale(), entry.value());
        }
        return map;
    }
}
