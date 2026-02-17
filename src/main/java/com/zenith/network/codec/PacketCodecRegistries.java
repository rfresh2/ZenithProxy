package com.zenith.network.codec;

import com.zenith.network.client.handler.incoming.*;
import com.zenith.network.client.handler.incoming.entity.*;
import com.zenith.network.client.handler.incoming.inventory.*;
import com.zenith.network.client.handler.incoming.level.*;
import com.zenith.network.client.handler.incoming.scoreboard.*;
import com.zenith.network.client.handler.incoming.spawn.AddEntityHandler;
import com.zenith.network.client.handler.incoming.spawn.AddExperienceOrbHandler;
import com.zenith.network.client.handler.incoming.spawn.SpawnPositionHandler;
import com.zenith.network.client.handler.outgoing.*;
import com.zenith.network.client.handler.postoutgoing.*;
import com.zenith.network.server.ServerSession;
import com.zenith.network.server.handler.player.incoming.*;
import com.zenith.network.server.handler.player.outgoing.ClientCommandsOutgoingHandler;
import com.zenith.network.server.handler.player.postoutgoing.LoginPostHandler;
import com.zenith.network.server.handler.shared.incoming.*;
import com.zenith.network.server.handler.shared.outgoing.*;
import com.zenith.network.server.handler.shared.postoutgoing.*;
import com.zenith.network.server.handler.spectator.incoming.*;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerPositionRotationSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerPositionSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerRotationSpectatorHandler;
import com.zenith.network.server.handler.spectator.outgoing.*;
import com.zenith.network.server.handler.spectator.postoutgoing.LoginSpectatorPostHandler;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundKeepAlivePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundPongPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundSelectKnownPacks;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.serverbound.ServerboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddExperienceOrbPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetActionBarTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.title.ClientboundSetSubtitleTextPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundTeleportToEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.*;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundKeyPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundLoginAcknowledgedPacket;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundPongResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.ServerboundPingRequestPacket;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;

import java.util.Set;

import static com.zenith.Globals.*;

public final class PacketCodecRegistries {
    private PacketCodecRegistries() {}
    public static final CodecRegistry CLIENT_REGISTRY = new CodecRegistry("Client Handlers");
    public static final CodecRegistry SERVER_REGISTRY = new CodecRegistry("Server Handlers");
    /**
     * Packets that are not forwarded from zenith client
     */
    public static final Set<Class<? extends MinecraftPacket>> SPECTATOR_PACKET_FILTER = Set.of(
        ClientboundContainerClosePacket.class,
        ClientboundContainerSetContentPacket.class,
        ClientboundContainerSetDataPacket.class,
        ClientboundContainerSetSlotPacket.class,
        ClientboundHorseScreenOpenPacket.class,
        ClientboundMoveVehiclePacket.class,
        ClientboundOpenBookPacket.class,
        ClientboundOpenScreenPacket.class,
        ClientboundOpenSignEditorPacket.class,
        ClientboundPlaceGhostRecipePacket.class,
        ClientboundPlayerAbilitiesPacket.class,
        ClientboundPlayerPositionPacket.class,
        ClientboundSetHeldSlotPacket.class,
        ClientboundSetExperiencePacket.class,
        ClientboundSetHealthPacket.class
    );

    static {
        final PacketHandlerCodec CLIENT_CODEC = PacketHandlerCodec.clientBuilder()
            .setId("client")
            .setPriority(0)
            .state(ProtocolState.HANDSHAKE, PacketHandlerStateCodec.clientBuilder()
                .allowUnhandledInbound(false)
                .build())
            .state(ProtocolState.STATUS, PacketHandlerStateCodec.clientBuilder()
                .allowUnhandledInbound(false)
                .inbound(ClientboundStatusResponsePacket.class, new CStatusResponseHandler())
                .inbound(ClientboundPongResponsePacket.class, PongResponseHandler.INSTANCE)
                .inbound(ClientboundCookieRequestPacket.class, new CCookieRequestHandler())
                .build())
            .state(ProtocolState.LOGIN, PacketHandlerStateCodec.clientBuilder()
                .allowUnhandledInbound(false)
                .inbound(ClientboundHelloPacket.class, new CHelloHandler())
                .inbound(ClientboundLoginCompressionPacket.class, new CLoginCompressionHandler())
                .inbound(ClientboundLoginFinishedPacket.class, new CLoginFinishedHandler())
                .inbound(ClientboundLoginDisconnectPacket.class, new LoginDisconnectHandler())
                .inbound(ClientboundCustomQueryPacket.class, new CCustomQueryHandler())
                .inbound(ClientboundCookieRequestPacket.class, new CCookieRequestHandler())
                .build())
            .state(ProtocolState.CONFIGURATION, PacketHandlerStateCodec.clientBuilder()
                .allowUnhandledInbound(false)
                .inbound(ClientboundFinishConfigurationPacket.class, new CFinishConfigurationHandler())
                .inbound(ClientboundRegistryDataPacket.class, new CRegistryDataHandler())
                .inbound(ClientboundUpdateEnabledFeaturesPacket.class, new UpdateEnabledFeaturesHandler())
                .inbound(ClientboundResourcePackPushPacket.class, ResourcePackPushHandler.INSTANCE)
                .inbound(ClientboundResourcePackPopPacket.class, ResourcePackPopHandler.INSTANCE)
                .inbound(ClientboundUpdateTagsPacket.class, UpdateTagsHandler.INSTANCE)
                .inbound(ClientboundCustomPayloadPacket.class, CustomPayloadHandler.INSTANCE)
                .inbound(ClientboundPingPacket.class, PingHandler.INSTANCE)
                .inbound(ClientboundDisconnectPacket.class, CDisconnectHandler.INSTANCE)
                .inbound(ClientboundSelectKnownPacks.class, new CSelectKnownPacksHandler())
                .inbound(ClientboundStoreCookiePacket.class, new CStoreCookieHandler())
                .inbound(ClientboundCookieRequestPacket.class, new CCookieRequestHandler())
                .inbound(ClientboundTransferPacket.class, new CTransferHandler())
                .inbound(ClientboundKeepAlivePacket.class, CKeepAliveHandler.INSTANCE)
                .outbound(ServerboundKeepAlivePacket.class, OutgoingCKeepAliveHandler.INSTANCE)
                .outbound(ServerboundPongPacket.class, OutgoingCPongHandler.INSTANCE)
                .postOutbound(ServerboundFinishConfigurationPacket.class, new PostOutgoingFinishConfigurationHandler())
                .build())
            .state(ProtocolState.GAME, PacketHandlerStateCodec.clientBuilder()
                .inbound(ClientboundDisconnectPacket.class, CDisconnectHandler.INSTANCE)
                .inbound(ClientboundStartConfigurationPacket.class, new CStartConfigurationHandler())
                .inbound(ClientboundDamageEventPacket.class, new DamageEventHandler())
                .inbound(ClientboundUpdateAdvancementsPacket.class, new UpdateAdvancementsHandler())
                .inbound(ClientboundBlockUpdatePacket.class, new BlockUpdateHandler())
                .inbound(ClientboundChunkBatchFinishedPacket.class, new ChunkBatchFinishedHandler())
                .inbound(ClientboundChangeDifficultyPacket.class, new ChangeDifficultyHandler())
                .inbound(ClientboundBossEventPacket.class, new BossEventHandler())
                .inbound(ClientboundChunksBiomesPacket.class, new ChunksBiomesHandler())
                .inbound(ClientboundSystemChatPacket.class, new SystemChatHandler())
                .inbound(ClientboundPlayerChatPacket.class, new PlayerChatHandler())
                .inbound(ClientboundLevelChunkWithLightPacket.class, new LevelChunkWithLightHandler())
                .inbound(ClientboundLightUpdatePacket.class, new LightUpdateHandler())
                .inbound(ClientboundCommandsPacket.class, new CommandsHandler())
                .inbound(ClientboundGameEventPacket.class, new GameEventHandler())
                .inbound(ClientboundLoginPacket.class, new LoginHandler())
                .inbound(ClientboundSectionBlocksUpdatePacket.class, new SectionBlocksUpdateHandler())
                .inbound(ClientboundSetHeldSlotPacket.class, new SetCarriedItemHandler())
                .inbound(ClientboundSetChunkCacheCenterPacket.class, new SetChunkCacheCenterHandler())
                .inbound(ClientboundSetChunkCacheRadiusPacket.class, new SetChunkCacheRadiusHandler())
                .inbound(ClientboundSetSimulationDistancePacket.class, new SetSimulationDistanceHandler())
                .inbound(ClientboundSetHealthPacket.class, new SetHealthHandler())
                .inbound(ClientboundSetSubtitleTextPacket.class, new SetSubtitleTextHandler())
                .inbound(ClientboundPlayerPositionPacket.class, new PlayerPositionHandler())
                .inbound(ClientboundPlayerRotationPacket.class, new PlayerRotationHandler())
                .inbound(ClientboundPongResponsePacket.class, PongResponseHandler.INSTANCE)
                .inbound(ClientboundResourcePackPushPacket.class, ResourcePackPushHandler.INSTANCE)
                .inbound(ClientboundResourcePackPopPacket.class, ResourcePackPopHandler.INSTANCE)
                .inbound(ClientboundSetExperiencePacket.class, new SetExperienceHandler())
                .inbound(ClientboundRespawnPacket.class, new RespawnHandler())
                .inbound(ClientboundContainerClosePacket.class, new ContainerCloseHandler())
                .inbound(ClientboundOpenScreenPacket.class, new ContainerOpenScreenHandler())
                .inbound(ClientboundContainerSetSlotPacket.class, new ContainerSetSlotHandler())
                .inbound(ClientboundContainerSetContentPacket.class, new ContainerSetContentHandler())
                .inbound(ClientboundSetPlayerInventoryPacket.class, new SetPlayerInventoryHandler())
                .inbound(ClientboundSetCursorItemPacket.class, new SetCursorItemHandler())
                .inbound(ClientboundAwardStatsPacket.class, new AwardStatsHandler())
                .inbound(ClientboundTabListPacket.class, new TabListDataHandler())
                .inbound(ClientboundPlayerInfoUpdatePacket.class, new PlayerInfoUpdateHandler())
                .inbound(ClientboundExplodePacket.class, new ExplodeHandler())
                .inbound(ClientboundPlayerInfoRemovePacket.class, new PlayerInfoRemoveHandler())
                .inbound(ClientboundSetActionBarTextPacket.class, new SetActionBarTextHandler())
                .inbound(ClientboundSetEntityMotionPacket.class, new SetEntityMotionHandler())
                .inbound(ClientboundForgetLevelChunkPacket.class, new ForgetLevelChunkHandler())
                .inbound(ClientboundRecipeBookAddPacket.class, new RecipeBookAddHandler())
                .inbound(ClientboundRecipeBookRemovePacket.class, new RecipeBookRemoveHandler())
                .inbound(ClientboundRecipeBookSettingsPacket.class, new RecipeBookSettingsHandler())
                .inbound(ClientboundUpdateRecipesPacket.class, new UpdateRecipesHandler())
                .inbound(ClientboundUpdateTagsPacket.class, UpdateTagsHandler.INSTANCE)
                .inbound(ClientboundBlockChangedAckPacket.class, new BlockChangedAckHandler())
                .inbound(ClientboundInitializeBorderPacket.class, new WorldBorderInitializeHandler())
                .inbound(ClientboundBlockEntityDataPacket.class, new BlockEntityDataHandler())
                .inbound(ClientboundSetTimePacket.class, new SetTimeHandler())
                .inbound(ClientboundPlayerCombatKillPacket.class, new PlayerCombatKillHandler())
                .inbound(ClientboundMapItemDataPacket.class, new MapDataHandler())
                .inbound(ClientboundPingPacket.class, PingHandler.INSTANCE)
                .inbound(ClientboundPlayerAbilitiesPacket.class, new PlayerAbilitiesHandler())
                .inbound(ClientboundCustomPayloadPacket.class, CustomPayloadHandler.INSTANCE)
                .inbound(ClientboundDisguisedChatPacket.class, new DisguisedChatHandler())
                .inbound(ClientboundSetPlayerTeamPacket.class, new TeamHandler())
                .inbound(ClientboundSetObjectivePacket.class, new SetObjectiveHandler())
                .inbound(ClientboundSetDisplayObjectivePacket.class, new SetDisplayObjectiveHandler())
                .inbound(ClientboundSetScorePacket.class, new SetScoreHandler())
                .inbound(ClientboundResetScorePacket.class, new ResetScoreHandler())
                .inbound(ClientboundStoreCookiePacket.class, new CStoreCookieHandler())
                .inbound(ClientboundCookieRequestPacket.class, new CCookieRequestHandler())
                .inbound(ClientboundTransferPacket.class, new CTransferHandler())
                .inbound(ClientboundEntityEventPacket.class, new EntityEventHandler())
                .inbound(ClientboundEntityPositionSyncPacket.class, new EntityPositionSyncHandler())
                .inbound(ClientboundSetEntityLinkPacket.class, new SetEntityLinkHandler())
                .inbound(ClientboundTakeItemEntityPacket.class, new TakeItemEntityHandler())
                .inbound(ClientboundRemoveEntitiesPacket.class, new RemoveEntitiesHandler())
                .inbound(ClientboundUpdateMobEffectPacket.class, new UpdateMobEffectHandler())
                .inbound(ClientboundRemoveMobEffectPacket.class, new RemoveMobEffectHandler())
                .inbound(ClientboundSetEquipmentPacket.class, new SetEquipmentHandler())
                .inbound(ClientboundRotateHeadPacket.class, new RotateHeadHandler())
                .inbound(ClientboundSetEntityDataPacket.class, new SetEntityDataHandler())
                .inbound(ClientboundMoveEntityPosPacket.class, new MoveEntityPosHandler())
                .inbound(ClientboundMoveEntityPosRotPacket.class, new MoveEntityPosRotHandler())
                .inbound(ClientboundUpdateAttributesPacket.class, new UpdateAttributesHandler())
                .inbound(ClientboundMoveEntityRotPacket.class, new MoveEntityRotHandler())
                .inbound(ClientboundMoveMinecartPacket.class, new MoveMinecartHandler())
                .inbound(ClientboundMoveVehiclePacket.class, new MoveVehicleHandler())
                .inbound(ClientboundSetPassengersPacket.class, new EntitySetPassengersHandler())
                .inbound(ClientboundTeleportEntityPacket.class, new TeleportEntityHandler())
                .inbound(ClientboundAddExperienceOrbPacket.class, new AddExperienceOrbHandler())
                .inbound(ClientboundAddEntityPacket.class, new AddEntityHandler())
                .inbound(ClientboundSetDefaultSpawnPositionPacket.class, new SpawnPositionHandler())
                .outbound(ServerboundChatPacket.class, new OutgoingChatHandler())
                .outbound(ServerboundChatCommandSignedPacket.class, new OutgoingChatCommandSignedHandler())
                .outbound(ServerboundContainerClickPacket.class, new OutgoingContainerClickHandler())
                .inbound(ClientboundKeepAlivePacket.class, CKeepAliveHandler.INSTANCE)
                .outbound(ServerboundKeepAlivePacket.class, OutgoingCKeepAliveHandler.INSTANCE)
                .outbound(ServerboundPongPacket.class, OutgoingCPongHandler.INSTANCE)
                .outbound(ServerboundPlayerLoadedPacket.class, new OutgoingPlayerLoadedHandler())
                .postOutbound(ServerboundAcceptTeleportationPacket.class, new PostOutgoingAcceptTeleportHandler())
                .postOutbound(ServerboundConfigurationAcknowledgedPacket.class, new PostOutgoingConfigurationAckHandler())
                .postOutbound(ServerboundMoveVehiclePacket.class, new PostOutgoingMoveVehicleHandler())
                .postOutbound(ServerboundPlayerAbilitiesPacket.class, new PostOutgoingPlayerAbilitiesHandler())
                .postOutbound(ServerboundPlayerCommandPacket.class, new PostOutgoingPlayerCommandHandler())
                .postOutbound(ServerboundSetCarriedItemPacket.class, new PostOutgoingSetCarriedItemHandler())
                .postOutbound(ServerboundMovePlayerPosPacket.class, new PostOutgoingPlayerPositionHandler())
                .postOutbound(ServerboundMovePlayerPosRotPacket.class, new PostOutgoingPlayerPositionRotationHandler())
                .postOutbound(ServerboundMovePlayerRotPacket.class, new PostOutgoingPlayerRotationHandler())
                .postOutbound(ServerboundMovePlayerStatusOnlyPacket.class, new PostOutgoingPlayerStatusOnlyHandler())
                .postOutbound(ServerboundSwingPacket.class, new PostOutgoingSwingHandler())
                .postOutbound(ServerboundContainerClosePacket.class, new PostOutgoingContainerCloseHandler())
                .postOutbound(ServerboundContainerClickPacket.class, new PostOutgoingContainerClickHandler())
                .postOutbound(ServerboundPlayerActionPacket.class, new PostOutgoingPlayerActionHandler())
                .postOutbound(ServerboundSetCreativeModeSlotPacket.class, new PostOutgoingSetCreativeModeSlotHandler())
                .build())
            .build();

        final PacketHandlerCodec SERVER_PLAYER_CODEC = PacketHandlerCodec.serverBuilder()
            .setId("server-player")
            .setPriority(2)
            .setActivePredicate((connection) -> !connection.isSpectator())
            .state(ProtocolState.CONFIGURATION, PacketHandlerStateCodec.serverBuilder()
                .inbound(ServerboundKeepAlivePacket.class, SPlayerKeepAliveHandler.INSTANCE)
                .build())
            .state(ProtocolState.GAME, PacketHandlerStateCodec.serverBuilder()
                .inbound(ServerboundAcceptTeleportationPacket.class, new SAcceptTeleportHandler())
                .inbound(ServerboundMovePlayerPosRotPacket.class, new SPlayerPositionRotHandler())
                .inbound(ServerboundChatCommandPacket.class, new ChatCommandHandler())
                .inbound(ServerboundChatCommandSignedPacket.class, new SignedChatCommandHandler())
                .inbound(ServerboundChatPacket.class, new ChatHandler())
                .inbound(ServerboundCommandSuggestionPacket.class, new CommandSuggestionHandler())
                .inbound(ServerboundKeepAlivePacket.class, SPlayerKeepAliveHandler.INSTANCE)
                .inbound(ServerboundSetCarriedItemPacket.class, new SSetCarriedItemHandler())
                .outbound(ClientboundCommandsPacket.class, new ClientCommandsOutgoingHandler())
                .postOutbound(ClientboundLoginPacket.class, new LoginPostHandler())
                .build())
            .build();

        final PacketHandlerCodec SERVER_SPECTATOR_CODEC = PacketHandlerCodec.serverBuilder()
            .setId("server-spectator")
            .setPriority(1)
            .setActivePredicate(ServerSession::isSpectator)
            .state(ProtocolState.CONFIGURATION, PacketHandlerStateCodec.serverBuilder()
                .inbound(ServerboundKeepAlivePacket.class, KeepAliveSpectatorHandler.INSTANCE)
                .build())
            .state(ProtocolState.GAME, PacketHandlerStateCodec.serverBuilder()
                .allowUnhandledInbound(false)
                .inbound(ServerboundMovePlayerPosRotPacket.class, new PlayerPositionRotationSpectatorHandler())
                .inbound(ServerboundMovePlayerPosPacket.class, new PlayerPositionSpectatorHandler())
                .inbound(ServerboundMovePlayerRotPacket.class, new PlayerRotationSpectatorHandler())
                .inbound(ServerboundChatPacket.class, new ServerChatSpectatorHandler())
                .inbound(ServerboundPlayerCommandPacket.class, new PlayerCommandSpectatorHandler())
                .inbound(ServerboundTeleportToEntityPacket.class, new TeleportToEntitySpectatorHandler())
                .inbound(ServerboundInteractPacket.class, new InteractEntitySpectatorHandler())
                .inbound(ServerboundChatCommandPacket.class, new ChatCommandSpectatorHandler())
                .inbound(ServerboundCommandSuggestionPacket.class, new CommandSuggestionSpectatorHandler())
                .inbound(ServerboundChatCommandSignedPacket.class, new SignedChatCommandSpectatorHandler())
                .inbound(ServerboundKeepAlivePacket.class, KeepAliveSpectatorHandler.INSTANCE)
                .outbound(ClientboundCommandsPacket.class, new ClientCommandsSpectatorOutgoingHandler())
                .outbound(ClientboundGameEventPacket.class, new GameEventSpectatorOutgoingHandler())
                .outbound(ClientboundPlayerAbilitiesPacket.class, new PlayerAbilitiesSpectatorOutgoingHandler())
                .outbound(ClientboundRespawnPacket.class, new RespawnSpectatorOutgoingPacket())
                .outbound(ClientboundStartConfigurationPacket.class, new StartConfigurationSpectatorOutgoingHandler())
                .outbound(ClientboundLoginPacket.class, new LoginSpectatorOutgoingHandler())
                .postOutbound(ClientboundLoginPacket.class, new LoginSpectatorPostHandler())
                .build())
            .build();

        final PacketHandlerCodec SERVER_SHARED_CODEC = PacketHandlerCodec.serverBuilder()
            .setId("server-shared")
            .setPriority(0)
            .state(ProtocolState.CONFIGURATION, PacketHandlerStateCodec.serverBuilder()
                .inbound(ServerboundFinishConfigurationPacket.class, new FinishConfigurationHandler())
                .inbound(ServerboundClientInformationPacket.class, SClientInformationHandler.INSTANCE)
                .outbound(ClientboundKeepAlivePacket.class, KeepAliveOutgoingHandler.INSTANCE)
                .postOutbound(ClientboundFinishConfigurationPacket.class, new ClientFinishConfigurationPostOutgoingHandler())
                .build())
            .state(ProtocolState.HANDSHAKE, PacketHandlerStateCodec.serverBuilder()
                .inbound(ClientIntentionPacket.class, new IntentionHandler())
                .build())
            .state(ProtocolState.LOGIN, PacketHandlerStateCodec.serverBuilder()
                .inbound(ServerboundKeyPacket.class, new KeyHandler())
                .inbound(ServerboundHelloPacket.class, new SHelloHandler())
                .inbound(ServerboundCookieResponsePacket.class, new SCookieResponseHandler())
                .inbound(ServerboundLoginAcknowledgedPacket.class, new LoginAckHandler())
                .outbound(ClientboundLoginFinishedPacket.class, new SLoginFinishedOutgoingHandler())
                .postOutbound(ClientboundLoginCompressionPacket.class, new LoginCompressionPostOutgoingHandler())
                .build())
            .state(ProtocolState.STATUS, PacketHandlerStateCodec.serverBuilder()
                .inbound(ServerboundPingRequestPacket.class, PingRequestHandler.INSTANCE)
                .inbound(ServerboundStatusRequestPacket.class, new StatusRequestHandler())
                .build())
            .state(ProtocolState.GAME, PacketHandlerStateCodec.serverBuilder()
                .inbound(ServerboundConfigurationAcknowledgedPacket.class, new ConfigurationAckHandler())
                .inbound(ServerboundPongPacket.class, new PongHandler())
                .inbound(ServerboundClientInformationPacket.class, SClientInformationHandler.INSTANCE)
                .inbound(ServerboundChatSessionUpdatePacket.class, new SChatSessionUpdateHandler())
                .inbound(ServerboundPlayerLoadedPacket.class, new SPlayerLoadedHandler())
                .postOutbound(ClientboundPingPacket.class, new PingPostOutgoingHandler())
                .outbound(ClientboundTabListPacket.class, new ServerTablistDataOutgoingHandler())
                .outbound(ClientboundKeepAlivePacket.class, KeepAliveOutgoingHandler.INSTANCE)
                .outbound(ClientboundPlayerChatPacket.class, new SPlayerChatOutgoingHandler())
                .outbound(ClientboundDeleteChatPacket.class, new SDeleteChatOutgoingHandler())
                .outbound(ClientboundRespawnPacket.class, new SRespawnOutgoingHandler())
                .postOutbound(ClientboundStartConfigurationPacket.class, new ClientStartConfigurationPostOutgoingHandler())
                .postOutbound(ClientboundTransferPacket.class, new TransferPostOutgoingHandler())
                .build())
            .build();

        final PacketHandlerCodec CLIENT_PACKETLOG = new PacketLogPacketHandlerCodec(
            "client-packet-log",
            CLIENT_LOG,
            () -> CONFIG.debug.packetLog.clientPacketLog
        );

        final PacketHandlerCodec SERVER_PACKETLOG = new PacketLogPacketHandlerCodec(
            "server-packet-log",
            SERVER_LOG,
            () -> CONFIG.debug.packetLog.serverPacketLog
        );

        CLIENT_REGISTRY.register(CLIENT_CODEC);
        CLIENT_REGISTRY.register(CLIENT_PACKETLOG);
        SERVER_REGISTRY.register(SERVER_PLAYER_CODEC);
        SERVER_REGISTRY.register(SERVER_SPECTATOR_CODEC);
        SERVER_REGISTRY.register(SERVER_SHARED_CODEC);
        SERVER_REGISTRY.register(SERVER_PACKETLOG);
    }
}
