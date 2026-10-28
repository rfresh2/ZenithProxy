package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.feature.player.World;
import com.zenith.mc.block.BlockPos;
import com.zenith.util.config.Config.Client.Extra.PearlLoader.Pearl;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import lombok.Data;

import java.util.concurrent.TimeUnit;

import static com.zenith.Globals.*;
import static com.zenith.command.brigadier.BlockPosArgument.blockPos;
import static com.zenith.command.brigadier.BlockPosArgument.getBlockPos;
import static com.zenith.command.brigadier.CustomStringArgumentType.getString;
import static com.zenith.command.brigadier.CustomStringArgumentType.wordWithChars;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class PearlLoader extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("pearlLoader")
            .category(CommandCategory.MODULE)
            .description("""
           Loads player's pearls.

           Positions must be of interactable blocks like levers, buttons, trapdoors, etc.

           They should be unobstructed and reachable.
           """)
            .usageLines(
                "add <id> <x> <y> <z>",
                "del <id>",
                "clear",
                "load <id>",
                "list",
                "returnToStartPos on/off",
                "doubleClick on/off"
            )
            .aliases("pl")
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("pearlLoader")
            .then(literal("add").then(argument("id", wordWithChars()).then(argument("pos", blockPos()).executes(c -> {
                String id = getString(c, "id");
                var pos = getBlockPos(c, "pos");
                int x = pos.x();
                int y = pos.y();
                int z = pos.z();
                if (World.isChunkLoadedBlockPos(x, z)) {
                    var block = World.getBlock(x, y, z);
                    c.getSource().getEmbed()
                        .addField("Block At Position", block.name());
                }
                Pearl pearl = new Pearl(id, x, y, z);
                var pearls = CONFIG.client.extra.pearlLoader.pearls;
                for (var p : pearls) {
                    if (p.id().equals(id)) {
                        pearls.remove(p);
                        break;
                    }
                }
                pearls.add(pearl);
                c.getSource().getEmbed()
                    .title("Pearl Added")
                    .successColor();
            }))))
            .then(literal("del").then(argument("id", wordWithChars()).executes(c -> {
                String id = getString(c, "id");
                var pearls = CONFIG.client.extra.pearlLoader.pearls;
                for (var pearl : pearls) {
                    if (pearl.id().equals(id)) {
                        pearls.remove(pearl);
                        c.getSource().getEmbed()
                            .title("Pearl Removed")
                            .successColor();
                        return OK;
                    }
                }
                c.getSource().getEmbed()
                    .title("Pearl Not Found")
                    .addField("Error", "Pearl with id: " + id + " not found.", false)
                    .errorColor();
                return OK;
            })))
            .then(literal("clear").executes(c -> {
                var pearls = CONFIG.client.extra.pearlLoader.pearls;
                int sizeBefore = pearls.size();
                pearls.clear();
                c.getSource().getEmbed()
                    .title(sizeBefore + " Pearls Cleared")
                    .successColor();
            }))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("Pearls List")
                    .primaryColor();
                return OK;
            }))
            .then(literal("load").then(argument("id", wordWithChars()).executes(c -> {
                if (!Proxy.getInstance().isConnected() || Proxy.getInstance().isInQueue()) {
                    c.getSource().getEmbed()
                        .title("Can't Load Pearl")
                        .description("Bot is not online")
                        .errorColor();
                    return ERROR;
                }
                if (Proxy.getInstance().hasActivePlayer()) {
                    c.getSource().getEmbed()
                        .title("Can't Load Pearl")
                        .description("Player is controlling")
                        .errorColor();
                    return ERROR;
                }
                String id = getString(c, "id");
                var pearls = CONFIG.client.extra.pearlLoader.pearls;
                for (var pearl : pearls) {
                    if (pearl.id().equals(id)) {
                        BlockPos current = CACHE.getPlayerCache().getThePlayer().blockPos();
                        var loadAction = new PearlLoadAction(pearl, c, current);
                        loadAction.tick();
                        c.getSource().getEmbed()
                            .title("Loading Pearl")
                            .successColor();
                        return OK;
                    }
                }
                c.getSource().getEmbed()
                    .title("Pearl Not Found")
                    .addField("Error", "Pearl with id: " + id + " not found.", false)
                    .errorColor();
                return OK;
            })))
            .then(literal("returnToStartPos").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pearlLoader.returnToStartPos = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Return to Start Pos Set")
                    .primaryColor();
            })))
            .then(literal("doubleClick").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.pearlLoader.doubleClick = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Double Click Set")
                    .primaryColor();
            })));
    }

    @Data
    private static class PearlLoadAction {
        private final Pearl pearl;
        private final com.mojang.brigadier.context.CommandContext<CommandContext> c;
        private final BlockPos startPos;
        private final Timer doubleClickTimer = Timers.tickTimer();
        private State state = State.PEARL_LOAD;
        private enum State {
            PEARL_LOAD, DOUBLE_CLICK, RETURN_TO_START, END,
        }

        public void tick() {
            if (!Proxy.getInstance().isConnected() || Proxy.getInstance().hasActivePlayer()) {
                return;
            }
            switch (state) {
                case PEARL_LOAD -> {
                    BARITONE.rightClickBlock(pearl.x(), pearl.y(), pearl.z())
                        .addExecutedListener(f -> {
                            if (CONFIG.client.extra.pearlLoader.doubleClick) {
                                state = State.DOUBLE_CLICK;
                                doubleClickTimer.reset();
                            } else if (CONFIG.client.extra.pearlLoader.returnToStartPos) {
                                state = State.RETURN_TO_START;
                            } else {
                                state = State.END;
                            }
                            tick();
                        });
                }
                case DOUBLE_CLICK -> {
                    if (!doubleClickTimer.tick(5)) {
                        EXECUTOR.schedule(this::tick, 50, TimeUnit.MILLISECONDS);
                        return;
                    }
                    BARITONE.rightClickBlock(pearl.x(), pearl.y(), pearl.z())
                        .addExecutedListener(f -> {
                            if (CONFIG.client.extra.pearlLoader.returnToStartPos) {
                                state = State.RETURN_TO_START;
                            } else {
                                state = State.END;
                            }
                            tick();
                        });
                }
                case RETURN_TO_START -> {
                    BARITONE.pathTo(startPos.x(), startPos.y(), startPos.z())
                        .addExecutedListener(f -> {
                            state = State.END;
                            tick();
                        });
                }
                case END -> {
                    c.getSource().getSource().logEmbed(c.getSource(), Embed.builder()
                        .title("Pearl Loaded!")
                        .addField("Pearl ID", pearl.id(), false)
                        .successColor());
                }
            }
        }
    }

    @Override
    public void defaultEmbed(Embed embed) {
        if (embed.description() == null) {
            embed.description(pearlsList());
        }
        embed
            .addField("Return To Start Pos", toggleStr(CONFIG.client.extra.pearlLoader.returnToStartPos))
            .addField("Double Click", toggleStr(CONFIG.client.extra.pearlLoader.doubleClick));
    }

    private String pearlsList() {
        var pearls = CONFIG.client.extra.pearlLoader.pearls;
        if (pearls.isEmpty()) return "None";
        StringBuilder sb = new StringBuilder();
        for (var pearl : pearls) {
            sb.append("**")
                .append(pearl.id())
                .append("**: ");
            if (CONFIG.discord.reportCoords) {
                sb.append("||[")
                    .append(pearl.x()).append(", ").append(pearl.y()).append(", ").append(pearl.z())
                    .append("]||\n");
            } else {
                sb.append("coords disabled\n");
            }
        }
        String s = sb.toString();
        if (s.isEmpty()) return "None";
        return s.substring(0, s.length() - 1);
    }
}
