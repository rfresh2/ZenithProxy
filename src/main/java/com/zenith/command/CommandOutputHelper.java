package com.zenith.command;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.google.common.collect.ImmutableMap;
import com.zenith.network.server.ServerConnection;
import de.themoep.minedown.adventure.MineDown;
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

    public void logInputToDiscord(String command) {
        if (DISCORD_BOT.isRunning()) {
            DISCORD_BOT.sendEmbedMessage(EmbedCreateSpec.builder()
                                             .title("Terminal Command Executed")
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
        output.append(embed.title().get());
        if (embed.isDescriptionPresent()) {
            output.append("\n");
            output.append(embed.description().get());
        }
        if (embed.isUrlPresent()) {
            output.append("\n");
            output.append(embed.url().get());
        }
        embed.fields().forEach(field -> {
            // todo: format fields as in discord where there can be multiple on a line
            output.append("\n");
            output.append(field.name());
            output.append(": ");
            output.append(field.value());
        });
        session.sendAsync(new ClientboundSystemChatPacket(MineDown.parse(output.toString()), false));
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
        embed.fields().forEach(field -> {
            // todo: format fields as in discord where there can be multiple on a line
            output.append("\n");
            output.append(field.name());
            output.append(": ");
            output.append(field.value());
        });
        TERMINAL_LOG.info(unescape(output.toAnsi()));
    }

    public String unescape(String s) {
        return s.replace("\\_", "_");
    }

    public void logMultiLineOutputToTerminal(CommandContext context) {
        context.getMultiLineOutput().forEach(TERMINAL_LOG::info);
    }
}
