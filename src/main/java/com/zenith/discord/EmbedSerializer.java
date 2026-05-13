package com.zenith.discord;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class EmbedSerializer {
    private static final Pattern DISCORD_TIMESTAMP_PATTERN = Pattern.compile("<t:(\\d+):.>");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss O");
    // used to find discord formatted text and replace it with the appropriate style
    // **bold** -> bold
    // *italic* -> italic
    // _italic_ -> italic
    // __underline__ -> underline
    // `code` -> code
    // ```code block``` -> code block
    // [link](url) -> text with click event
    // ||spoiler|| -> spoiler
    // there's more we don't currently use that aren't implemented here
    private static final Pattern DISCORD_FORMATTING_REGEX = Pattern.compile(
        "(\\*\\*(\\w[^\\n\\\\\\*]*)\\*\\*)|(\\*(\\w[^\\n\\\\\\*]*)\\*)|(__(\\w[^\\n\\\\_]*)__)|(_(\\w[^\\n\\\\_]*)_)|(```(\\w[^\\\\`]*)```)|(`(\\w[^\\n\\\\`]*)`)|(\\[(.+?)]\\((.+?)\\))|(\\|\\|(.+?)\\|\\|)"
    );

    public static Component serialize(final Embed embed) {
        var c = Component.text()
            .appendNewline();
        if (embed.isTitlePresent()) {
            c.append(serializeText(embed.title()).decorate(TextDecoration.BOLD));
            if (embed.isDescriptionPresent() || embed.isUrlPresent() || !embed.fields().isEmpty())
                c.appendNewline();
        }
        if (embed.isDescriptionPresent()) {
            c.appendNewline()
                .append(serializeText(replaceDiscordTime(embed.description())));
        }
        if (embed.isUrlPresent()) {
            c.appendNewline()
                .append(Component
                            .text(embed.url())
                            .color(NamedTextColor.BLUE)
                            .clickEvent(ClickEvent.openUrl(embed.url()))
                            .hoverEvent(HoverEvent.showText(Component.text(embed.url()))));
        }
        for (var field : embed.fields()) {
            if (field.name().equals("\u200B")) continue; // ignore empty fields (used for spacing)
            c.appendNewline()
                .append(Component.text(field.name())
                            .appendNewline()
                            .decorate(TextDecoration.BOLD))
                .append(serializeText(replaceDiscordTime(field.value())));
        }
        if (embed.isColorPresent()) {
            var color = embed.color();
            c.color(TextColor.color(
                color.r(),
                color.g(),
                color.b()));
        }
        return c.build();
    }

    public static String replaceDiscordTime(final String input) {
        return DISCORD_TIMESTAMP_PATTERN.matcher(input).replaceAll(matchResult -> {
            var timestamp = Long.parseLong(matchResult.group(1));
            var instant = Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.systemDefault());
            return dateTimeFormatter.format(instant);
        });
    }

    public static Component serializeText(String text) {
        var matcher = DISCORD_FORMATTING_REGEX.matcher(text);
        var component = Component.text();
        var lastEnd = 0;
        while (matcher.find()) {
            var start = matcher.start();
            var end = matcher.end();
            if (start > lastEnd) {
                component.append(Component.text(text.substring(lastEnd, start)));
            }
            if (matcher.group(2) != null) {
                component.append(Component.text(matcher.group(2)).decorate(TextDecoration.BOLD));
            } else if (matcher.group(4) != null) {
                component.append(Component.text(matcher.group(4)).decorate(TextDecoration.ITALIC));
            } else if (matcher.group(6) != null) {
                component.append(Component.text(matcher.group(6)).decorate(TextDecoration.UNDERLINED));
            } else if (matcher.group(8) != null) {
                component.append(Component.text(matcher.group(8)).decorate(TextDecoration.ITALIC));
            } else if (matcher.group(10) != null) {
                component.append(Component.text(matcher.group(10)).color(NamedTextColor.GRAY));
            } else if (matcher.group(12) != null) {
                component.append(Component.text(matcher.group(12)).color(NamedTextColor.GRAY));
            } else if (matcher.group(14) != null) {
                component.append(Component.text(matcher.group(14)).color(NamedTextColor.BLUE)
                    .clickEvent(ClickEvent.openUrl(matcher.group(15)))
                    .hoverEvent(HoverEvent.showText(Component.text(matcher.group(15)))));
            } else if (matcher.group(16) != null) {
                component.append(Component.text(matcher.group(16)))
                                     .decorate(TextDecoration.ITALIC);
            }
            lastEnd = end;
        }
        if (lastEnd < text.length()) {
            component.append(Component.text(text.substring(lastEnd)));
        }

        component.applyDeep(c -> {
            if (c instanceof TextComponent.Builder t) {
                t.content(DiscordBot.unescape(t.content()));
            }
        });

        return component.build();
    }
}
