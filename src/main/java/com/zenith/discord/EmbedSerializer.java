package com.zenith.discord;

import discord4j.core.spec.EmbedCreateFields;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class EmbedSerializer {
    private static final Pattern DISCORD_TIMESTAMP_PATTERN = Pattern.compile("<t:(\\d+):.>");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Component serialize(final Embed embed) {
        Component c = Component.newline();
        if (embed.isColorPresent()) {
            var color = embed.color();
            c = c.applyFallbackStyle(Style.style(TextColor.color(
                color.getRed(),
                color.getGreen(),
                color.getBlue())));
        }
        // todo: handle discord formatted bold, italicized, or underlined text
        c = c.append(Component.text(embed.title()));
        if (embed.isDescriptionPresent()) {
            c = c
                .appendNewline()
                .append(Component.text(replaceDiscordTime(embed.description())));
        }
        if (embed.isUrlPresent()) {
            c = c
                .appendNewline()
                .append(Component.text(embed.url()));
        }
        for (EmbedCreateFields.Field field : embed.fields()) {
            if (field.name().equals("\u200B")) continue; // ignore empty fields (used for spacing)
            c = c
                .appendNewline()
                .append(Component.text(field.name() + ": " + replaceDiscordTime(field.value())));
        }
        return c;
    }

    public static String replaceDiscordTime(final String input) {
        return DISCORD_TIMESTAMP_PATTERN.matcher(input).replaceAll(matchResult -> {
            var timestamp = Long.parseLong(matchResult.group(1));
            var instant = Instant.ofEpochSecond(timestamp);
            return dateTimeFormatter.format(instant.atOffset(ZoneOffset.UTC));
        });
    }
}
