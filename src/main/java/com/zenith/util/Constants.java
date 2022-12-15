package com.zenith.util;

import com.collarmc.pounce.EventBus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zenith.cache.DataCache;
import com.zenith.client.ClientSession;
import com.zenith.client.handler.incoming.*;
import com.zenith.client.handler.incoming.entity.*;
import com.zenith.client.handler.incoming.spawn.*;
import com.zenith.client.handler.postoutgoing.PostOutgoingPlayerPositionHandler;
import com.zenith.client.handler.postoutgoing.PostOutgoingPlayerPositionRotationHandler;
import com.zenith.client.handler.postoutgoing.PostOutgoingPlayerRotationHandler;
import com.zenith.discord.DiscordBot;
import com.zenith.server.ServerConnection;
import com.zenith.server.handler.player.incoming.ServerChatHandler;
import com.zenith.server.handler.player.incoming.movement.PlayerSwingArmPacketHandler;
import com.zenith.server.handler.player.postoutgoing.ClientRequestPacketPostHandler;
import com.zenith.server.handler.player.postoutgoing.HeldItemChangePostHandler;
import com.zenith.server.handler.player.postoutgoing.JoinGamePostHandler;
import com.zenith.server.handler.shared.incoming.LoginStartHandler;
import com.zenith.server.handler.shared.incoming.ServerKeepaliveHandler;
import com.zenith.server.handler.shared.outgoing.LoginSuccessOutgoingHandler;
import com.zenith.server.handler.shared.outgoing.ServerTablistDataOutgoingHandler;
import com.zenith.server.handler.spectator.incoming.PlayerStateSpectatorHandler;
import com.zenith.server.handler.spectator.incoming.ServerChatSpectatorHandler;
import com.zenith.server.handler.spectator.incoming.movement.PlayerPositionRotationSpectatorHandler;
import com.zenith.server.handler.spectator.incoming.movement.PlayerPositionSpectatorHandler;
import com.zenith.server.handler.spectator.incoming.movement.PlayerRotationSpectatorHandler;
import com.zenith.server.handler.spectator.outgoing.*;
import com.zenith.server.handler.spectator.postoutgoing.JoinGameSpectatorPostHandler;
import com.zenith.util.handler.HandlerRegistry;
import net.daporkchop.lib.binary.oio.appendable.PAppendable;
import net.daporkchop.lib.binary.oio.reader.UTF8FileReader;
import net.daporkchop.lib.binary.oio.writer.UTF8FileWriter;
import net.daporkchop.lib.common.misc.file.PFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Constants {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Logger DEFAULT_LOG = LoggerFactory.getLogger("Proxy");
    public static final Logger AUTH_LOG = LoggerFactory.getLogger("Auth");
    public static final Logger CACHE_LOG = LoggerFactory.getLogger("Cache");
    public static final Logger CLIENT_LOG = LoggerFactory.getLogger("Client");
    public static final Logger CHAT_LOG = LoggerFactory.getLogger("Chat");
    public static final Logger MODULE_LOG = LoggerFactory.getLogger("Module");
    public static final Logger SERVER_LOG = LoggerFactory.getLogger("Server");
    public static final Logger DISCORD_LOG = LoggerFactory.getLogger("Discord");
    public static final Logger DATABASE_LOG = LoggerFactory.getLogger("Database");

    public static final File CONFIG_FILE = new File("config.json");
    public static final String SERVER_RESTARTING = "Server restarting";
    public static final String SYSTEM_DISCONNECT = "System disconnect";
    public static final String MANUAL_DISCONNECT = "Manual Disconnect";

    public static boolean isReconnectableDisconnect(final String reason) {
        return !(reason.equals(SYSTEM_DISCONNECT) || reason.equals(MANUAL_DISCONNECT));
    }

    public static Config CONFIG;
    public static final DataCache CACHE;
    public static final DiscordBot DISCORD_BOT;
    public static final EventBus EVENT_BUS;
    public static final ExecutorService MODULE_EXECUTOR_SERVICE;
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE;
    public static final WhitelistManager WHITELIST_MANAGER;


    public static final HandlerRegistry<ClientSession> CLIENT_HANDLERS = new HandlerRegistry.Builder<ClientSession>()
            .setLogger(CLIENT_LOG)
            .allowUnhandled(true)
            //
            // Inbound packets
            //
            .registerInbound(new AdvancementsHandler())
            .registerInbound(new BlockChangeHandler())
            .registerInbound(new BossBarHandler())
            .registerInbound(new ChatHandler())
            .registerInbound(new ChunkDataHandler())
            .registerInbound(new ClientKeepaliveHandler())
            .registerInbound(new ConfirmTransactionHandler())
            .registerInbound(new GameStateHandler())
            .registerInbound(new JoinGameHandler())
            .registerInbound(new LoginSuccessHandler())
            .registerInbound(new MultiBlockChangeHandler())
            .registerInbound(new PlayerHealthHandler())
            .registerInbound(new PlayerPosRotHandler())
            .registerInbound(new PlayerSetExperienceHandler())
            .registerInbound(new RespawnHandler())
            .registerInbound(new SetSlotHandler())
            .registerInbound(new SetWindowItemsHandler())
            .registerInbound(new StatisticsHandler())
            .registerInbound(new TabListDataHandler())
            .registerInbound(new TabListEntryHandler())
            .registerInbound(new TitlePacketHandler())
            .registerInbound(new UnloadChunkHandler())
            .registerInbound(new UnlockRecipesHandler())
            .registerInbound(new UpdateTileEntityHandler())
            .registerInbound(new UpdateTimePacketHandler())
            .registerInbound(new ServerCombatHandler())
            .registerInbound(new PlayerChangeHeldItemHandler())
            .registerInbound(new MapDataHandler())
            .registerInbound(new PluginMessageHandler())
            //ENTITY
            .registerInbound(new EntityAttachHandler())
            .registerInbound(new EntityCollectItemHandler())
            .registerInbound(new EntityDestroyHandler())
            .registerInbound(new EntityEffectHandler())
            .registerInbound(new EntityRemoveEffectHandler())
            .registerInbound(new EntityEquipmentHandler())
            .registerInbound(new EntityHeadLookHandler())
            .registerInbound(new EntityMetadataHandler())
            .registerInbound(new EntityPositionHandler())
            .registerInbound(new EntityPositionRotationHandler())
            .registerInbound(new EntityPropertiesHandler())
            .registerInbound(new EntityRotationHandler())
            .registerInbound(new EntitySetPassengersHandler())
            .registerInbound(new EntityTeleportHandler())
            //SPAWN
            .registerInbound(new SpawnExperienceOrbHandler())
            .registerInbound(new SpawnMobHandler())
            .registerInbound(new SpawnObjectHandler())
            .registerInbound(new SpawnPaintingPacket())
            .registerInbound(new SpawnPlayerHandler())
            .registerInbound(new SpawnPositionHandler())
            //Postoutgoing
            .registerPostOutbound(new PostOutgoingPlayerPositionHandler())
            .registerPostOutbound(new PostOutgoingPlayerPositionRotationHandler())
            .registerPostOutbound(new PostOutgoingPlayerRotationHandler())
            .build();

    public static final HandlerRegistry<ServerConnection> SERVER_PLAYER_HANDLERS = new HandlerRegistry.Builder<ServerConnection>()
            .setLogger(SERVER_LOG)
            .allowUnhandled(true)
            //
            // Inbound packets
            //
            .registerInbound(new LoginStartHandler())
            .registerInbound(new ServerChatHandler())
            .registerInbound(new ServerKeepaliveHandler())
            //PLAYER MOVEMENT
            .registerInbound(new PlayerSwingArmPacketHandler())
            //
            // Outbound packets
            //
            .registerOutbound(new LoginSuccessOutgoingHandler())
            .registerOutbound(new ServerTablistDataOutgoingHandler())
            //
            // Post-outbound packets
            //
            .registerPostOutbound(new JoinGamePostHandler())
            .registerPostOutbound(new ClientRequestPacketPostHandler())
            .registerInbound(new HeldItemChangePostHandler())
            .build();

    public static final HandlerRegistry<ServerConnection> SERVER_SPECTATOR_HANDLERS = new HandlerRegistry.Builder<ServerConnection>()
            .setLogger(SERVER_LOG)
            .allowUnhandled(false)

            .registerInbound(new LoginStartHandler())
            .registerInbound(new ServerKeepaliveHandler())
            .registerInbound(new PlayerPositionRotationSpectatorHandler())
            .registerInbound(new PlayerPositionSpectatorHandler())
            .registerInbound(new PlayerRotationSpectatorHandler())
            .registerInbound(new ServerChatSpectatorHandler())
            .registerInbound(new PlayerStateSpectatorHandler())

            .registerOutbound(new LoginSuccessOutgoingHandler())

            .registerOutbound(new ServerCloseWindowSpectatorOutgoingHandler())
            .registerOutbound(new ServerConfirmTransactionSpectatorOutgoingHandler())
            .registerOutbound(new ServerOpenTileEntityEditorSpectatorOutgoingHandler())
            .registerOutbound(new ServerOpenWindowSpectatorOutgoingHandler())
            .registerOutbound(new ServerPlayerChangeHeldItemSpectatorOutgoingHandler())
            .registerOutbound(new ServerPlayerHealthSpectatorOutgoingHandler())
            .registerOutbound(new ServerPlayerPositionRotationSpectatorOutgoingHandler())
            .registerOutbound(new ServerPlayerSetExperienceSpectatorOutgoingHandler())
            .registerOutbound(new ServerPreparedCraftingGridSpectatorOutgoingHandler())
            .registerOutbound(new ServerSetSlotSpectatorOutgoingHandler())
            .registerOutbound(new ServerVehicleMoveSpectatorOutgoingHandler())
            .registerOutbound(new ServerWindowItemsSpectatorOutgoingHandler())
            .registerOutbound(new ServerWindowPropertySpectatorOutgoingHandler())
            .registerOutbound(new ServerTablistDataOutgoingHandler())

            .registerPostOutbound(new JoinGameSpectatorPostHandler())
            .build();

    static {
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            DEFAULT_LOG.error(String.format("Uncaught exception in thread \"%s\"!", thread), e);
        });
        loadConfig();

        SHOULD_RECONNECT = CONFIG.client.extra.autoReconnect.enabled;

        CACHE = new DataCache();
        DISCORD_BOT = new DiscordBot();
        EVENT_BUS = new EventBus(Runnable::run);
        MODULE_EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(1);
        SCHEDULED_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
        WHITELIST_MANAGER = new WhitelistManager();
    }

    public static volatile boolean SHOULD_RECONNECT;

    public static synchronized void loadConfig() {
        DEFAULT_LOG.info("Loading config...");

        Config config;
        if (PFiles.checkFileExists(CONFIG_FILE)) {
            try (Reader reader = new UTF8FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, Config.class);
            } catch (IOException e) {
                throw new RuntimeException("Unable to load config!", e);
            }
        } else {
            config = new Config();
        }

        CONFIG = config.doPostLoad();
        DEFAULT_LOG.info("Config loaded.");
    }

    public static synchronized void saveConfig() {
        DEFAULT_LOG.debug("Saving config...");

        if (CONFIG == null) {
            DEFAULT_LOG.warn("Config is not set, saving default config!");
            CONFIG = new Config().doPostLoad();
        }

        try (PAppendable out = new UTF8FileWriter(PFiles.ensureFileExists(CONFIG_FILE))) {
            GSON.toJson(CONFIG, out);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save config!", e);
        }

        DEFAULT_LOG.debug("Config saved.");
    }
}
