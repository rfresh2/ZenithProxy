package com.zenith.server.handler.spectator.postoutgoing;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.zenith.cache.DataCache;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.server.ServerConnection;
import com.zenith.util.RefStrings;
import com.zenith.util.handler.HandlerRegistry;
import com.zenith.util.spectator.SpectatorHelper;
import lombok.NonNull;

import static com.github.steveice10.mc.protocol.data.game.entity.player.GameMode.SPECTATOR;
import static com.zenith.util.Constants.CACHE;
import static java.util.Arrays.asList;

public class JoinGameSpectatorPostHandler implements HandlerRegistry.PostOutgoingHandler<ServerJoinGamePacket, ServerConnection> {
    @Override
    public void accept(@NonNull ServerJoinGamePacket packet, @NonNull ServerConnection session) {
        session.send(new ServerPluginMessagePacket("MC|Brand", RefStrings.BRAND_ENCODED));
        session.send(new ServerPlayerListEntryPacket(
                PlayerListEntryAction.ADD_PLAYER,
                new PlayerListEntry[]{new PlayerListEntry(session.getProfileCache().getProfile(), SPECTATOR)}
        ));
        EntityPlayer spectatorEntityPlayer = getSpectatorPlayerEntity(session);
        session.getSpectatorPlayerCache()
                .setThePlayer(spectatorEntityPlayer)
                .setGameMode(SPECTATOR)
                .setDimension(CACHE.getPlayerCache().getDimension())
                .setDifficulty(CACHE.getPlayerCache().getDifficulty())
                .setHardcore(false)
                .setMaxPlayers(CACHE.getPlayerCache().getMaxPlayers());
        session.send(session.getSpawnPacket());
        session.send(session.getSelfEntityMetadataPacket());
        //send cached data
        DataCache.sendCacheData(CACHE.getAllDataSpectator(session.getSpectatorPlayerCache()), session);
        session.getProxy().getServerConnections().stream()
            .filter(connection -> !connection.equals(session))
            .forEach(connection -> sendInitPackets(connection, session));
        session.send(new ServerPlayerAbilitiesPacket(true, true, true, false, 0.05f, 0.1f));
        session.send(new ServerEntityMetadataPacket(session.getSpectatorSelfEntityId(), spectatorEntityPlayer.getEntityMetadataAsArray()));
        session.send(new ServerChatPacket("§9Enter playercam: \"!playercam\"§r", true));
        session.send(new ServerChatPacket("§9Hide your entity from yourself: \"!etoggle\"§r", true));
        session.send(new ServerChatPacket("§9Change your entity: \"!e <entity>\"§r", true));
        session.setLoggedIn(true);
        session.setAllowSpectatorServerPlayerPosRotate(false);
    }

    @Override
    public Class<ServerJoinGamePacket> getPacketClass() {
        return ServerJoinGamePacket.class;
    }

    // send initialization packets for spectator joining to another server connection
    private void sendInitPackets(final ServerConnection connection, final ServerConnection selfSession) {
        connection.send(new ServerChatPacket(
                "§9" + selfSession.getProfileCache().getProfile().getName() + " connected!§r", true
        ));
        if (connection.equals(selfSession.getProxy().getCurrentPlayer().get())) {
            selfSession.send(new ServerSpawnPlayerPacket(
                    CACHE.getPlayerCache().getEntityId(),
                    CACHE.getProfileCache().getProfile().getId(),
                    CACHE.getPlayerCache().getX(),
                    CACHE.getPlayerCache().getY(),
                    CACHE.getPlayerCache().getZ(),
                    CACHE.getPlayerCache().getYaw(),
                    CACHE.getPlayerCache().getPitch(),
                    CACHE.getPlayerCache().getThePlayer().getEntityMetadataAsArray()));
            connection.send(new ServerChatPacket(
                    "§9Send private messages: \"!m <message>\"§r", true
            ));
        } else {
            selfSession.send(connection.getSpawnPacket());
            selfSession.send(connection.getEntityMetadataPacket());
        }
        connection.send(selfSession.getSpawnPacket());
        connection.send(selfSession.getEntityMetadataPacket());
        SpectatorHelper.syncPlayerEquipmentWithSpectatorsFromCache(selfSession.getProxy());
    }

    private EntityPlayer getSpectatorPlayerEntity(final ServerConnection session) {
        EntityPlayer spectatorEntityPlayer = new EntityPlayer();
        spectatorEntityPlayer.setUuid(session.getProfileCache().getProfile().getId());
        spectatorEntityPlayer.setSelfPlayer(true);
        spectatorEntityPlayer.setX(CACHE.getPlayerCache().getX());
        spectatorEntityPlayer.setY(CACHE.getPlayerCache().getY());
        spectatorEntityPlayer.setZ(CACHE.getPlayerCache().getZ());
        spectatorEntityPlayer.setEntityId(session.getSpectatorSelfEntityId());
        spectatorEntityPlayer.setYaw(CACHE.getPlayerCache().getYaw());
        spectatorEntityPlayer.setPitch(CACHE.getPlayerCache().getPitch());
        final CompoundTag emptyNbtTag = new CompoundTag("");
        emptyNbtTag.clear();
        spectatorEntityPlayer.setMetadata(asList(
                new EntityMetadata(0, MetadataType.BYTE, (byte) (((byte) 0) | 0x20)),
                new EntityMetadata(1, MetadataType.INT, 0),
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
        return spectatorEntityPlayer;
    }
}
