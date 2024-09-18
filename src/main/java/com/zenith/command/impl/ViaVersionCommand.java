package com.zenith.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;

import static com.zenith.Shared.CONFIG;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class ViaVersionCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.args(
            "via",
            CommandCategory.MODULE,
            """
             Configure the integrated ViaVersion module.
             
             `zenithToServer` -> ZenithProxy connecting to the MC server
             `playerToZenith` -> players connecting to ZenithProxy
             """,
            asList(
                "zenithToServer on/off",
                "zenithToServer disableOn2b2t on/off",
                "zenithToServer autoConfig on/off",
                "zenithToServer version <MC version>",
                "playerToZenith on/off"
            )
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("via")
            .then(literal("zenithToServer")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.viaversion.enabled = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Zenith To Server ViaVersion " + toggleStrCaps(CONFIG.client.viaversion.enabled));
                          return 1;
                      }))
                      .then(literal("disableOn2b2t")
                                .then(argument("toggle", toggle()).executes(c -> {
                                    CONFIG.client.viaversion.disableOn2b2t = getToggle(c, "toggle");
                                    c.getSource().getEmbed()
                                        .title("Zenith To Server Disable On 2b2t " + toggleStrCaps(CONFIG.client.viaversion.disableOn2b2t));
                                    return OK;
                                })))
                      .then(literal("autoConfig")
                                .then(argument("toggle", toggle()).executes(c -> {
                                    CONFIG.client.viaversion.autoProtocolVersion = getToggle(c, "toggle");
                                    c.getSource().getEmbed()
                                        .title("Zenith To Server ViaVersion AutoConfig " + toggleStrCaps(CONFIG.client.viaversion.autoProtocolVersion));
                                    return 1;
                                })))
                      .then(literal("version")
                                .then(argument("version", wordWithChars()).executes(c -> {
                                    final String version = StringArgumentType.getString(c, "version");
                                    ProtocolVersion closest = ProtocolVersion.getClosest(version);
                                    if (closest == null) {
                                        c.getSource().getEmbed()
                                            .title("Invalid Version!")
                                            .description("Please select a valid version. Example: 1.19.4")
                                            .errorColor();
                                    } else {
                                        CONFIG.client.viaversion.protocolVersion = closest.getVersion();
                                        c.getSource().getEmbed()
                                            .title("Zenith To Server ViaVersion Version Updated!");
                                    }
                                    return 1;
                                }))))
            .then(literal("playerToZenith")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.server.viaversion.enabled = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Player To Zenith ViaVersion " + toggleStrCaps(CONFIG.server.viaversion.enabled));
                          return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed embedBuilder) {
        embedBuilder
            .addField("Zenith To Server ViaVersion", toggleStr(CONFIG.client.viaversion.enabled), false)
            .addField("Zenith To Server Disable On 2b2t", toggleStr(CONFIG.client.viaversion.disableOn2b2t), false)
            .addField("Zenith To Server AutoConfig", toggleStr(CONFIG.client.viaversion.autoProtocolVersion), false)
            .addField("Zenith To Server Version", ProtocolVersion.getProtocol(CONFIG.client.viaversion.protocolVersion).getName(), false)
            .addField("Player To Zenith ViaVersion", toggleStr(CONFIG.server.viaversion.enabled), false)
            .primaryColor();
    }}
