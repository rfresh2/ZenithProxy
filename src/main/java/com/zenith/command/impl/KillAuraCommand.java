package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.KillAura;
import com.zenith.util.config.Config;
import com.zenith.util.config.Config.Client.Extra.KillAura.WeaponMaterial;
import com.zenith.util.config.Config.Client.Extra.KillAura.WeaponType;

import java.util.stream.Collectors;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.RegistryDataArgument.entity;
import static com.zenith.command.brigadier.RegistryDataArgument.getEntity;
import static com.zenith.command.brigadier.TimeArgument.time;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class KillAuraCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("killAura")
            .category(CommandCategory.MODULE)
            .description("""
             Attacks entities near the player.

             Custom targets list: https://link.2b2t.vc/1

             Aggressive mobs are mobs that are actively targeting and attacking the player.
             """)
            .usageLines(
                "on/off",
                "attackDelay <ticks>",
                "tpsSync on/off",
                "targetPlayers on/off",
                "targetHostileMobs on/off",
                "targetHostileMobs onlyAggressive on/off",
                "targetNeutralMobs on/off",
                "targetNeutralMobs onlyAggressive on/off",
                "targetCustom on/off",
                "targetCustom add/del <entityType>",
                "weaponSwitch on/off",
                "weaponType <any/sword/axe>",
                "weaponMaterial <any/diamond/netherite>",
                "raycast on/off",
                "priority <none/nearest>"
            )
            .aliases("ka")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("killAura")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.killAura.enabled = getToggle(c, "toggle");
                MODULE.get(KillAura.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("Kill Aura " + toggleStrCaps(CONFIG.client.extra.killAura.enabled));
            }))
            .then(literal("attackDelay").then(argument("ticks", time(0, 1000)).executes(c -> {
                CONFIG.client.extra.killAura.attackDelayTicks = c.getArgument("ticks", Integer.class);
                c.getSource().getEmbed()
                    .title("Attack Delay Ticks Set!");
            })))
            .then(literal("tpsSync").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.killAura.tpsSync = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("TPS Sync " + toggleStrCaps(CONFIG.client.extra.killAura.tpsSync));
            })))
            .then(literal("targetPlayers").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.killAura.targetPlayers = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Target Players " + toggleStrCaps(CONFIG.client.extra.killAura.targetPlayers));
            })))
            .then(literal("targetHostileMobs")
                .then(literal("onlyAggressive").then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.killAura.onlyHostileAggressive = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Target Hostile Mobs Only Aggressive " + toggleStrCaps(CONFIG.client.extra.killAura.onlyHostileAggressive));
                })))
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.killAura.targetHostileMobs = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Target Mobs " + toggleStrCaps(CONFIG.client.extra.killAura.targetHostileMobs));
                })))
            .then(literal("targetNeutralMobs")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.killAura.targetNeutralMobs = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Target Neutral Mobs " + toggleStrCaps(CONFIG.client.extra.killAura.targetNeutralMobs));
                }))
                .then(literal("onlyAggressive")
                    .then(argument("toggle", toggle()).executes(c -> {
                        CONFIG.client.extra.killAura.onlyNeutralAggressive = getToggle(c, "toggle");
                        c.getSource().getEmbed()
                            .title("Target Neutral Mobs Only Aggressive " + toggleStrCaps(CONFIG.client.extra.killAura.onlyNeutralAggressive));
                    }))))
            .then(literal("weaponSwitch").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.killAura.switchWeapon = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Weapon Switching " + toggleStrCaps(CONFIG.client.extra.killAura.switchWeapon));
            })))
            .then(literal("weaponType").then(argument("type", enumStrings(WeaponType.values())).executes(c -> {
                CONFIG.client.extra.killAura.weaponType = WeaponType.valueOf(getString(c, "type").toUpperCase());
                c.getSource().getEmbed()
                    .title("Weapon Type Set");
            })))
            .then(literal("weaponMaterial").then(argument("material", enumStrings(WeaponMaterial.values())).executes(c -> {
                CONFIG.client.extra.killAura.weaponMaterial = WeaponMaterial.valueOf(getString(c, "material").toUpperCase());
                c.getSource().getEmbed()
                    .title("Weapon Material Set");
            })))
            .then(literal("targetCustom")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.killAura.targetCustom = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Target Custom " + toggleStrCaps(CONFIG.client.extra.killAura.targetCustom));
                }))
                .then(literal("add").then(argument("entityType", entity()).executes(c -> {
                    var entityData = getEntity(c, "entityType");
                    var type = entityData.mcplType();
                    if (!CONFIG.client.extra.killAura.customTargets.contains(type))
                        CONFIG.client.extra.killAura.customTargets.add(type);
                    c.getSource().getEmbed()
                        .title("Added " + type);
                })))
                .then(literal("del").then(argument("entityType", entity()).executes(c -> {
                    var entityData = getEntity(c, "entityType");
                    var type = entityData.mcplType();
                    CONFIG.client.extra.killAura.customTargets.remove(type);
                    c.getSource().getEmbed()
                        .title("Removed " + type);
                }))))
            .then(literal("raycast").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.killAura.raycast = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Raycast " + toggleStrCaps(CONFIG.client.extra.killAura.raycast));
            })))
            .then(literal("priority")
                .then(literal("none").executes(c -> {
                    CONFIG.client.extra.killAura.priority = Config.Client.Extra.KillAura.Priority.NONE;
                    c.getSource().getEmbed()
                        .title("Priority Set");
                }))
                .then(literal("nearest").executes(c -> {
                    CONFIG.client.extra.killAura.priority = Config.Client.Extra.KillAura.Priority.NEAREST;
                    c.getSource().getEmbed()
                        .title("Priority Set");
                })));
    }

    @Override
    public void defaultEmbed(Embed builder) {
        builder
            .addField("KillAura", toggleStr(CONFIG.client.extra.killAura.enabled))
            .addField("Target Players", toggleStr(CONFIG.client.extra.killAura.targetPlayers))
            .addField("Target Hostile Mobs", toggleStr(CONFIG.client.extra.killAura.targetHostileMobs) + " [onlyAggressive: " + toggleStr(CONFIG.client.extra.killAura.onlyHostileAggressive) + "]")
            .addField("Target Neutral Mobs", toggleStr(CONFIG.client.extra.killAura.targetNeutralMobs) + " [onlyAggressive: " + toggleStr(CONFIG.client.extra.killAura.onlyNeutralAggressive) + "]")
            .addField("Target Custom", toggleStr(CONFIG.client.extra.killAura.targetCustom))
            .addField("Weapon Switching", toggleStr(CONFIG.client.extra.killAura.switchWeapon))
            .addField("Weapon Type", CONFIG.client.extra.killAura.weaponType.name().toLowerCase())
            .addField("Weapon Material", CONFIG.client.extra.killAura.weaponMaterial.name().toLowerCase())
            .addField("Attack Delay Ticks", CONFIG.client.extra.killAura.attackDelayTicks)
            .addField("TPS Sync", toggleStr(CONFIG.client.extra.killAura.tpsSync))
            .addField("Raycast", toggleStr(CONFIG.client.extra.killAura.raycast))
            .addField("Priority", CONFIG.client.extra.killAura.priority.name().toLowerCase())
            .primaryColor();
        if (CONFIG.client.extra.killAura.targetCustom) {
            builder.description("**Custom Targets**\n" + CONFIG.client.extra.killAura.customTargets.stream().map(Enum::name).collect(
                Collectors.joining(", ", "[", "]")));
        }
    }
}
