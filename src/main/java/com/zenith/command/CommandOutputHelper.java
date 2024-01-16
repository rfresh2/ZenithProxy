package com.zenith.command;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.zenith.feature.whitelist.PlayerList;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static com.zenith.Shared.DISCORD_BOT;
import static com.zenith.Shared.TERMINAL_LOG;

@UtilityClass
public class CommandOutputHelper {
    private static final Pattern DISCORD_TIMESTAMP_PATTERN = Pattern.compile("<t:(\\d+):.>");

    public void logMultiLineOutputToDiscord(CommandContext commandContext) {
        if (DISCORD_BOT.isRunning()) {
            commandContext.getMultiLineOutput().forEach(DISCORD_BOT::sendMessage);
        }
    }

    public void logEmbedOutputToDiscord(final EmbedCreateSpec embed) {
        if (DISCORD_BOT.isRunning()) {
            if (embed.isTitlePresent()) {
                DISCORD_BOT.sendEmbedMessage(embed);
            }
        }
    }

    public void logInputToDiscord(String command, CommandSource source) {
        if (DISCORD_BOT.isRunning()) {
            DISCORD_BOT.sendEmbedMessage(EmbedCreateSpec.builder()
                                             .title(source.getName() + " Command Executed")
                                             .description(command)
                                             .build());
        }
    }

    public void logEmbedOutputToInGame(final EmbedCreateSpec embed, final ServerConnection session) {
        if (!embed.isTitlePresent()) return;
        var component = serializeDiscordEmbed(embed);
        session.sendAsync(new ClientboundSystemChatPacket(component, false));
    }

    private Component serializeDiscordEmbed(final EmbedCreateSpec embed) {
        Component c = Component.newline();
        if (embed.isColorPresent()) {
            var color = embed.color().get();
            c = c.applyFallbackStyle(Style.style(TextColor.color(
                color.getRed(),
                color.getGreen(),
                color.getBlue())));
        }
        // todo: handle discord formatted bold, italicized, or underlined text
        c = c.append(Component.text(embed.title().get()));
        if (embed.isDescriptionPresent()) {
            c = c
                .appendNewline()
                .append(Component.text(replaceDiscordTime(embed.description().get())));
        }
        if (embed.isUrlPresent()) {
            c = c
                .appendNewline()
                .append(Component.text(embed.url().get()));
        }
        for (EmbedCreateFields.Field field : embed.fields()) {
            if (field.name().equals("\u200B")) continue; // ignore empty fields (used for spacing)
            c = c
                .appendNewline()
                .append(Component.text(field.name() + ": " + replaceDiscordTime(field.value())));
        }
        return c;
    }

    public void logMultiLineOutputToInGame(final CommandContext commandContext, final ServerConnection session) {
        commandContext.getMultiLineOutput().forEach(line -> session.sendAsync(new ClientboundSystemChatPacket(Component.text(line), false)));
    }

    public void logEmbedOutputToTerminal(final EmbedCreateSpec embed) {
        // todo: handle formatted bold, italicized, or underlined text
        if (!embed.isTitlePresent()) return;
        var component = serializeDiscordEmbed(embed);
        var serializedAnsi = ComponentSerializer.serializeAnsi(component);
        TERMINAL_LOG.info(serializedAnsi);
    }

    public String unescape(String s) {
        return s.replace("\\_", "_");
    }

    public void logMultiLineOutputToTerminal(CommandContext context) {
        context.getMultiLineOutput().forEach(TERMINAL_LOG::info);
    }

    // intended for use in embed descriptions
    public static String playerListToString(final PlayerList playerList) {
        var entries = playerList.entries();
        if (entries.isEmpty()) return "Empty";
        var output = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            var line = "[" + entry.getUsername() + "](" + entry.getNameMCLink() + ")\n";
            if (output.length() + line.length() > 4000) { // 4096 max len + some buffer for more text before/after
                output.append("and ").append(entries.size() - i).append(" more...");
                break;
            } else output.append(line);
        }
        return output.toString();
    }

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String replaceDiscordTime(final String input) {
        return DISCORD_TIMESTAMP_PATTERN.matcher(input).replaceAll(matchResult -> {
            var timestamp = Long.parseLong(matchResult.group(1));
            var instant = Instant.ofEpochSecond(timestamp);
            return dateTimeFormatter.format(instant.atOffset(ZoneOffset.UTC));
        });
    }
}
