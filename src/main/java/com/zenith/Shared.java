package com.zenith;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zenith.cache.DataCache;
import com.zenith.command.CommandManager;
import com.zenith.database.DatabaseManager;
import com.zenith.discord.DiscordBot;
import com.zenith.event.SimpleEventBus;
import com.zenith.feature.language.LanguageManager;
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
import com.zenith.network.client.handler.incoming.level.*;
import com.zenith.network.client.handler.incoming.spawn.AddEntityHandler;
import com.zenith.network.client.handler.incoming.spawn.AddExperienceOrbHandler;
import com.zenith.network.client.handler.incoming.spawn.AddPlayerHandler;
import com.zenith.network.client.handler.incoming.spawn.SpawnPositionHandler;
import com.zenith.network.client.handler.postoutgoing.PostOutgoingPlayerPositionHandler;
import com.zenith.network.client.handler.postoutgoing.PostOutgoingPlayerPositionRotationHandler;
import com.zenith.network.client.handler.postoutgoing.PostOutgoingPlayerRotationHandler;
import com.zenith.network.client.handler.postoutgoing.PostOutgoingSetCarriedItemHandler;
import com.zenith.network.registry.HandlerRegistry;
import com.zenith.network.server.ServerConnection;
import com.zenith.network.server.handler.player.incoming.ClientSettingsPacketHandler;
import com.zenith.network.server.handler.player.incoming.PlayerPongHandler;
import com.zenith.network.server.handler.player.incoming.ServerboundChatHandler;
import com.zenith.network.server.handler.player.incoming.movement.PlayerSwingArmPacketHandler;
import com.zenith.network.server.handler.player.outgoing.SystemChatOutgoingHandler;
import com.zenith.network.server.handler.player.postoutgoing.ClientCommandPostHandler;
import com.zenith.network.server.handler.player.postoutgoing.LoginPostHandler;
import com.zenith.network.server.handler.shared.incoming.ServerboundHelloHandler;
import com.zenith.network.server.handler.shared.incoming.ServerboundKeepAliveHandler;
import com.zenith.network.server.handler.shared.outgoing.GameProfileOutgoingHandler;
import com.zenith.network.server.handler.shared.outgoing.ServerTablistDataOutgoingHandler;
import com.zenith.network.server.handler.spectator.incoming.PlayerStateSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.ServerChatSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.SpectatorPongHandler;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerPositionRotationSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerPositionSpectatorHandler;
import com.zenith.network.server.handler.spectator.incoming.movement.PlayerRotationSpectatorHandler;
import com.zenith.network.server.handler.spectator.outgoing.*;
import com.zenith.network.server.handler.spectator.postoutgoing.JoinGameSpectatorPostHandler;
import com.zenith.terminal.TerminalManager;
import com.zenith.util.Config;
import com.zenith.util.LaunchConfig;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Locale;
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
    public static final File LAUNCH_CONFIG_FILE = new File("launch_config.json");
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
    public static LaunchConfig LAUNCH_CONFIG;
    public static final DataCache CACHE;
    public static final DiscordBot DISCORD_BOT;
    public static final SimpleEventBus EVENT_BUS;
    public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE;
    public static final WhitelistManager WHITELIST_MANAGER;
    public static final PriorityBanChecker PRIORITY_BAN_CHECKER;
    public static final World WORLD;
    public static final BlockDataManager BLOCK_DATA_MANAGER;
    public static final DatabaseManager DATABASE_MANAGER;
    public static final TPSCalculator TPS_CALCULATOR;
    public static final ModuleManager MODULE_MANAGER;
    public static final Pathing PATHING;
    public static final TerminalManager TERMINAL_MANAGER;
    public static final CommandManager COMMAND_MANAGER;
    public static final LanguageManager LANGUAGE_MANAGER;
    public static final HandlerRegistry<ClientSession> CLIENT_HANDLERS = new HandlerRegistry.Builder<ClientSession>()
        .setLogger(CLIENT_LOG)
        .allowUnhandled(true)
        //
        // Inbound packets
        //
        .registerInbound(new AdvancementsHandler())
        .registerInbound(new BlockChangeHandler())
        .registerInbound(new ChangeDifficultyHandler())
        .registerInbound(new BossBarHandler())
        .registerInbound(new ChunksBiomesHandler())
        .registerInbound(new SystemChatHandler())
        .registerInbound(new PlayerChatHandler())
        .registerInbound(new LevelChunkWithLightHandler())
        .registerInbound(new LightUpdateHandler())
        .registerInbound(new ClientKeepaliveHandler())
        .registerInbound(new CommandsHandler())
        .registerInbound(new GameEventHandler())
        .registerInbound(new LoginHandler())
        .registerInbound(new GameProfileHandler())
        .registerInbound(new SectionBlocksUpdateHandler())
        .registerInbound(new SetCarriedItemHandler())
        .registerInbound(new SetChunkCacheCenterHandler())
        .registerInbound(new SetChunkCacheRadiusHandler())
        .registerInbound(new SetSimulationDistanceHandler())
        .registerInbound(new SetHealthHandler())
        .registerInbound(new SetTitleTextHandler())
        .registerInbound(new PlayerPositionHandler())
        .registerInbound(new SetExperienceHandler())
        .registerInbound(new RespawnHandler())
        .registerInbound(new ContainerSetSlotHandler())
        .registerInbound(new SetWindowItemsHandler())
        .registerInbound(new StatisticsHandler())
        .registerInbound(new TabListDataHandler())
        .registerInbound(new UpdateEnabledFeaturesHandler())
        .registerInbound(new PlayerInfoUpdateHandler())
        .registerInbound(new PlayerInfoRemoveHandler())
        .registerInbound(new SetActionBarTextHandler())
        .registerInbound(new ForgetLevelChunkHandler())
        .registerInbound(new UpdateRecipesHandler())
        .registerInbound(new UpdateTagsHandler())
        .registerInbound(new BlockEntityDataHandler())
        .registerInbound(new UpdateTimePacketHandler())
        .registerInbound(new ServerCombatHandler())
        .registerInbound(new MapDataHandler())
        .registerInbound(new PingHandler())
        .registerInbound(new PlayerAbilitiesHandler())
        .registerInbound(new PluginMessageHandler())
        //ENTITY
        .registerInbound(new EntityEventHandler())
        .registerInbound(new SetEntityLinkHandler())
        .registerInbound(new TakeItemEntityHandler())
        .registerInbound(new RemoveEntitiesHandler())
        .registerInbound(new UpdateMobEffectHandler())
        .registerInbound(new RemoveMobEffectHandler())
        .registerInbound(new SetEquipmentHandler())
        .registerInbound(new RotateHeadHandler())
        .registerInbound(new SetEntityDataHandler())
        .registerInbound(new MoveEntityPosHandler())
        .registerInbound(new MoveEntityPosRotHandler())
        .registerInbound(new UpdateAttributesHandler())
        .registerInbound(new MoveEntityRotHandler())
        .registerInbound(new EntitySetPassengersHandler())
        .registerInbound(new EntityTeleportHandler())
        //SPAWN
        .registerInbound(new AddExperienceOrbHandler())
        .registerInbound(new AddEntityHandler())
        .registerInbound(new AddPlayerHandler())
        .registerInbound(new SpawnPositionHandler())
        //Postoutgoing
        .registerPostOutbound(new PostOutgoingSetCarriedItemHandler())
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

    public static synchronized void loadLaunchConfig() {
        try {
            DEFAULT_LOG.info("Loading launch config...");

            LaunchConfig config = null;
            if (LAUNCH_CONFIG_FILE.exists()) {
                try (Reader reader = new FileReader(LAUNCH_CONFIG_FILE)) {
                    config = GSON.fromJson(reader, LaunchConfig.class);
                } catch (IOException e) {
                    saveLaunchConfig();
                }
            } else {
                saveLaunchConfig();
            }
            if (config == null) {
                if (LAUNCH_CONFIG == null) LAUNCH_CONFIG = new LaunchConfig();
            } else LAUNCH_CONFIG = config;
            CONFIG.autoUpdater.autoUpdate = LAUNCH_CONFIG.auto_update;
            DEFAULT_LOG.info("Launch config loaded.");
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to load launch config!", e);
            System.exit(1);
        }
    }

    public static synchronized void saveConfig() {
        DEFAULT_LOG.debug("Saving config...");

        if (CONFIG == null) {
            DEFAULT_LOG.warn("Config is not set, saving default config!");
            CONFIG = new Config().doPostLoad();
        }

        try {
            final File tempFile = new File(CONFIG_FILE.getAbsolutePath() + ".tmp");
            if (tempFile.exists()) tempFile.delete();
            try (Writer out = new FileWriter(tempFile)) {
                GSON.toJson(CONFIG, out);
            }
            Files.move(tempFile, CONFIG_FILE);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save config!", e);
        }

        DEFAULT_LOG.debug("Config saved.");
    }
    public static synchronized void saveLaunchConfig() {
        DEFAULT_LOG.debug("Saving launch config...");

        if (LAUNCH_CONFIG == null) {
            DEFAULT_LOG.warn("Launch config is not set, saving default config!");
            LAUNCH_CONFIG = new LaunchConfig();
        }

        try {
            final File tempFile = new File(LAUNCH_CONFIG_FILE.getAbsolutePath() + ".tmp");
            if (tempFile.exists()) tempFile.delete();
            try (Writer out = new FileWriter(tempFile)) {
                GSON.toJson(LAUNCH_CONFIG, out);
            }
            Files.move(tempFile, LAUNCH_CONFIG_FILE);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save launch config!", e);
        }

        DEFAULT_LOG.debug("Launch config saved.");
    }

    public static final HandlerRegistry<ServerConnection> SERVER_PLAYER_HANDLERS = new HandlerRegistry.Builder<ServerConnection>()
        .setLogger(SERVER_LOG)
        .allowUnhandled(true)
        //
        // Inbound packets
        //
        .registerInbound(new ServerboundHelloHandler())
        .registerInbound(new ServerboundKeepAliveHandler())
        .registerInbound(new ServerboundChatHandler())
        .registerInbound(new ClientSettingsPacketHandler())
        .registerInbound(new PlayerPongHandler())
        //PLAYER MOVEMENT
        .registerInbound(new PlayerSwingArmPacketHandler())
        //
        // Outbound packets
        //
        .registerOutbound(new GameProfileOutgoingHandler())
        .registerOutbound(new ServerTablistDataOutgoingHandler())
        .registerOutbound(new SystemChatOutgoingHandler())
        //
        // Post-outbound packets
        //
        .registerPostOutbound(new LoginPostHandler())
        .registerPostOutbound(new ClientCommandPostHandler())
        .build();
    public static final HandlerRegistry<ServerConnection> SERVER_SPECTATOR_HANDLERS = new HandlerRegistry.Builder<ServerConnection>()
        .setLogger(SERVER_LOG)
        .allowUnhandled(false)

        .registerInbound(new ServerboundHelloHandler())
        .registerInbound(new ServerboundKeepAliveHandler())
        .registerInbound(new SpectatorPongHandler())
        .registerInbound(new PlayerPositionRotationSpectatorHandler())
        .registerInbound(new PlayerPositionSpectatorHandler())
        .registerInbound(new PlayerRotationSpectatorHandler())
        .registerInbound(new ServerChatSpectatorHandler())
        .registerInbound(new PlayerStateSpectatorHandler())

        .registerOutbound(new GameProfileOutgoingHandler())

        .registerOutbound(new ClientboundContainerCloseSpectatorOutgoingHandler())
        .registerOutbound(new ClientboundContainerSetContentSpectatorOutgoingHandler())
        .registerOutbound(new PlaceGhostRecipeSpectatorOutgoingHandler())
        .registerOutbound(new OpenScreenSpectatorOutgoingHandler())
        .registerOutbound(new ClientboundSetCarriedItemSpectatorOutgoingHandler())
        .registerOutbound(new SetHealthSpectatorOutgoingHandler())
        .registerOutbound(new ClientboundPlayerPositionRotationSpectatorOutgoingHandler())
        .registerOutbound(new ClientboundSetExperienceSpectatorOutgoingHandler())
        .registerOutbound(new OpenBookSpectatorOutgoingHandler())
        .registerOutbound(new ContainerSetSlotSpectatorOutgoingHandler())
        .registerOutbound(new ClientboundVehicleMoveSpectatorOutgoingHandler())
        .registerOutbound(new HorseScreenOpenSpectatorOutgoingHandler())
        .registerOutbound(new ClientboundContainerSetDataSpectatorOutgoingHandler())
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
            EVENT_BUS = new SimpleEventBus();
            CACHE = new DataCache();
            WHITELIST_MANAGER = new WhitelistManager();
            PRIORITY_BAN_CHECKER = new PriorityBanChecker();
            BLOCK_DATA_MANAGER = new BlockDataManager();
            WORLD = new World(BLOCK_DATA_MANAGER);
            DATABASE_MANAGER = new DatabaseManager();
            TPS_CALCULATOR = new TPSCalculator();
            MODULE_MANAGER = new ModuleManager();
            PATHING = new Pathing(WORLD);
            TERMINAL_MANAGER = new TerminalManager();
            COMMAND_MANAGER = new CommandManager();
            LANGUAGE_MANAGER = new LanguageManager();
            TranslationRegistry translationRegistry = TranslationRegistry.create(Key.key("minecraft"));
            translationRegistry.registerAll(Locale.ENGLISH, LANGUAGE_MANAGER.getLanguageDataMap());
            GlobalTranslator.translator().addSource(translationRegistry);
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to initialize!", e);
            throw e;
        }
    }

}
