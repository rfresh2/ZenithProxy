package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.event.proxy.PrivateMessageSendEvent;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.EVENT_BUS;
import static java.util.Arrays.asList;

public class SendMessageCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simpleAliases(
            "sendMessage",
            CommandCategory.MODULE,
            "Sends a message in-game.",
            asList("say", "msg", "m"));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("sendMessage")
            .then(argument("message", greedyString())
                      .executes(c -> {
                          final String message = c.getArgument("message", String.class);
                          if (c.getSource().getSource() == CommandSource.IN_GAME_PLAYER) {
                              var session = Proxy.getInstance().getCurrentPlayer().get();
                              if (session == null) return ERROR;
                              var senderName = session.getProfileCache().getProfile().getName();
                              var senderUUID = session.getProfileCache().getProfile().getId();
                              EVENT_BUS.postAsync(new PrivateMessageSendEvent(senderUUID, senderName, message));
                              c.getSource().setSensitiveInput(true);
                              c.getSource().setNoOutput(true);
                          } else if (c.getSource().getSource() == CommandSource.SPECTATOR) {
                              var session = c.getSource().getInGamePlayerInfo().session();
                              if (CONFIG.server.spectator.spectatorPublicChatEnabled) {
                                  Proxy.getInstance().getClient().sendAsync(new ServerboundChatPacket(message));
                                  c.getSource().getEmbed()
                                      .title("Sent Message!")
                                      .description(message);
                              } else {
                                  session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>Spectator chat disabled"), false));
                                  c.getSource().setNoOutput(true);
                              }
                          } else {
                              if (Proxy.getInstance().isConnected() && !message.isBlank()) {
                                  Proxy.getInstance().getClient().sendAsync(new ServerboundChatPacket(message));
                                  c.getSource().getEmbed()
                                      .title("Sent Message!")
                                      .description(message);
                              } else {
                                  c.getSource().getEmbed()
                                      .title("Failed to send message");
                              }
                          }
                          return 1;
                      }));
    }
}
