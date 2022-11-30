package com.zenith.discord.command.impl;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.client.ClientSession;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import discord4j.rest.util.Color;

import static com.zenith.util.Constants.CACHE;
import static java.util.Objects.nonNull;

public class RespawnCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simple("respawn", "Performs a player respawn");
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("respawn").executes(c -> {
            final ClientSession client = Proxy.getInstance().getClient();
            if (nonNull(client)) {
                client.send(new ClientRequestPacket(ClientRequest.RESPAWN));
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
