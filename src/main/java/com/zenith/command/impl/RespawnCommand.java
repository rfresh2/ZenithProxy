package com.zenith.command.impl;

import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.command.Command;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;
import com.zenith.network.client.ClientSession;
import discord4j.rest.util.Color;

import static com.zenith.Shared.CACHE;
import static java.util.Objects.nonNull;

public class RespawnCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simple("respawn", "Performs a player respawn");
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("respawn")
            .executes(c -> {
                final ClientSession client = Proxy.getInstance().getClient();
                if (nonNull(client)) {
                    client.sendAsync(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
                }
                final EntityPlayer player = CACHE.getPlayerCache().getThePlayer();
                if (nonNull(player)) {
                    player.setHealth(20.0f);
                }
                c.getSource().getEmbedBuilder()
                    .title("Respawn performed")
                    .color(Color.CYAN);
            });
    }
}
