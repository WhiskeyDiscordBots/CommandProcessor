package com.github.mirko0.commandprocessor.annotations;

import net.dv8tion.jda.api.interactions.DiscordLocale;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
public @interface LocalizedEntry {
    DiscordLocale locale();
    String value();
}