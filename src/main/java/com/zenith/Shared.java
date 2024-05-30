package com.zenith;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.rfresh2.SimpleEventBus;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zenith.cache.DataCache;
import com.zenith.command.CommandManager;
import com.zenith.database.DatabaseManager;
import com.zenith.discord.DiscordBot;
import com.zenith.feature.api.crafthead.CraftheadApi;
import com.zenith.feature.api.minetools.MinetoolsApi;
import com.zenith.feature.api.minotar.MinotarApi;
import com.zenith.feature.api.mojang.MojangApi;
import com.zenith.feature.api.prioban.PriobanApi;
import com.zenith.feature.api.sessionserver.SessionServerApi;
import com.zenith.feature.api.vcapi.VcApi;
import com.zenith.feature.items.PlayerInventoryManager;
import com.zenith.feature.tps.TPSCalculator;
import com.zenith.feature.whitelist.PlayerListsManager;
import com.zenith.feature.world.Pathing;
import com.zenith.mc.block.BlockDataManager;
import com.zenith.mc.dimension.DimensionDataManager;
import com.zenith.mc.entity.EntityDataManager;
import com.zenith.mc.language.TranslationRegistryInitializer;
import com.zenith.mc.map.MapBlockColorManager;
import com.zenith.module.ModuleManager;
import com.zenith.network.server.handler.player.InGameCommandManager;
import com.zenith.terminal.TerminalManager;
import com.zenith.util.Config;
import com.zenith.util.LaunchConfig;
import com.zenith.via.ZenithViaInitializer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Shared {
    public static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new JavaTimeModule());
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
    public static final String LOGIN_FAILED = "Login Failed";
    public static Config CONFIG;
    public static LaunchConfig LAUNCH_CONFIG;
    public static final DataCache CACHE;
    public static final DiscordBot DISCORD;
    public static final SimpleEventBus EVENT_BUS;
    public static final ScheduledExecutorService EXECUTOR;
    public static final PlayerListsManager PLAYER_LISTS;
    public static final BlockDataManager BLOCK_DATA;
    public static final EntityDataManager ENTITY_DATA;
    public static final MapBlockColorManager MAP_BLOCK_COLOR;
    public static final DimensionDataManager DIMENSION_DATA;
    public static final DatabaseManager DATABASE;
    public static final TPSCalculator TPS;
    public static final ModuleManager MODULE;
    public static final Pathing PATHING;
    public static final TerminalManager TERMINAL;
    public static final InGameCommandManager IN_GAME_COMMAND;
    public static final CommandManager COMMAND;
    public static final PlayerInventoryManager INVENTORY;
    public static final VcApi VC;
    public static final MojangApi MOJANG;
    public static final SessionServerApi SESSION_SERVER;
    public static final MinetoolsApi MINETOOLS;
    public static final CraftheadApi CRAFTHEAD;
    public static final MinotarApi MINOTAR;
    public static final PriobanApi PRIOBAN;
    public static final ZenithViaInitializer VIA_INITIALIZER;
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
            PLAYER_LISTS.init();
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
                    DEFAULT_LOG.error("Unable to load launch config. Writing default config", e);
                    saveLaunchConfig();
                }
            } else {
                saveLaunchConfig();
            }
            if (config == null) {
                if (LAUNCH_CONFIG == null) LAUNCH_CONFIG = new LaunchConfig();
            } else LAUNCH_CONFIG = config;
            DEFAULT_LOG.info("Launch config loaded.");
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to load launch config!", e);
            System.exit(1);
        }
    }

    public static @Nullable String getExecutableCommit() {
        return readResourceTxt("zenith_commit.txt");
    }

    public static @Nullable String getExecutableReleaseVersion() {
        return readResourceTxt("zenith_release.txt");
    }

    private static @Nullable String readResourceTxt(final String name) {
        try (InputStream in = Shared.class.getResourceAsStream(name)) {
            if (in == null) return null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                return reader.readLine();
            }
        } catch (IOException e) {
            return null;
        }
    }
    public static void saveConfigAsync() {
        Thread.ofVirtual().name("Async Config Save").start(Shared::saveConfig);
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

    static {
        try {
            Thread.setDefaultUncaughtExceptionHandler(
                (thread, e) -> DEFAULT_LOG.error("Uncaught exception in thread {}", thread, e));
            EXECUTOR = Executors.newScheduledThreadPool(4, new ThreadFactoryBuilder()
                .setNameFormat("ZenithProxy Scheduled Executor - #%d")
                .setDaemon(true)
                .build());
            DISCORD = new DiscordBot();
            EVENT_BUS = new SimpleEventBus(Executors.newFixedThreadPool(2, new ThreadFactoryBuilder()
                .setNameFormat("ZenithProxy Async EventBus - #%d")
                .setDaemon(true)
                .build()), DEFAULT_LOG);
            DIMENSION_DATA = new DimensionDataManager();
            CACHE = new DataCache();
            PLAYER_LISTS = new PlayerListsManager();
            BLOCK_DATA = new BlockDataManager();
            ENTITY_DATA = new EntityDataManager();
            MAP_BLOCK_COLOR = new MapBlockColorManager();
            DATABASE = new DatabaseManager();
            TPS = new TPSCalculator();
            MODULE = new ModuleManager();
            PATHING = new Pathing();
            TERMINAL = new TerminalManager();
            IN_GAME_COMMAND = new InGameCommandManager();
            COMMAND = new CommandManager();
            INVENTORY = new PlayerInventoryManager();
            VC = new VcApi();
            MOJANG = new MojangApi();
            SESSION_SERVER = new SessionServerApi();
            MINETOOLS = new MinetoolsApi();
            CRAFTHEAD = new CraftheadApi();
            MINOTAR = new MinotarApi();
            PRIOBAN = new PriobanApi();
            VIA_INITIALIZER = new ZenithViaInitializer();
            TranslationRegistryInitializer.registerAllTranslations();
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to initialize!", e);
            throw e;
        }
    }

}
