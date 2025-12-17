package com.zenith.command.api;

import com.zenith.Proxy;
import com.zenith.discord.Embed;
import com.zenith.feature.whitelist.PlayerList;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;

import java.util.List;

import static com.zenith.Globals.DISCORD;
import static com.zenith.Globals.TERMINAL_LOG;

@UtilityClass
public class CommandOutputHelper {
    public void logMultiLineOutputToDiscord(List<String> multiLine) {
        multiLine.forEach(DISCORD::sendMessage);
    }

    public void logEmbedOutputToDiscord(final Embed embed) {
        if (embed.isTitlePresent())
            DISCORD.sendEmbedMessage(embed);
    }

    public void logInputToDiscord(String command, CommandSource source, CommandContext ctx) {
        Embed embed = Embed.builder().title(source.name() + " Command Executed")
            .description(command);
        if (source instanceof PlayerCommandSource playerCommandSource) {
            ServerSession executor = ctx.getInGamePlayerInfo().session();
            embed.footer("Executed by: " + executor.getName(), Proxy.getInstance().getPlayerHeadURL(executor.getUUID()).toString());
        }
        DISCORD.sendEmbedMessage(embed);
    }

    public void logEmbedOutputToInGame(final Embed embed, final ServerSession session) {
        if (!embed.isTitlePresent()) return;
        var component = ComponentSerializer.deserializeEmbed(embed);
        session.sendAsyncMessage(component);
    }

    public void logEmbedOutputToInGameAllConnectedPlayers(final Embed embed) {
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            logEmbedOutputToInGame(embed, connection);
        }
    }

    public void logMultiLineOutputToInGame(final List<String> multiLine, final ServerSession session) {
        multiLine.forEach(line -> session.sendAsyncMessage(Component.text(line)));
    }

    public void logMultiLineOutputToInGameAllConnectedPlayers(final List<String> multLine) {
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            logMultiLineOutputToInGame(multLine, connection);
        }
    }

    public void logEmbedOutputToTerminal(final Embed embed) {
        if (!embed.isTitlePresent()) return;
        var component = ComponentSerializer.deserializeEmbed(embed);
        TERMINAL_LOG.info(component);
    }

    public String unescape(String s) {
        return s.replace("\\_", "_");
    }

    public void logMultiLineOutputToTerminal(List<String> multiLine) {
        multiLine.forEach(TERMINAL_LOG::info);
    }

    public void logMultiLineOutput(final List<String> multiLineOutput) {
        multiLineOutput.forEach(DISCORD::sendMessage);
    }

    // intended for use in embed descriptions
    public static String playerListToString(final PlayerList playerList) {
        var entries = playerList.entries();
        if (entries.isEmpty()) return "Empty";
        var output = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            String line;
            if (entry.isBedrock()) {
                line = String.format("`%s` (Bedrock)\n", entry.getUsername());
            } else {
                line = String.format("[%s](%s)\n", entry.getUsername(), entry.getNameMCLink());
            }
            if (output.length() + line.length() > 4000) { // 4096 max len + some buffer for more text before/after
                output.append("and ").append(entries.size() - i).append(" more...");
                break;
            } else output.append(line);
        }
        return output.toString();
    }
}
