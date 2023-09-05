package com.zenith.command.impl;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;

import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static java.util.Arrays.asList;

public class SendMessageCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simpleAliases("sendMessage", "Sends a message in-game.", asList("say", "msg", "m"));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("sendMessage")
                .then(argument("message", greedyString())
                        .executes(c -> {
                            final String message = c.getArgument("message", String.class);
                            if (Proxy.getInstance().isConnected() && !message.isBlank()) {
                                Proxy.getInstance().getClient().send(new ServerboundChatPacket(message));
                                c.getSource().getEmbedBuilder()
                                        .title("Sent Message!")
                                        .description(message);
                            } else {
                                c.getSource().getEmbedBuilder()
                                        .title("Failed to send message");
                            }
                            return 1;
                        }));
    }

    @Override
    public List<String> aliases() {
        return asList("msg", "m");
    }
}
