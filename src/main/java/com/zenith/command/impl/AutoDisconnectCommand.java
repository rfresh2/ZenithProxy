package com.zenith.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AutoDisconnect;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static java.util.Arrays.asList;

public class AutoDisconnectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "autoDisconnect",
            CommandCategory.MODULE, """
            Configures the AutoDisconnect module.
            Modes:
            
              * Health: Disconnects when health is below a set threshold
              * Thunder: Disconnects during thunderstorms (i.e. avoid lightning burning down bases)
              * Unknown Player: Disconnects when a player not on the friends list, whitelist, or spectator whitelist is in visual range
            Multiple modes can be enabled, they are non-exclusive
            
            Global Settings:
              * WhilePlayerConnected: If AutoDisconnect should disconnect while a player is controlling the proxy account
              * AutoClientDisconnect: Disconnects when the controlling player disconnects
              * CancelAutoReconnect: Cancels AutoReconnect when AutoDisconnect is triggered. If the proxy account has prio this is ignored and AutoReconnect is always cancelled
            """,
            asList(
                        "on/off",
                        "health <integer>",
                        "thunder on/off",
                        "unknownPlayer on/off",
                        "whilePlayerConnected on/off",
                        "autoClientDisconnect on/off",
                        "cancelAutoReconnect on/off"
            ),
            asList("autoLog")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoDisconnect")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.utility.actions.autoDisconnect.enabled = getToggle(c, "toggle");
                MODULE.get(AutoDisconnect.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AutoDisconnect " + toggleStrCaps(CONFIG.client.extra.utility.actions.autoDisconnect.enabled));
                return 1;
            }))
            .then(literal("health")
                      .then(argument("healthLevel", integer(1, 19)).executes(c -> {
                          CONFIG.client.extra.utility.actions.autoDisconnect.health = IntegerArgumentType.getInteger(c, "healthLevel");
                          c.getSource().getEmbed()
                              .title("AutoDisconnect Health Updated!");
                          return 1;
                      })))
            .then(literal("cancelAutoReconnect")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("AutoDisconnect CancelAutoReconnect " + toggleStrCaps(CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect));
                            return 1;
                      })))
            .then(literal("autoClientDisconnect")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("AutoDisconnect AutoClientDisconnect " + toggleStrCaps(CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect));
                            return 1;
                      })))
            .then(literal("thunder")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.utility.actions.autoDisconnect.thunder = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("AutoDisconnect Thunder " + toggleStrCaps(CONFIG.client.extra.utility.actions.autoDisconnect.thunder));
                            return 1;
                      })))
            .then(literal("unknownPlayer")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.utility.actions.autoDisconnect.onUnknownPlayerInVisualRange = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("AutoDisconnect UnknownPlayer " + toggleStrCaps(CONFIG.client.extra.utility.actions.autoDisconnect.onUnknownPlayerInVisualRange));
                          return 1;
                      })))
            .then(literal("whilePlayerConnected")
                      .then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.utility.actions.autoDisconnect.whilePlayerConnected = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("AutoDisconnect WhilePlayerConnected " + toggleStrCaps(CONFIG.client.extra.utility.actions.autoDisconnect.whilePlayerConnected));
                          return 1;
                      })));
    }

    @Override
    public void postPopulate(final Embed builder) {
        builder
            .addField("AutoDisconnect", toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.enabled), false)
            .addField("Health", CONFIG.client.extra.utility.actions.autoDisconnect.health, false)
            .addField("Thunder", toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.thunder), false)
            .addField("UnknownPlayer", toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.onUnknownPlayerInVisualRange), false)
            .addField("WhilePlayerConnected", toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.whilePlayerConnected), false)
            .addField("AutoClientDisconnect", toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.autoClientDisconnect), false)
            .addField("CancelAutoReconnect", toggleStr(CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect), false)
            .primaryColor();
    }
}
