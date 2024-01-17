package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.ToggleArgumentType.getToggle;
import static com.zenith.command.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class DebugCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "debug",
            CommandCategory.MANAGE,
            "Debug settings for developers",
            asList(
                        "autoConnect on/off",
                        "packetLog on/off"
                        // todo: packet filter setting
                ));
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("debug").requires(Command::validateAccountOwner)
            .then(literal("autoconnect")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.autoConnect = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Auto Connect " + (CONFIG.client.autoConnect ? "On!" : "Off!"))
                                .color(Color.CYAN);
                            return 1;
                      })))
            .then(literal("packetlog")
                        .then(argument("toggle", toggle()).executes(c -> {
                            boolean toggle = getToggle(c, "toggle");
                            if (toggle) {
                                CONFIG.debug.packet.received = true;
                                CONFIG.debug.packet.receivedBody = true;
                                CONFIG.debug.packet.preSent = true;
                                CONFIG.debug.packet.postSent = true;
//                                CONFIG.debug.packet.postSentBody = true;
                            } else {
                                CONFIG.debug.packet.received = false;
                                CONFIG.debug.packet.postSent = false;
                                CONFIG.debug.packet.preSent = false;
                            }
                            c.getSource().getEmbed()
                                    .title("Packet Log " + (toggle ? "On!" : "Off!"))
                                    .color(Color.CYAN);
                                return 1;
                        })));
    }
}
