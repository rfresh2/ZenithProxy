package com.zenith.network.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerConnection;
import com.zenith.util.ComponentSerializer;

import static com.zenith.Shared.*;

public class ChatCommandHandler implements PacketHandler<ServerboundChatCommandPacket, ServerConnection> {
    @Override
    public ServerboundChatCommandPacket apply(final ServerboundChatCommandPacket packet, final ServerConnection session) {
        final String command = packet.getCommand();
        if (command.isBlank()) return packet;
        final String commandLowercased = command.toLowerCase().split(" ")[0];
        if (CONFIG.inGameCommands.slashCommands) {
            IN_GAME_COMMAND_MANAGER.handleInGameCommand(command, session);
            return null;
        } else {
            return switch (commandLowercased) {
                case "ignorelist" -> {
                    CONFIG.client.extra.chat.ignoreList.forEach(s -> session.send(new ClientboundSystemChatPacket(
                        ComponentSerializer.minedown(
                            "&c" + s.getUsername()), true)));
                    yield null;
                }
                case "ignoredeathmsgs" -> {
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&9ZenithProxy&7]&r &cNot implemented yet"), false));
                    yield null;
                }
                case "ignore" -> {
                    String[] split = command.split(" ");
                    if (split.length == 2) {
                        final String player = split[1];
                        if (PLAYER_LISTS.getIgnoreList().contains(player)) {
                            PLAYER_LISTS.getIgnoreList().remove(player);
                            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&9ZenithProxy&7]&r &cRemoved " + player + " from ignore list"), false));
                            yield null;
                        }
                        PLAYER_LISTS.getIgnoreList().add(player).ifPresentOrElse(
                            ignoreEntry -> session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&9ZenithProxy&7]&r &cAdded " + ignoreEntry.getUsername() + " to ignore list"), false)),
                            () -> session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&9ZenithProxy&7]&r &cFailed to add " + player + " to ignore list"), false))
                        );
                    } else {
                        session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&9ZenithProxy&7]&r &cInvalid syntax. Usage: /ignore <name>"), false));
                    }
                    yield null;
                }
                case "togglechat" -> {
                    CONFIG.client.extra.chat.hideChat = !CONFIG.client.extra.chat.hideChat;
                    saveConfigAsync();
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&9ZenithProxy&7]&r &cChat toggled " + (CONFIG.client.extra.chat.hideChat ? "off" : "on") + "&r"), false));
                    yield null;
                }
                case "toggleprivatemsgs" -> {
                    CONFIG.client.extra.chat.hideWhispers = !CONFIG.client.extra.chat.hideWhispers;
                    saveConfigAsync();
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&9ZenithProxy&7]&r &cWhispers messages toggled " + (CONFIG.client.extra.chat.hideWhispers ? "off" : "on") + "&r"), false));
                    yield null;
                }
                case "toggledeathmsgs" -> {
                    CONFIG.client.extra.chat.hideDeathMessages = !CONFIG.client.extra.chat.hideDeathMessages;
                    saveConfigAsync();
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&9ZenithProxy&7]&r &cDeath messages toggled " + (CONFIG.client.extra.chat.hideDeathMessages ? "off" : "on") + "&r"), false));
                    yield null;
                }
                case "toggleconnectionmsgs" -> {
                    CONFIG.client.extra.chat.showConnectionMessages = !CONFIG.client.extra.chat.showConnectionMessages;
                    saveConfigAsync();
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&7[&9ZenithProxy&7]&r &cConnection messages toggled " + (CONFIG.client.extra.chat.showConnectionMessages ? "on" : "off") + "&r"), false));
                    yield null;
                }
                default -> packet;
            };
        }
    }
}
