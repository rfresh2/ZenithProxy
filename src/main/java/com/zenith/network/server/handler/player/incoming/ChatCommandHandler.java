package com.zenith.network.server.handler.player.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.zenith.network.registry.IncomingHandler;
import com.zenith.network.server.ServerConnection;
import de.themoep.minedown.adventure.MineDown;

import static com.zenith.Shared.*;

public class ChatCommandHandler implements IncomingHandler<ServerboundChatCommandPacket, ServerConnection> {
    @Override
    public boolean apply(final ServerboundChatCommandPacket packet, final ServerConnection session) {
        final String message = packet.getCommand();
        final String lowerCase = message.toLowerCase();
        if (lowerCase.startsWith("ignorelist")) {
            CONFIG.client.extra.chat.ignoreList.forEach(s -> session.send(new ClientboundSystemChatPacket(MineDown.parse(
                "&c" + s.username), true)));
            return false;
        } else if (lowerCase.startsWith("ignoredeathmsgs")) { // /ignoredeathmsgs <name>
            session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cNot implemented yet"), false));
            return false;
        } else if (lowerCase.startsWith("ignore")) { // can also be ignorehard which is what we treat these like anyway
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
        } else if (lowerCase.startsWith("togglechat")) {
            CONFIG.client.extra.chat.hideChat = !CONFIG.client.extra.chat.hideChat;
            saveConfigAsync();
            session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cChat toggled " + (CONFIG.client.extra.chat.hideChat ? "off" : "on") + "&r"), false));
            return false;
        } else if (lowerCase.startsWith("toggleprivatemsgs")) {
            CONFIG.client.extra.chat.hideWhispers = !CONFIG.client.extra.chat.hideWhispers;
            saveConfigAsync();
            session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cWhispers messages toggled " + (CONFIG.client.extra.chat.hideWhispers ? "off" : "on") + "&r"), false));
            return false;
        } else if (lowerCase.startsWith("toggledeathmsgs")) {
            CONFIG.client.extra.chat.hideDeathMessages = !CONFIG.client.extra.chat.hideDeathMessages;
            saveConfigAsync();
            session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cDeath messages toggled " + (CONFIG.client.extra.chat.hideDeathMessages ? "off" : "on") + "&r"), false));
            return false;
        } else if (lowerCase.startsWith("toggleconnectionmsgs")) { // todo: this command no longer exists on 2b?
            CONFIG.client.extra.chat.showConnectionMessages = !CONFIG.client.extra.chat.showConnectionMessages;
            saveConfigAsync();
            session.send(new ClientboundSystemChatPacket(MineDown.parse("&7[&9ZenithProxy&7]&r &cConnection messages toggled " + (CONFIG.client.extra.chat.showConnectionMessages ? "on" : "off") + "&r"), false));
            return false;
        }
        return true;
    }
}
