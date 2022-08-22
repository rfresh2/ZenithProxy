package com.zenith.server.handler.spectator.postoutgoing;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.world.notify.ClientNotification;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerNotifyClientPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.RefStrings;
import com.zenith.util.Wait;
import com.zenith.util.handler.HandlerRegistry;
import lombok.NonNull;

import java.util.concurrent.ForkJoinPool;

import static com.github.steveice10.mc.protocol.data.game.entity.player.GameMode.SPECTATOR;
import static com.zenith.util.Constants.*;
import static com.zenith.util.Constants.SERVER_LOG;
import static java.util.Arrays.asList;

public class JoinGameSpectatorPostHandler implements HandlerRegistry.PostOutgoingHandler<ServerJoinGamePacket, PorkServerConnection> {
    @Override
    public void accept(@NonNull ServerJoinGamePacket packet, @NonNull PorkServerConnection session) {
        session.send(new ServerPluginMessagePacket("MC|Brand", RefStrings.BRAND_ENCODED));
        EntityPlayer spectatorEntityPlayer = new EntityPlayer();
        spectatorEntityPlayer.setUuid(session.getProfileCache().getProfile().getId());
        spectatorEntityPlayer.setSelfPlayer(true);
        spectatorEntityPlayer.setX(CACHE.getPlayerCache().getX());
        spectatorEntityPlayer.setY(CACHE.getPlayerCache().getY());
        spectatorEntityPlayer.setZ(CACHE.getPlayerCache().getZ());
        spectatorEntityPlayer.setEntityId(session.getSpectatorEntityId());
        spectatorEntityPlayer.setYaw(CACHE.getPlayerCache().getYaw());
        spectatorEntityPlayer.setPitch(CACHE.getPlayerCache().getPitch());
        final CompoundTag emptyNbtTag = new CompoundTag("");
        emptyNbtTag.clear();
        spectatorEntityPlayer.setMetadata(asList(
                new EntityMetadata(0, MetadataType.BYTE, (byte)0),
                new EntityMetadata(1, MetadataType.INT, 300),
                new EntityMetadata(2, MetadataType.STRING, ""),
                new EntityMetadata(3, MetadataType.BOOLEAN, false),
                new EntityMetadata(4, MetadataType.BOOLEAN, false),
                new EntityMetadata(5, MetadataType.BOOLEAN, false),
                new EntityMetadata(6, MetadataType.BYTE, (byte)0),
                new EntityMetadata(7, MetadataType.FLOAT, 20f),
                new EntityMetadata(8, MetadataType.INT, 0),
                new EntityMetadata(9, MetadataType.BOOLEAN, false),
                new EntityMetadata(10, MetadataType.INT, 0),
                new EntityMetadata(11, MetadataType.FLOAT, 0.0f),
                new EntityMetadata(12, MetadataType.INT, 202),
                new EntityMetadata(13, MetadataType.BYTE, (byte)0),
                new EntityMetadata(14, MetadataType.BYTE, (byte)1),
                new EntityMetadata(15, MetadataType.NBT_TAG, emptyNbtTag),
                new EntityMetadata(16, MetadataType.NBT_TAG, emptyNbtTag)));
        session.getSpectatorPlayerCache()
                .setThePlayer(spectatorEntityPlayer)
                .setGameMode(SPECTATOR)
                .setDimension(CACHE.getPlayerCache().getDimension())
                .setDifficulty(CACHE.getPlayerCache().getDifficulty())
                .setHardcore(false)
                .setMaxPlayers(CACHE.getPlayerCache().getMaxPlayers());
        //send cached data
        CACHE.getAllDataSpectator(session.getSpectatorPlayerCache()).forEach(data -> {
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
            session.send(new ServerChatPacket(
                    "Send private messages: \"!m <message>\"", true
            ));
            session.getProxy().getServerConnections().stream()
                .filter(connection -> !connection.equals(session))
                .forEach(connection -> {
                    connection.send(new ServerChatPacket(
                            session.getProfileCache().getProfile().getName() + " connected!", true
                    ));
                    connection.send(new ServerChatPacket(
                            "Send private messages: \"!m <message>\"", true
                    ));
                    connection.send(new ServerSpawnPlayerPacket(
                            session.getSpectatorEntityId(),
                            session.getProfileCache().getProfile().getId(),
                            CACHE.getPlayerCache().getX(),
                            CACHE.getPlayerCache().getY(),
                            CACHE.getPlayerCache().getZ(),
                            CACHE.getPlayerCache().getYaw(),
                            CACHE.getPlayerCache().getPitch(),
                            CACHE.getPlayerCache().getThePlayer().getEntityMetadataAsArray()));
                });
            session.send(new ServerNotifyClientPacket(ClientNotification.CHANGE_GAMEMODE, SPECTATOR));
            session.send(new ServerPlayerAbilitiesPacket(true, true, true, false, 0.05f, 0.1f));
            session.send(new ServerEntityMetadataPacket(session.getSpectatorEntityId(), spectatorEntityPlayer.getEntityMetadataAsArray()));
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
