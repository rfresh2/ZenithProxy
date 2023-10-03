package com.zenith.network.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.zenith.Proxy;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.feature.queue.Queue;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import de.themoep.minedown.adventure.MineDown;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;

import static com.zenith.Shared.*;

public class ChatHandler implements IncomingHandler<ServerboundChatPacket, ServerConnection> {

    @Override
    public boolean apply(@NonNull ServerboundChatPacket packet, @NonNull ServerConnection session) {
        final String message = packet.getMessage();
        if (message.startsWith("!")) {
            final String lowerCase = message.toLowerCase();
            if (message.startsWith("!!")) {
                packet.setMessage(message.substring(1));
                return true;
            } else if (lowerCase.startsWith("!help")) {
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&9&lPlayer commands"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&2Prefix : \"!\""), false));
                session.send(new ClientboundSystemChatPacket(Component.text(""), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&chelp &7- &8Display help menu"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&cdc &7- &8Disconnect proxy"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&cq &7- &8Display 2b2t queue status"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&cm &7- &8Sends a message to spectators"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&ckick &7- &8Kicks all spectators"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&ckick <player> &7- &8Kicks a spectator by name"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&cspectator &7- &8Toggles spectator enabled status. If disabled, all spectators are kicked"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&cschat &7- &8Toggles if spectators are allowed to send public chats."), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&csync &7- &8Syncs current player inventory with server"), false));
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7&cchunksync &7- &8Syncs server chunks to the current player"), false));
                return false;
            } else if ("!dc".equalsIgnoreCase(message)) {
                Proxy.getInstance().getClient().disconnect(MANUAL_DISCONNECT);
                return false;
            } else if ("!q".equalsIgnoreCase(message)) {
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &7Queue: &c" + Queue.getQueueStatus().regular + " &r- &7Prio: &a" + Queue.getQueueStatus().prio), false));
                return false;
            } else if (lowerCase.startsWith("!m")) {
                Proxy.getInstance().getActiveConnections().forEach(connection -> {
                    connection.send(new ClientboundSystemChatPacket(MineDown.parse("&c" + session.getProfileCache().getProfile().getName() + " > " + message.substring(2).trim() + "&r"), false));
                });
                return false;
            } else if (lowerCase.startsWith("!kick")) {
                List<String> args = Arrays.asList(lowerCase.split(" "));
                if (args.size() == 1) {
                    Proxy.getInstance().getSpectatorConnections().forEach(connection ->
                            connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
                    session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cAll Spectators kicked&r"), false));
                } else {
                    final String playerName = args.get(1);
                    Proxy.getInstance().getSpectatorConnections().stream()
                            .filter(connection -> connection.getProfileCache().getProfile().getName().equalsIgnoreCase(playerName))
                            .forEach(connection -> {
                                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cKicked " + playerName + "&r"), true));
                                connection.disconnect(CONFIG.server.extra.whitelist.kickmsg);
                            });
                }
                return false;
            } else if (lowerCase.startsWith("!schat")) {
                CONFIG.server.spectator.spectatorPublicChatEnabled = !CONFIG.server.spectator.spectatorPublicChatEnabled;
                saveConfig();
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cSpectators public chat toggled " + (CONFIG.server.spectator.spectatorPublicChatEnabled ? "on" : "off") + "&r"), false));
                return false;
            } else if (lowerCase.startsWith("!spectator")) {
                CONFIG.server.spectator.allowSpectator = !CONFIG.server.spectator.allowSpectator;
                saveConfig();
                if (!CONFIG.server.spectator.allowSpectator) {
                    Proxy.getInstance().getSpectatorConnections().forEach(connection -> connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
                }
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cSpectators toggled " + (CONFIG.server.spectator.allowSpectator ? "on" : "off") + "&r"), false));
                return false;
            } else if (lowerCase.startsWith("!sync")) {
                PlayerCache.sync();
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cSync inventory complete"), false));
                return false;
            } else if (lowerCase.startsWith("!chunksync")) {
                ChunkCache.sync();
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cSync chunks complete"), false));
                return false;
            } else {
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cUnknown command: &o%msg%", "msg", message), false));
                return false;
            }
        }
        return true;
    }
}
