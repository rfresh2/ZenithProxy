package com.zenith.discord.command.impl;

import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerRespawnPacket;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.chunk.ChunkCache;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.client.ClientSession;
import com.zenith.discord.command.Command;
import com.zenith.discord.command.CommandContext;
import com.zenith.discord.command.CommandUsage;
import com.zenith.server.ServerConnection;
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
        return command("respawn")
                .executes(c -> {
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
                })
                .then(literal("fix").executes(c -> {
                    // simulates a respawn for the client. not entirely sure if this will work
                    // intention is to be able to escape situations where player thinks they're in a respawn gui
                    // but they can't actually respawn
                    // i haven't fully found the root cause of those situations yet
                    final ServerConnection serverConnection = Proxy.getInstance().getCurrentPlayer().get();
                    if (nonNull(serverConnection)) {
                        final int realDim = CACHE.getPlayerCache().getDimension();
                        int fakeDim = 1;
                        if (realDim == 1) {
                            fakeDim = -1;
                        }
                        serverConnection.sendDirect(new ServerRespawnPacket(
                                fakeDim,
                                CACHE.getPlayerCache().getDifficulty(),
                                CACHE.getPlayerCache().getGameMode(),
                                CACHE.getPlayerCache().getWorldType()));
                        serverConnection.sendDirect(new ServerRespawnPacket(
                                realDim,
                                CACHE.getPlayerCache().getDifficulty(),
                                CACHE.getPlayerCache().getGameMode(),
                                CACHE.getPlayerCache().getWorldType()));
                        ChunkCache.sync();
                        CACHE.getPlayerCache().getPackets(serverConnection::sendDirect);
                        CACHE.getEntityCache().getPackets(serverConnection::sendDirect);
                    }
                    c.getSource().getEmbedBuilder()
                            .title("Fix Respawn performed")
                            .color(Color.CYAN);
                }));
    }
}
