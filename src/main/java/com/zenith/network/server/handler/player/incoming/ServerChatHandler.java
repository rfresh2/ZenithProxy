package com.zenith.network.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.zenith.cache.data.PlayerCache;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.feature.queue.Queue;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;

import static com.zenith.Shared.*;

public class ServerChatHandler implements IncomingHandler<ClientChatPacket, ServerConnection> {

    @Override
    public boolean apply(@NonNull ClientChatPacket packet, @NonNull ServerConnection session) {
        final String message = packet.getMessage();
        if (message.startsWith("!")) {
            final String lowerCase = message.toLowerCase();
            if (message.startsWith("!!")) {
                //allow sending ingame commands to bots or whatever
                // todo: reimplement without reflection
//                PUnsafe.putObject(packet, CLIENTCHATPACKET_MESSAGE_OFFSET, message.substring(1));
                return true;
            } else if (lowerCase.startsWith("!help")) {
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
            } else if ("!dc".equalsIgnoreCase(message)) {
                session.getProxy().getClient().disconnect(MANUAL_DISCONNECT);
                return false;
            } else if ("!q".equalsIgnoreCase(message)) {
                session.send(new ServerChatPacket(String.format("§7[§9Proxy§7]§r §7Queue: §c" + Queue.getQueueStatus().regular + " §r- §7Prio: §a" + Queue.getQueueStatus().prio, message), true));
                return false;
            } else if (lowerCase.startsWith("!m")) {
                session.getProxy().getActiveConnections().forEach(connection -> {
                    connection.send(new ServerChatPacket("§c" + session.getProfileCache().getProfile().getName() + " > " + message.substring(2).trim() + "§r", true));
                });
                return false;
            } else if (lowerCase.startsWith("!kick")) {
                List<String> args = Arrays.asList(lowerCase.split(" "));
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
            } else if (lowerCase.startsWith("!schat")) {
                CONFIG.server.spectator.spectatorPublicChatEnabled = !CONFIG.server.spectator.spectatorPublicChatEnabled;
                saveConfig();
                session.send(new ServerChatPacket("§cSpectators public chat toggled " + (CONFIG.server.spectator.spectatorPublicChatEnabled ? "on" : "off") + "§r", true));
                return false;
            } else if (lowerCase.startsWith("!spectator")) {
                CONFIG.server.spectator.allowSpectator = !CONFIG.server.spectator.allowSpectator;
                saveConfig();
                if (!CONFIG.server.spectator.allowSpectator) {
                    session.getProxy().getSpectatorConnections().forEach(connection -> connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
                }
                session.send(new ServerChatPacket("§cSpectators toggled " + (CONFIG.server.spectator.allowSpectator ? "on" : "off") + "§r", true));
                return false;
            } else if (lowerCase.startsWith("!sync")) {
                PlayerCache.sync();
                session.send(new ServerChatPacket("§cSync inventory complete", true));
                return false;
            } else if (lowerCase.startsWith("!chunksync")) {
                ChunkCache.sync();
                session.send(new ServerChatPacket("§cSync chunks complete", true));
                return false;
            } else {
                session.send(new ServerChatPacket(String.format("§7[§9Proxy§7]§r §cUnknown command: §o%s", message), true));
                return false;
            }
        } else if (message.startsWith("/")) {
            // redirect server commands so we can still grab these in the proxy and simulate the effect for the client
            final String lowerCase = message.toLowerCase();
            if (lowerCase.startsWith("/ignorelist")) {
                CONFIG.client.extra.chat.ignoreList.forEach(s -> session.send(new ServerChatPacket("§c" + s.username, true)));
                return false;
            } else if (lowerCase.startsWith("/ignoredeathmsgs")) { // /ignoredeathmsgs <name>
                // todo
                session.send(new ServerChatPacket("§cNot implemented yet", true));
                return false;
            } else if (lowerCase.startsWith("/ignore")) { // can also be ignorehard which is what we treat these like anyway
                String[] split = message.split(" ");
                if (split.length == 2) {
                    final String player = split[1];
                    if (WHITELIST_MANAGER.isPlayerIgnored(player)) {
                        WHITELIST_MANAGER.removeIgnoreWhitelistEntryByUsername(player);
                        session.send(new ServerChatPacket("§cRemoved " + player + " from ignore list", true));
                        return false;
                    }
                    WHITELIST_MANAGER.addIgnoreWhitelistEntryByUsername(player).ifPresentOrElse(
                            ignoreEntry -> session.send(new ServerChatPacket("§cAdded " + ignoreEntry.username + " to ignore list", true)),
                            () -> session.send(new ServerChatPacket("§cFailed to add " + player + " to ignore list", true))
                    );
                } else {
                    session.send(new ServerChatPacket("§cInvalid syntax. Usage: /ignore <name>", true));
                }
                return false;
            } else if (lowerCase.startsWith("/togglechat")) {
                CONFIG.client.extra.chat.hideChat = !CONFIG.client.extra.chat.hideChat;
                saveConfig();
                session.send(new ServerChatPacket("§cChat toggled " + (CONFIG.client.extra.chat.hideChat ? "off" : "on") + "§r", true));
                return false;
            } else if (lowerCase.startsWith("/toggleprivatemsgs")) {
                CONFIG.client.extra.chat.hideWhispers = !CONFIG.client.extra.chat.hideWhispers;
                saveConfig();
                session.send(new ServerChatPacket("§cWhispers messages toggled " + (CONFIG.client.extra.chat.hideWhispers ? "off" : "on") + "§r", true));
                return false;
            } else if (lowerCase.startsWith("/toggledeathmsgs")) {
                CONFIG.client.extra.chat.hideDeathMessages = !CONFIG.client.extra.chat.hideDeathMessages;
                saveConfig();
                session.send(new ServerChatPacket("§cDeath messages toggled " + (CONFIG.client.extra.chat.hideDeathMessages ? "off" : "on") + "§r", true));
                return false;
            } else if (lowerCase.startsWith("/toggleconnectionmsgs")) {
                CONFIG.client.extra.chat.showConnectionMessages = !CONFIG.client.extra.chat.showConnectionMessages;
                saveConfig();
                session.send(new ServerChatPacket("§cConnection messages toggled " + (CONFIG.client.extra.chat.showConnectionMessages ? "on" : "off") + "§r", true));
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
