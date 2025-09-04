package com.github.mirko0.commandprocessor.annotations;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandInfo {
    String name();
    LocalizedEntry[] localizedNames() default {};
    String description() default "";
    LocalizedEntry[] localizedDescriptions() default {};
    boolean nsfw() default false;
    InteractionContextType contextType() default InteractionContextType.UNKNOWN;
    IntegrationType integrationType() default IntegrationType.UNKNOWN;
    Permission[] permissions() default {};
    SubCommandGroupInfo[] groups() default {};
    String guildId() default "";

}

