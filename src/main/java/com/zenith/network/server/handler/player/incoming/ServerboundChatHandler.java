package com.zenith.network.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
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

public class ServerboundChatHandler implements IncomingHandler<ServerboundChatPacket, ServerConnection> {

    @Override
    public boolean apply(@NonNull ServerboundChatPacket packet, @NonNull ServerConnection session) {
        final String message = packet.getMessage();
        if (message.startsWith("!")) {
            final String lowerCase = message.toLowerCase();
            if (message.startsWith("!!")) {
                //allow sending ingame commands to bots or whatever
                // todo: reimplement without reflection
//                PUnsafe.putObject(packet, CLIENTCHATPACKET_MESSAGE_OFFSET, message.substring(1));
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
                session.getProxy().getClient().disconnect(MANUAL_DISCONNECT);
                return false;
            } else if ("!q".equalsIgnoreCase(message)) {
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &7Queue: &c" + Queue.getQueueStatus().regular + " &r- &7Prio: &a" + Queue.getQueueStatus().prio), false));
                return false;
            } else if (lowerCase.startsWith("!m")) {
                session.getProxy().getActiveConnections().forEach(connection -> {
                    connection.send(new ClientboundSystemChatPacket(MineDown.parse("&c" + session.getProfileCache().getProfile().getName() + " > " + message.substring(2).trim() + "&r"), false));
                });
                return false;
            } else if (lowerCase.startsWith("!kick")) {
                List<String> args = Arrays.asList(lowerCase.split(" "));
                if (args.size() == 1) {
                    session.getProxy().getSpectatorConnections().forEach(connection ->
                            connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
                    session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cAll Spectators kicked&r"), false));
                } else {
                    final String playerName = args.get(1);
                    session.getProxy().getSpectatorConnections().stream()
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
                    session.getProxy().getSpectatorConnections().forEach(connection -> connection.disconnect(CONFIG.server.extra.whitelist.kickmsg));
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
        } else if (message.startsWith("/")) {
            // redirect server commands so we can still grab these in the proxy and simulate the effect for the client
            final String lowerCase = message.toLowerCase();
            if (lowerCase.startsWith("/ignorelist")) {
                CONFIG.client.extra.chat.ignoreList.forEach(s -> session.send(new ClientboundSystemChatPacket(MineDown.parse(
                    "&c" + s.username), true)));
                return false;
            } else if (lowerCase.startsWith("/ignoredeathmsgs")) { // /ignoredeathmsgs <name>
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cNot implemented yet"), false));
                return false;
            } else if (lowerCase.startsWith("/ignore")) { // can also be ignorehard which is what we treat these like anyway
                String[] split = message.split(" ");
                if (split.length == 2) {
                    final String player = split[1];
                    if (WHITELIST_MANAGER.isPlayerIgnored(player)) {
                        WHITELIST_MANAGER.removeIgnoreWhitelistEntryByUsername(player);
                        session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cRemoved " + player + " from ignore list"), false));
                        return false;
                    }
                    WHITELIST_MANAGER.addIgnoreWhitelistEntryByUsername(player).ifPresentOrElse(
                            ignoreEntry -> session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cAdded " + ignoreEntry.username + " to ignore list"), false)),
                            () -> session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cFailed to add " + player + " to ignore list"), false))
                    );
                } else {
                    session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cInvalid syntax. Usage: /ignore <name>"), false));
                }
                return false;
            } else if (lowerCase.startsWith("/togglechat")) {
                CONFIG.client.extra.chat.hideChat = !CONFIG.client.extra.chat.hideChat;
                saveConfig();
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cChat toggled " + (CONFIG.client.extra.chat.hideChat ? "off" : "on") + "&r"), false));
                return false;
            } else if (lowerCase.startsWith("/toggleprivatemsgs")) {
                CONFIG.client.extra.chat.hideWhispers = !CONFIG.client.extra.chat.hideWhispers;
                saveConfig();
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cWhispers messages toggled " + (CONFIG.client.extra.chat.hideWhispers ? "off" : "on") + "&r"), false));
                return false;
            } else if (lowerCase.startsWith("/toggledeathmsgs")) {
                CONFIG.client.extra.chat.hideDeathMessages = !CONFIG.client.extra.chat.hideDeathMessages;
                saveConfig();
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cDeath messages toggled " + (CONFIG.client.extra.chat.hideDeathMessages ? "off" : "on") + "&r"), false));
                return false;
            } else if (lowerCase.startsWith("/toggleconnectionmsgs")) {
                CONFIG.client.extra.chat.showConnectionMessages = !CONFIG.client.extra.chat.showConnectionMessages;
                saveConfig();
                session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cConnection messages toggled " + (CONFIG.client.extra.chat.showConnectionMessages ? "on" : "off") + "&r"), false));
                return false;
            }
        }
        return true;
    }

    @Override
    public Class<ServerboundChatPacket> getPacketClass() {
        return ServerboundChatPacket.class;
    }
}
