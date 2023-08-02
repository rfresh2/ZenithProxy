package com.zenith;

import com.collarmc.pounce.EventBus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zenith.cache.DataCache;
import com.zenith.command.CommandManager;
import com.zenith.database.DatabaseManager;
import com.zenith.discord.DiscordBot;
import com.zenith.feature.autoupdater.AutoUpdater;
import com.zenith.feature.pathing.Pathing;
import com.zenith.feature.pathing.World;
import com.zenith.feature.pathing.blockdata.BlockDataManager;
import com.zenith.feature.prioban.PriorityBanChecker;
import com.zenith.feature.tps.TPSCalculator;
import com.zenith.feature.whitelist.WhitelistManager;
import com.zenith.module.ModuleManager;
import com.zenith.network.client.ClientSession;
import com.zenith.network.client.handler.incoming.*;
import com.zenith.network.client.handler.incoming.entity.*;
import com.zenith.network.client.handler.incoming.spawn.*;
import com.zenith.network.client.handler.postoutgoing.PostOutgoingPlayerChangeHeldItemHandler;
import com.zenith.network.client.handler.postoutgoing.PostOutgoingPlayerPositionHandler;
import com.zenith.network.client.handler.postoutgoing.PostOutgoingPlayerPositionRotationHandler;
import com.zenith.network.client.handler.postoutgoing.PostOutgoingPlayerRotationHandler;
import com.zenith.network.registry.HandlerRegistry;
import com.zenith.network.server.ServerConnection;
import com.zenith.network.server.handler.player.incoming.ClientSettingsPacketHandler;
import com.zenith.network.server.handler.player.incoming.ServerChatHandler;
import com.zenith.network.server.handler.player.incoming.movement.PlayerSwingArmPacketHandler;
import com.zenith.network.server.handler.player.outgoing.ServerChatOutgoingHandler;
import com.zenith.network.server.handler.player.postoutgoing.ClientRequestPacketPostHandler;
import com.zenith.network.server.handler.player.postoutgoing.JoinGamePostHandler;
import com.zenith.network.server.handler.shared.incoming.LoginStartHandler;
import com.zenith.network.server.handler.shared.incoming.ServerKeepaliveHandler;
import com.zenith.network.server.handler.shared.outgoing.LoginSuccessOutgoingHandler;
import com.zenith.network.server.handler.shared.outgoing.ServerTablistDataOutgoingHandler;
import com.zenith.network.server.handler.spectator.incoming.PlayerStateSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.ServerChatSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerPositionRotationSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerPositionSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerRotationSpectatorHandler;
import com.zenith.network.server.handler.spectator.outgoing.*;
import com.zenith.network.server.handler.spectator.postoutgoing.JoinGameSpectatorPostHandler;
import com.zenith.terminal.TerminalManager;
import com.zenith.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Shared {

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
    public static final Logger TERMINAL_LOG = LoggerFactory.getLogger("Terminal");
    public static final File CONFIG_FILE = new File("config.json");
    public static final String SERVER_RESTARTING = "Server restarting";
    public static final String SYSTEM_DISCONNECT = "System disconnect";
    public static final String MANUAL_DISCONNECT = "Manual Disconnect";
    public static final String AUTO_DISCONNECT = "AutoDisconnect";
    public static boolean isReconnectableDisconnect(final String reason) {
        if (reason.equals(SYSTEM_DISCONNECT) || reason.equals(MANUAL_DISCONNECT)) {
            return false;
        } else if (reason.equals(AUTO_DISCONNECT)) {
            return !CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect;
        } else {
            return true;
        }
    }
    public static Config CONFIG;
    public static final DataCache CACHE;
    public static final DiscordBot DISCORD_BOT;
    public static final EventBus EVENT_BUS;
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE;
    public static final WhitelistManager WHITELIST_MANAGER;
    public static final PriorityBanChecker PRIORITY_BAN_CHECKER;
    public static final World WORLD;
    public static final BlockDataManager BLOCK_DATA_MANAGER;
    public static final DatabaseManager DATABASE_MANAGER;
    public static final TPSCalculator TPS_CALCULATOR;
    public static final ModuleManager MODULE_MANAGER;
    public static final Pathing PATHING;
    public static final AutoUpdater AUTO_UPDATER;
    public static final TerminalManager TERMINAL_MANAGER;
    public static final CommandManager COMMAND_MANAGER;
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
            .registerInbound(new PlayerChangeHeldItemHandler())
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
            .registerPostOutbound(new PostOutgoingPlayerChangeHeldItemHandler())
            .registerPostOutbound(new PostOutgoingPlayerPositionHandler())
            .registerPostOutbound(new PostOutgoingPlayerPositionRotationHandler())
            .registerPostOutbound(new PostOutgoingPlayerRotationHandler())
            .build();

    public static volatile boolean SHOULD_RECONNECT;

    public static synchronized void loadConfig() {
        try {
            DEFAULT_LOG.info("Loading config...");

            Config config;
            if (CONFIG_FILE.exists()) {
                try (Reader reader = new FileReader(CONFIG_FILE)) {
                    config = GSON.fromJson(reader, Config.class);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to load config!", e);
                }
            } else {
                config = new Config();
            }

            CONFIG = config.doPostLoad();
            SHOULD_RECONNECT = CONFIG.client.extra.autoReconnect.enabled;
            DEFAULT_LOG.info("Config loaded.");
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to load config!", e);
            System.exit(1);
        }
    }

    public static synchronized void saveConfig() {
        DEFAULT_LOG.debug("Saving config...");

        if (CONFIG == null) {
            DEFAULT_LOG.warn("Config is not set, saving default config!");
            CONFIG = new Config().doPostLoad();
        }

        try (Writer out = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(CONFIG, out);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save config!", e);
        }

        DEFAULT_LOG.debug("Config saved.");
    }
    public static final HandlerRegistry<ServerConnection> SERVER_PLAYER_HANDLERS = new HandlerRegistry.Builder<ServerConnection>()
            .setLogger(SERVER_LOG)
            .allowUnhandled(true)
            //
            // Inbound packets
            //
            .registerInbound(new LoginStartHandler())
            .registerInbound(new ServerChatHandler())
            .registerInbound(new ClientSettingsPacketHandler())
            .registerInbound(new ServerKeepaliveHandler())
            //PLAYER MOVEMENT
            .registerInbound(new PlayerSwingArmPacketHandler())
            //
            // Outbound packets
            //
            .registerOutbound(new LoginSuccessOutgoingHandler())
            .registerOutbound(new ServerTablistDataOutgoingHandler())
            .registerOutbound(new ServerChatOutgoingHandler())
            //
            // Post-outbound packets
            //
            .registerPostOutbound(new JoinGamePostHandler())
            .registerPostOutbound(new ClientRequestPacketPostHandler())
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
        try {
            Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
                DEFAULT_LOG.error("Uncaught exception in thread {}", thread, e);
            });
            SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(16);
            DISCORD_BOT = new DiscordBot();
            EVENT_BUS = new EventBus(Runnable::run);
            CACHE = new DataCache();
            WHITELIST_MANAGER = new WhitelistManager();
            PRIORITY_BAN_CHECKER = new PriorityBanChecker();
            BLOCK_DATA_MANAGER = new BlockDataManager();
            WORLD = new World(BLOCK_DATA_MANAGER);
            DATABASE_MANAGER = new DatabaseManager();
            TPS_CALCULATOR = new TPSCalculator();
            MODULE_MANAGER = new ModuleManager();
            PATHING = new Pathing(WORLD);
            AUTO_UPDATER = new AutoUpdater();
            TERMINAL_MANAGER = new TerminalManager();
            COMMAND_MANAGER = new CommandManager();
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to initialize!", e);
            throw e;
        }
    }

}
