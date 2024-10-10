package com.zenith.network.server.handler.player.incoming;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;

import static com.zenith.Shared.*;

public class ChatCommandHandler implements PacketHandler<ServerboundChatCommandPacket, ServerSession> {
    @Override
    public ServerboundChatCommandPacket apply(final ServerboundChatCommandPacket packet, final ServerSession session) {
        final String command = packet.getCommand();
        if (command.isBlank()) return packet;
        if (CONFIG.inGameCommands.slashCommands
            && CONFIG.inGameCommands.enable
            && (IN_GAME_COMMAND.handleInGameCommand(
                command,
                session,
                CONFIG.inGameCommands.slashCommandsReplacesServerCommands)
            || CONFIG.inGameCommands.slashCommandsReplacesServerCommands))
                return null;
        return replaceExtraChatServerCommands(command, session) ? packet : null;
    }

    public static boolean replaceExtraChatServerCommands(
        final String command,
        final ServerSession session
    ) {
        final String commandLowercased = command.toLowerCase().split(" ")[0];
        // todo: replace these by executing `extraChat <cmd>`?
        return switch (commandLowercased) {
            case "ignorelist" -> {
                CONFIG.client.extra.chat.ignoreList.forEach(s -> session.send(
                    new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>" + s.getUsername()), true)));
                yield false;
            }
            case "ignoredeathmsgs" -> { // todo
                session.sendAsyncAlert("<red>Not implemented yet");
                yield false;
            }
            case "ignore" -> {
                String[] split = command.split(" ");
                if (split.length == 2) {
                    final String player = split[1];
                    if (PLAYER_LISTS.getIgnoreList().contains(player)) {
                        PLAYER_LISTS.getIgnoreList().remove(player);
                        session.sendAsyncAlert("<red>Removed " + player + " from ignore list");
                        yield false;
                    }
                    PLAYER_LISTS.getIgnoreList().add(player).ifPresentOrElse(
                        ignoreEntry -> session.sendAsyncAlert("<red>Added " + ignoreEntry.getUsername() + " to ignore list"),
                        () -> session.sendAsyncAlert("<red>Failed to add " + player + " to ignore list")
                    );
                } else {
                    session.sendAsyncAlert("<red>Invalid syntax. Usage: /ignore \\<name>");
                }
                yield false;
            }
            case "togglechat" -> {
                CONFIG.client.extra.chat.hideChat = !CONFIG.client.extra.chat.hideChat;
                saveConfigAsync();
                session.sendAsyncAlert("<red>Chat toggled " + (CONFIG.client.extra.chat.hideChat ? "off" : "on"));
                yield false;
            }
            case "toggleprivatemsgs" -> {
                CONFIG.client.extra.chat.hideWhispers = !CONFIG.client.extra.chat.hideWhispers;
                saveConfigAsync();
                session.sendAsyncAlert("<red>Whispers messages toggled " + (CONFIG.client.extra.chat.hideWhispers ? "off" : "on"));
                yield false;
            }
            case "toggledeathmsgs" -> {
                CONFIG.client.extra.chat.hideDeathMessages = !CONFIG.client.extra.chat.hideDeathMessages;
                saveConfigAsync();
                session.sendAsyncAlert("<red>Death messages toggled " + (CONFIG.client.extra.chat.hideDeathMessages ? "off" : "on"));
                yield false;
            }
            case "toggleconnectionmsgs" -> {
                CONFIG.client.extra.chat.showConnectionMessages = !CONFIG.client.extra.chat.showConnectionMessages;
                saveConfigAsync();
                session.sendAsyncAlert("<red>Connection messages toggled " + (CONFIG.client.extra.chat.showConnectionMessages ? "on" : "off"));
                yield false;
            }
            default -> true;
        };
    }
}
