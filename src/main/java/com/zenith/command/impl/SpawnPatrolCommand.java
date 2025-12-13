package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.SpawnPatrol;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.zenith.Globals.*;
import static com.zenith.command.api.CommandOutputHelper.playerListToString;
import static com.zenith.command.brigadier.BlockPosArgument.blockPos;
import static com.zenith.command.brigadier.BlockPosArgument.getBlockPos;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class SpawnPatrolCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("spawnPatrol")
            .category(CommandCategory.MODULE)
            .description("""
            Patrols spawn and paths to any player it finds, killing them if you have kill aura enabled.
            """)
            .usageLines(
                "on/off",
                "goal <x> <y> <z>",
                "maxPatrolRange <blocks>",
                "targetOnlyNakeds on/off",
                "targetOnlyBedrock on/off",
                "stickyTargeting on/off",
                "targetAttackers on/off",
                "nether on/off",
                "stuckKill on/off",
                "stuckKill seconds <seconds>",
                "stuckKill minDist <blocks>",
                "stuckKill antiStuck on/off",
                "ignore add/del <player>",
                "ignore addAll <player1,player2,...>",
                "ignore clear",
                "ignore list"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("spawnPatrol")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.spawnPatrol.enabled = getToggle(c, "toggle");
                MODULE.get(SpawnPatrol.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("SpawnPatrol " + toggleStrCaps(CONFIG.client.extra.spawnPatrol.enabled));
            }))
            .then(literal("goal").then(argument("pos", blockPos()).executes(c -> {
                var pos = getBlockPos(c, "pos");
                CONFIG.client.extra.spawnPatrol.goalX = pos.x();
                CONFIG.client.extra.spawnPatrol.goalY = pos.y();
                CONFIG.client.extra.spawnPatrol.goalZ = pos.z();
                c.getSource().getEmbed()
                    .title("Goal Set");
            })))
            .then(literal("maxPatrolRange").then(argument("blocks", integer(10)).executes(c -> {
                CONFIG.client.extra.spawnPatrol.maxPatrolRange = getInteger(c, "blocks");
                c.getSource().getEmbed()
                    .title("Max Patrol Range Set");
            })))
            .then(literal("targetOnlyNakeds").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.spawnPatrol.targetOnlyNakeds = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Target Only Nakeds " + toggleStrCaps(CONFIG.client.extra.spawnPatrol.targetOnlyNakeds));
            })))
            .then(literal("targetOnlyBedrock").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.spawnPatrol.targetOnlyBedrock = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Target Only Bedrock " + toggleStrCaps(CONFIG.client.extra.spawnPatrol.targetOnlyBedrock));
            })))
            .then(literal("stickyTargeting").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.spawnPatrol.stickyTargeting = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Sticky Targeting " + toggleStrCaps(CONFIG.client.extra.spawnPatrol.stickyTargeting));
            })))
            .then(literal("targetAttackers").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.spawnPatrol.targetAttackers = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Target Attackers " + toggleStrCaps(CONFIG.client.extra.spawnPatrol.targetAttackers));
            })))
            .then(literal("nether").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.spawnPatrol.nether = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Nether " + toggleStrCaps(CONFIG.client.extra.spawnPatrol.nether));
            })))
            .then(literal("stuckKill")
                      .then(argument("toggle", toggle()).executes(c -> {
                            CONFIG.client.extra.spawnPatrol.stuckKill = getToggle(c, "toggle");
                            c.getSource().getEmbed()
                                .title("Stuck /kill " + toggleStrCaps(CONFIG.client.extra.spawnPatrol.stuckKill));
                      }))
                      .then(literal("seconds").then(argument("seconds", integer()).executes(c -> {
                            CONFIG.client.extra.spawnPatrol.stuckKillSeconds = getInteger(c, "seconds");
                            c.getSource().getEmbed()
                                .title("Stuck /kill Seconds Set");
                      })))
                      .then(literal("minDist").then(argument("blocks", integer()).executes(c -> {
                          CONFIG.client.extra.spawnPatrol.stuckKillMinDist = getInteger(c, "blocks");
                          c.getSource().getEmbed()
                              .title("Stuck /kill MinDist Set");
                      })))
                      .then(literal("antiStuck").then(argument("toggle", toggle()).executes(c -> {
                          CONFIG.client.extra.spawnPatrol.stuckKillAntiStuck = getToggle(c, "toggle");
                          c.getSource().getEmbed()
                              .title("Stuck /kill AntiStuck " + toggleStrCaps(CONFIG.client.extra.spawnPatrol.stuckKillAntiStuck));
                      })))
            )
            .then(literal("ignore")
                      .then(literal("add").then(argument("player", wordWithChars()).executes(c -> {
                          String player = getString(c, "player");
                          PLAYER_LISTS.getSpawnPatrolIgnoreList().add(player).ifPresentOrElse(e -> {
                              c.getSource().getEmbed()
                                  .title("Added " + player + " to ignore list")
                                  .description(playerListToString(PLAYER_LISTS.getSpawnPatrolIgnoreList()));
                          }, () -> {
                              c.getSource().getEmbed()
                                  .title("Failed");
                          });
                      })))
                      .then(literal("addAll").then(argument("playerList", wordWithChars()).executes(c -> {
                          String playerList = getString(c, "playerList");
                          String[] players = playerList.split(",");
                          if (players.length == 0) {
                                c.getSource().getEmbed()
                                    .title("No players provided");
                                return OK;
                          }
                          for (String player : players) {
                              PLAYER_LISTS.getSpawnPatrolIgnoreList().add(player);
                          }
                          c.getSource().getEmbed()
                              .title("Added " + players.length + " Players to ignore list")
                              .description(playerListToString(PLAYER_LISTS.getSpawnPatrolIgnoreList()));;
                          return OK;
                      })))
                      .then(literal("del").then(argument("player", wordWithChars()).executes(c -> {
                          String player = getString(c, "player");
                          PLAYER_LISTS.getSpawnPatrolIgnoreList().remove(player);
                          c.getSource().getEmbed()
                              .title("Removed " + player + " from ignore list")
                              .description(playerListToString(PLAYER_LISTS.getSpawnPatrolIgnoreList()));
                      })))
                      .then(literal("clear").executes(c -> {
                          PLAYER_LISTS.getSpawnPatrolIgnoreList().clear();
                          c.getSource().getEmbed()
                              .title("Cleared ignore list")
                              .description(playerListToString(PLAYER_LISTS.getSpawnPatrolIgnoreList()));
                      }))
                      .then(literal("list").executes(c -> {
                            c.getSource().getEmbed()
                                .title("Ignore List")
                                .description(playerListToString(PLAYER_LISTS.getSpawnPatrolIgnoreList()));
                      })));
    }

    @Override
    public void defaultEmbed(Embed embed) {
        embed.primaryColor();
        if (!embed.isDescriptionPresent()) {
            embed
                .addField("SpawnPatrol", toggleStr(CONFIG.client.extra.spawnPatrol.enabled), false)
                .addField("Goal", CONFIG.client.extra.spawnPatrol.goalX + ", " + CONFIG.client.extra.spawnPatrol.goalY + ", " + CONFIG.client.extra.spawnPatrol.goalZ, false)
                .addField("Max Patrol Range", CONFIG.client.extra.spawnPatrol.maxPatrolRange, false)
                .addField("Target Only Nakeds", toggleStr(CONFIG.client.extra.spawnPatrol.targetOnlyNakeds), false)
                .addField("Sticky Targeting", toggleStr(CONFIG.client.extra.spawnPatrol.stickyTargeting), false)
                .addField("Target Attackers", toggleStr(CONFIG.client.extra.spawnPatrol.targetAttackers), false)
                .addField("Nether", toggleStr(CONFIG.client.extra.spawnPatrol.nether), false)
                .addField("Stuck Kill", toggleStr(CONFIG.client.extra.spawnPatrol.stuckKill), false)
                .addField("Stuck Kill Seconds", CONFIG.client.extra.spawnPatrol.stuckKillSeconds, false)
                .addField("Stuck Kill MinDist", CONFIG.client.extra.spawnPatrol.stuckKillMinDist, false)
                .addField("Stuck Kill AntiStuck", toggleStr(CONFIG.client.extra.spawnPatrol.stuckKillAntiStuck), false);
        }

    }
}
