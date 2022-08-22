package com.zenith.server.handler.spectator.postoutgoing;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.RefStrings;
import com.zenith.util.Wait;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import java.util.concurrent.ForkJoinPool;

import static com.zenith.util.Constants.*;
import static com.zenith.util.Constants.SERVER_LOG;

public class JoinGameSpectatorPostHandler implements HandlerRegistry.PostOutgoingHandler<ServerJoinGamePacket, PorkServerConnection> {
    @Override
    public void accept(@NonNull ServerJoinGamePacket packet, @NonNull PorkServerConnection session) {
        session.send(new ServerPluginMessagePacket("MC|Brand", RefStrings.BRAND_ENCODED));

        //send cached data
        CACHE.getAllData().forEach(data -> {
            if (CONFIG.debug.server.cache.sendingmessages) {
                String msg = data.getSendingMessage();
                if (msg == null)    {
                    SERVER_LOG.debug("Sending data to spectator %s", data.getClass().getCanonicalName());
                } else {
                    SERVER_LOG.debug(msg);
                }
            }
            data.getPackets(p -> {
                if (p instanceof ServerBlockChangePacket || p instanceof ServerUpdateTileEntityPacket) {
                    return;
                }
                session.send(p);
            });
            ForkJoinPool.commonPool().submit(() -> {
                // client needs to receive chunks first.
                // this wait is kinda arbitrary and may be too short or long for some clients
                // likely dependent on client net speed
                // we don't have a good hook into when the client is done receiving chunks though.
                // waiting too long will appear as though chunks are visibly updating for client during play
                Wait.waitALittle(1);
                data.getPackets(p -> {
                    if (p instanceof ServerBlockChangePacket || p instanceof ServerUpdateTileEntityPacket) {
                        session.send(p);
                    }
                });
            });
        });
        try {
            session.send(new ServerSpawnPlayerPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getProfileCache().getProfile().getId(),
                    CACHE.getPlayerCache().getX(),
                    CACHE.getPlayerCache().getY(),
                    CACHE.getPlayerCache().getZ(),
                    CACHE.getPlayerCache().getYaw(),
                    CACHE.getPlayerCache().getPitch(),
                    CACHE.getPlayerCache().getThePlayer().getEntityMetadataAsArray()));
            session.getProxy().getCurrentPlayer().get().send(
                    new ServerSpawnPlayerPacket(
                            session.getSpectatorEntityId(),
                            session.getProfileCache().getProfile().getId(),
                            CACHE.getPlayerCache().getX(),
                            CACHE.getPlayerCache().getY(),
                            CACHE.getPlayerCache().getZ(),
                            CACHE.getPlayerCache().getYaw(),
                            CACHE.getPlayerCache().getPitch(),
                            CACHE.getPlayerCache().getThePlayer().getEntityMetadataAsArray()));
            EntityPlayer spectatorEntityPlayer = new EntityPlayer();
            spectatorEntityPlayer.setUuid(session.getProfileCache().getProfile().getId());
            spectatorEntityPlayer.setSelfPlayer(true);
            spectatorEntityPlayer.setX(CACHE.getPlayerCache().getX());
            spectatorEntityPlayer.setY(CACHE.getPlayerCache().getY());
            spectatorEntityPlayer.setZ(CACHE.getPlayerCache().getZ());
            spectatorEntityPlayer.setEntityId(session.getSpectatorEntityId());
            spectatorEntityPlayer.setYaw(CACHE.getPlayerCache().getYaw());
            spectatorEntityPlayer.setPitch(CACHE.getPlayerCache().getPitch());
            spectatorEntityPlayer.setMetadata(CACHE.getPlayerCache().getThePlayer().getMetadata());
            session.getSpectatorPlayerCache()
                    .setThePlayer(new EntityPlayer())
                    .setGameMode(GameMode.SPECTATOR)
                    .setDimension(CACHE.getPlayerCache().getDimension())
                    .setDifficulty(CACHE.getPlayerCache().getDifficulty())
                    .setHardcore(false)
                    .setMaxPlayers(CACHE.getPlayerCache().getMaxPlayers());
            session.send(new ServerPlayerAbilitiesPacket(true, true, true, false, 0.05f, 0.1f));

            session.setLoggedIn(true);
        } catch (final Exception e) {
            SERVER_LOG.error(e);
        }

    }

    @Override
    public Class<ServerJoinGamePacket> getPacketClass() {
        return ServerJoinGamePacket.class;
    }
}
