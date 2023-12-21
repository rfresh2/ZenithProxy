package com.zenith.command;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.google.common.collect.ImmutableMap;
import com.zenith.feature.whitelist.PlayerList;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.Map;

import static com.zenith.Shared.DISCORD_BOT;
import static com.zenith.Shared.TERMINAL_LOG;

@UtilityClass
public class CommandOutputHelper {
    // todo: we could skip the minedown parsing and just use adventure TextFormat RGB coloring
    private static final Map<Color, String> discordColorToMCFormatCodeMap = ImmutableMap.of(
        Color.BLACK, "&0",
        Color.RED, "&c",
        Color.CYAN, "&b",
        Color.GREEN, "&a",
        Color.MAGENTA, "&d",
        Color.MEDIUM_SEA_GREEN, "&a",
        Color.RUBY, "&c",
        Color.MOON_YELLOW, "&e"
    );

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
        final StringBuilder output = new StringBuilder();
        if (embed.isColorPresent()) {
            output.append(discordColorToMCFormatCodeMap.getOrDefault(embed.color().get(), ""));
        }
        output.append("\n");
        // todo: handle discord formatted bold, italicized, or underlined text
        output.append(embed.title().get());
        if (embed.isDescriptionPresent()) {
            output.append("\n");
            output.append(embed.description().get());
        }
        if (embed.isUrlPresent()) {
            output.append("\n");
            output.append(embed.url().get());
        }
        for (EmbedCreateFields.Field field : embed.fields()) {
            if (field.name().equals("\u200B")) continue; // ignore empty fields (used for spacing)
            // todo: format fields as in discord where there can be multiple on a line
            output.append("\n");
            output.append(field.name());
            output.append(": ");
            if (field.value().equals("\u200B")) continue;
            output.append(field.value());
        }
        session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.mineDownParse(output.toString()), false));
    }

    public void logMultiLineOutputToInGame(final CommandContext commandContext, final ServerConnection session) {
        commandContext.getMultiLineOutput().forEach(line -> session.sendAsync(new ClientboundSystemChatPacket(Component.text(line), false)));
    }

    public void logEmbedOutputToTerminal(final EmbedCreateSpec embed) {
        // todo: handle formatted bold, italicized, or underlined text
        if (!embed.isTitlePresent()) return;
        final AttributedStringBuilder output = new AttributedStringBuilder();
        if (embed.isColorPresent()) {
            final Color color = embed.color().get();
            output.style(AttributedStyle.DEFAULT.foreground(color.getRed(), color.getBlue(), color.getGreen()));
        }
        output.append("\n");
        output.append(embed.title().get());
        if (embed.isDescriptionPresent()) {
            output.append("\n");
            output.append(embed.description().get());
        }
        if (embed.isUrlPresent()) {
            output.append("\n");
            output.append(embed.url().get());
        }
        for (EmbedCreateFields.Field field : embed.fields()) {
            if (field.name().equals("\u200B")) continue; // ignore empty fields (used for spacing)
            // todo: format fields as in discord where there can be multiple on a line
            output.append("\n");
            output.append(field.name());
            output.append(": ");
            if (field.value().equals("\u200B")) continue;
            output.append(field.value());
        }
        TERMINAL_LOG.info(unescape(output.toAnsi()));
    }

    public String unescape(String s) {
        return s.replace("\\_", "_");
    }

    public void logMultiLineOutputToTerminal(CommandContext context) {
        context.getMultiLineOutput().forEach(TERMINAL_LOG::info);
    }

    // intended for use in embed descriptions
    public static String playerListToString(final PlayerList playerList) {
        var entries = playerList.getEntries();
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
}
