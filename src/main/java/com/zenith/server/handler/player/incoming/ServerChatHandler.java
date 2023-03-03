package com.zenith.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.server.ServerConnection;
import com.zenith.util.Queue;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Arrays;
import java.util.List;

import static com.zenith.util.Constants.*;

public class ServerChatHandler implements HandlerRegistry.IncomingHandler<ClientChatPacket, ServerConnection> {
    protected static final long CLIENTCHATPACKET_MESSAGE_OFFSET = PUnsafe.pork_getOffset(ClientChatPacket.class, "message");

    @Override
    public boolean apply(@NonNull ClientChatPacket packet, @NonNull ServerConnection session) {
        if (packet.getMessage().startsWith("!"))   {
            if (packet.getMessage().startsWith("!!")) {
                //allow sending ingame commands to bots or whatever
                PUnsafe.putObject(packet, CLIENTCHATPACKET_MESSAGE_OFFSET, packet.getMessage().substring(1));
                return true;
            } else if (packet.getMessage().toLowerCase().startsWith("!help")) {
                session.send(new ServerChatPacket("§9§lPlayer commands:", true));
                session.send(new ServerChatPacket("§2Prefix : \"!\"", true));
                session.send(new ServerChatPacket("", true));
                session.send(new ServerChatPacket("§7§chelp §7- §8Display help menu", true));
                session.send(new ServerChatPacket("§7§cdc §7- §8Disconnect proxy", true));
                session.send(new ServerChatPacket("§7§cq §7- §8Display 2b2t queue status", true));
                session.send(new ServerChatPacket("§7§cm §7- §8Sends a message to spectators", true));
                session.send(new ServerChatPacket("§7§ckick §7- §8Kicks all spectators", true));
                session.send(new ServerChatPacket("§7§ckick <player>§7- §8Kicks a spectator by name", true));
                session.send(new ServerChatPacket("§7§cspectator§7- §8Toggles spectator enabled status. If disabled, all spectators are kicked", true));
                session.send(new ServerChatPacket("§7§cschat§7- §8Toggles if spectators are allowed to send public chats.", true));
                session.send(new ServerChatPacket("§7§csync §7- §8Syncs current player inventory with server", true));
                session.send(new ServerChatPacket("§7§cchunksync §7- §8Syncs server chunks to the current player", true));
                return false;
            } else if ("!dc".equalsIgnoreCase(packet.getMessage())) {
                session.getProxy().getClient().disconnect(MANUAL_DISCONNECT);
                return false;
            } else if ("!q".equalsIgnoreCase(packet.getMessage())) {
                session.send(new ServerChatPacket(String.format("§7[§9Proxy§7]§r §7Queue: §c" + Queue.getQueueStatus().regular + " §r- §7Prio: §a" + Queue.getQueueStatus().prio, packet.getMessage()), true));
                return false;
            } else if (packet.getMessage().toLowerCase().startsWith("!m")) {
                session.getProxy().getActiveConnections().forEach(connection -> {
                    connection.send(new ServerChatPacket("§c" + session.getProfileCache().getProfile().getName() + " > " + packet.getMessage().substring(2).trim() + "§r", true));
                });
                return false;
            } else if (packet.getMessage().toLowerCase().startsWith("!kick")) {
                List<String> args = Arrays.asList(packet.getMessage().toLowerCase().split(" "));
                if (args.size() == 1) {
                    session.getProxy().getSpectatorConnections().forEach(connection ->
                            connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
                    session.send(new ServerChatPacket("§cAll Spectators kicked§r", true));
                } else {
                    final String playerName = args.get(1);
                    session.getProxy().getSpectatorConnections().stream()
                            .filter(connection -> connection.getProfileCache().getProfile().getName().equalsIgnoreCase(playerName))
                            .forEach(connection -> {
                                session.send(new ServerChatPacket("§cKicked " + playerName + "§r", true));
                                connection.disconnect(CONFIG.server.extra.whitelist.kickmsg);
                            });
                }
                return false;
            } else if (packet.getMessage().toLowerCase().startsWith("!schat")) {
                CONFIG.server.spectator.spectatorPublicChatEnabled = !CONFIG.server.spectator.spectatorPublicChatEnabled;
                saveConfig();
                session.send(new ServerChatPacket("§cSpectators public chat toggled " + (CONFIG.server.spectator.spectatorPublicChatEnabled ? "on" : "off") + "§r", true));
                return false;
            } else if (packet.getMessage().toLowerCase().startsWith("!spectator")) {
                CONFIG.server.spectator.allowSpectator = !CONFIG.server.spectator.allowSpectator;
                saveConfig();
                if (!CONFIG.server.spectator.allowSpectator) {
                    session.getProxy().getSpectatorConnections().forEach(connection -> connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
                }
                session.send(new ServerChatPacket("§cSpectators toggled " + (CONFIG.server.spectator.allowSpectator ? "on" : "off") + "§r", true));
                return false;
            } else if (packet.getMessage().toLowerCase().startsWith("!sync")) {
                PlayerCache.sync();
                session.send(new ServerChatPacket("§cSync inventory complete", true));
                return false;
            } else if (packet.getMessage().toLowerCase().startsWith("!chunksync")) {
                ChunkCache.sync();
                session.send(new ServerChatPacket("§cSync chunks complete", true));
                return false;
            } else {
                session.send(new ServerChatPacket(String.format("§7[§9Proxy§7]§r §cUnknown command: §o%s", packet.getMessage()), true));
                return false;
            }
        }
        return true;
    }

    @Override
    public Class<ClientChatPacket> getPacketClass() {
        return ClientChatPacket.class;
    }
}
