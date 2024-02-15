package com.zenith.command;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.zenith.discord.Embed;
import com.zenith.feature.whitelist.PlayerList;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;

import static com.zenith.Shared.DISCORD_BOT;
import static com.zenith.Shared.TERMINAL_LOG;

@UtilityClass
public class CommandOutputHelper {
    public void logMultiLineOutputToDiscord(CommandContext commandContext) {
        if (DISCORD_BOT.isRunning()) {
            commandContext.getMultiLineOutput().forEach(DISCORD_BOT::sendMessage);
        }
    }

    public void logEmbedOutputToDiscord(final Embed embed) {
        if (DISCORD_BOT.isRunning() && embed.isTitlePresent())
            DISCORD_BOT.sendEmbedMessage(embed);
    }

    public void logInputToDiscord(String command, CommandSource source) {
        if (DISCORD_BOT.isRunning()) {
            DISCORD_BOT.sendEmbedMessage(Embed.builder()
                                             .title(source.getName() + " Command Executed")
                                             .description(command));
        }
    }

    public void logEmbedOutputToInGame(final Embed embed, final ServerConnection session) {
        if (!embed.isTitlePresent()) return;
        var component = ComponentSerializer.deserializeEmbed(embed);
        session.sendAsync(new ClientboundSystemChatPacket(component, false));
    }

    public void logMultiLineOutputToInGame(final CommandContext commandContext, final ServerConnection session) {
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
