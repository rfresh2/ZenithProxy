package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.*;
import com.zenith.event.message.PrivateMessageSendEvent;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.EVENT_BUS;
import static com.zenith.discord.DiscordBot.escape;
import static com.zenith.util.ComponentSerializer.minimessage;

public class SendMessageCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("sendMessage")
            .category(CommandCategory.MODULE)
            .description("Sends a message in-game.")
            .usageLines(
                "<message>"
            )
            .aliases(
                "say",
                "m"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("sendMessage")
            .then(argument("message", greedyString())
                      .executes(c -> {
                          final String message = c.getArgument("message", String.class);
                          if (c.getSource().getSource() == CommandSources.PLAYER) {
                              var session = Proxy.getInstance().getCurrentPlayer().get();
                              if (session == null) return ERROR;
                              var senderName = session.getName();
                              var senderUUID = session.getUUID();
                              EVENT_BUS.postAsync(new PrivateMessageSendEvent(senderUUID, senderName, message));
                              c.getSource().setSensitiveInput(true);
                              c.getSource().setNoOutput(true);
                          } else if (c.getSource().getSource() == CommandSources.SPECTATOR) {
                              var session = c.getSource().getInGamePlayerInfo().session();
                              if (CONFIG.server.spectator.spectatorPublicChatEnabled) {
                                  Proxy.getInstance().getClient().sendAsync(new ServerboundChatPacket(message));
                                  c.getSource().getEmbed()
                                      .title("Sent Message!")
                                      .description(escape(message));
                              } else {
                                  session.sendAsyncMessage(minimessage("<red>Spectator chat disabled"));
                                  c.getSource().setNoOutput(true);
                              }
                          } else {
                              if (Proxy.getInstance().isConnected() && !message.isBlank()) {
                                  Proxy.getInstance().getClient().sendAsync(new ServerboundChatPacket(message));
                                  c.getSource().getEmbed()
                                      .title("Sent Message!")
                                      .description(escape(message));
                              } else {
                                  c.getSource().getEmbed()
                                      .title("Failed to send message");
                              }
                          }
                          return OK;
                      }));
    }
}
