package com.zenith;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.rfresh2.SimpleEventBus;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zenith.cache.DataCache;
import com.zenith.command.CommandManager;
import com.zenith.database.DatabaseManager;
import com.zenith.discord.DiscordBot;
import com.zenith.feature.api.minetools.MinetoolsApi;
import com.zenith.feature.api.mojang.MojangApi;
import com.zenith.feature.api.prioban.PriobanApi;
import com.zenith.feature.api.sessionserver.SessionServerApi;
import com.zenith.feature.api.vcapi.VcApi;
import com.zenith.feature.food.FoodManager;
import com.zenith.feature.items.ItemsManager;
import com.zenith.feature.language.LanguageManager;
import com.zenith.feature.pathing.Pathing;
import com.zenith.feature.pathing.blockdata.BlockDataManager;
import com.zenith.feature.tps.TPSCalculator;
import com.zenith.feature.whitelist.PlayerListsManager;
import com.zenith.module.ModuleManager;
import com.zenith.network.server.handler.player.InGameCommandManager;
import com.zenith.terminal.TerminalManager;
import com.zenith.util.Config;
import com.zenith.util.LaunchConfig;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@UtilityClass
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
    public static boolean isReconnectableDisconnect(final String reason) {
        if (reason.equals(SYSTEM_DISCONNECT)
            || reason.equals(MANUAL_DISCONNECT)
            || reason.equals(MinecraftConstants.SERVER_CLOSING_MESSAGE)
            || reason.equals(LOGIN_FAILED)
        ) {
            return false;
        } else if (reason.equals(AUTO_DISCONNECT)) {
            return (!CONFIG.client.extra.utility.actions.autoDisconnect.cancelAutoReconnect && !Proxy.getInstance().isPrio());
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
    public static final PlayerListsManager PLAYER_LISTS;
    public static final BlockDataManager BLOCK_DATA_MANAGER;
    public static final DatabaseManager DATABASE_MANAGER;
    public static final TPSCalculator TPS_CALCULATOR;
    public static final ModuleManager MODULE_MANAGER;
    public static final Pathing PATHING;
    public static final TerminalManager TERMINAL_MANAGER;
    public static final InGameCommandManager IN_GAME_COMMAND_MANAGER;
    public static final CommandManager COMMAND_MANAGER;
    public static final LanguageManager LANGUAGE_MANAGER;
    public static final FoodManager FOOD_MANAGER;
    public static final ItemsManager ITEMS_MANAGER;
    public static final VcApi VC_API;
    public static final MojangApi MOJANG_API;
    public static final SessionServerApi SESSION_SERVER_API;
    public static final MinetoolsApi MINETOOLS_API;
    public static final PriobanApi PRIOBAN_API;
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
            Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
                DEFAULT_LOG.error("Uncaught exception in thread {}", thread, e);
            });
            SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(4, new ThreadFactoryBuilder()
                .setNameFormat("ZenithProxy Scheduled Executor - #%d")
                .setDaemon(true)
                .build());
            DISCORD_BOT = new DiscordBot();
            EVENT_BUS = new SimpleEventBus(Executors.newFixedThreadPool(2, new ThreadFactoryBuilder()
                .setNameFormat("ZenithProxy Async EventBus - #%d")
                .setDaemon(true)
                .build()), DEFAULT_LOG);
            CACHE = new DataCache();
            PLAYER_LISTS = new PlayerListsManager();
            BLOCK_DATA_MANAGER = new BlockDataManager();
            DATABASE_MANAGER = new DatabaseManager();
            TPS_CALCULATOR = new TPSCalculator();
            MODULE_MANAGER = new ModuleManager();
            PATHING = new Pathing();
            TERMINAL_MANAGER = new TerminalManager();
            IN_GAME_COMMAND_MANAGER = new InGameCommandManager();
            COMMAND_MANAGER = new CommandManager();
            LANGUAGE_MANAGER = new LanguageManager();
            FOOD_MANAGER = new FoodManager();
            ITEMS_MANAGER = new ItemsManager();
            VC_API = new VcApi();
            MOJANG_API = new MojangApi();
            SESSION_SERVER_API = new SessionServerApi();
            MINETOOLS_API = new MinetoolsApi();
            PRIOBAN_API = new PriobanApi();
            TranslationRegistry translationRegistry = TranslationRegistry.create(Key.key("minecraft"));
            translationRegistry.registerAll(Locale.ENGLISH, LANGUAGE_MANAGER.getLanguageDataMap());
            GlobalTranslator.translator().addSource(translationRegistry);
        } catch (final Throwable e) {
            DEFAULT_LOG.error("Unable to initialize!", e);
            throw e;
        }
    }

}
