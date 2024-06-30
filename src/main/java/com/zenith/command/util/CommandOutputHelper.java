package com.zenith.command.util;

import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.discord.Embed;
import com.zenith.feature.whitelist.PlayerList;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

import java.util.List;

import static com.zenith.Shared.DISCORD;
import static com.zenith.Shared.TERMINAL_LOG;

@UtilityClass
public class CommandOutputHelper {
    public void logMultiLineOutputToDiscord(CommandContext commandContext) {
        if (DISCORD.isRunning()) {
            commandContext.getMultiLineOutput().forEach(DISCORD::sendMessage);
        }
    }

    public void logEmbedOutputToDiscord(final Embed embed) {
        if (DISCORD.isRunning() && embed.isTitlePresent())
            DISCORD.sendEmbedMessage(embed);
    }

    public void logInputToDiscord(String command, CommandSource source) {
        if (DISCORD.isRunning()) {
            DISCORD.sendEmbedMessage(Embed.builder()
                                             .title(source.getName() + " Command Executed")
                                             .description(command));
        }
    }

    public void logEmbedOutputToInGame(final Embed embed, final ServerSession session) {
        if (!embed.isTitlePresent()) return;
        var component = ComponentSerializer.deserializeEmbed(embed);
        session.sendAsync(new ClientboundSystemChatPacket(component, false));
    }

    public void logMultiLineOutputToInGame(final CommandContext commandContext, final ServerSession session) {
        commandContext.getMultiLineOutput().forEach(line -> session.sendAsync(new ClientboundSystemChatPacket(Component.text(line), false)));
    }

    public void logEmbedOutputToTerminal(final Embed embed) {
        if (!embed.isTitlePresent()) return;
        var component = ComponentSerializer.deserializeEmbed(embed);
        TERMINAL_LOG.info(ComponentSerializer.serializeJson(component));
    }

    public String unescape(String s) {
        return s.replace("\\_", "_");
    }

    public void logMultiLineOutputToTerminal(CommandContext context) {
        context.getMultiLineOutput().forEach(TERMINAL_LOG::info);
    }

    public void logMultiLineOutput(final List<String> multiLineOutput) {
        if (DISCORD.isRunning())
            multiLineOutput.forEach(DISCORD::sendMessage);
        else
            multiLineOutput.forEach(TERMINAL_LOG::info);
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
}
