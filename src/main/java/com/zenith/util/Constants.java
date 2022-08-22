/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.zenith.util;

import com.collarmc.pounce.EventBus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.zenith.cache.DataCache;
import com.zenith.client.PorkClientSession;
import com.zenith.client.handler.incoming.*;
import com.zenith.client.handler.incoming.entity.*;
import com.zenith.client.handler.incoming.spawn.*;
import com.zenith.discord.DiscordBot;
import com.zenith.server.PorkServerConnection;
import com.zenith.server.handler.player.incoming.PlayerSwingArmPacketHandler;
import com.zenith.server.handler.player.incoming.ServerChatHandler;
import com.zenith.server.handler.player.incoming.movement.PlayerPositionHandler;
import com.zenith.server.handler.player.incoming.movement.PlayerPositionRotationHandler;
import com.zenith.server.handler.player.incoming.movement.PlayerRotationHandler;
import com.zenith.server.handler.player.postoutgoing.JoinGamePostHandler;
import com.zenith.server.handler.shared.incoming.LoginStartHandler;
import com.zenith.server.handler.shared.incoming.ServerKeepaliveHandler;
import com.zenith.server.handler.shared.outgoing.LoginSuccessOutgoingHandler;
import com.zenith.server.handler.spectator.incoming.ServerSpectatorChatHandler;
import com.zenith.server.handler.spectator.incoming.movement.PlayerPositionRotationSpectatorHandler;
import com.zenith.server.handler.spectator.incoming.movement.PlayerPositionSpectatorHandler;
import com.zenith.server.handler.spectator.incoming.movement.PlayerRotationSpectatorHandler;
import com.zenith.server.handler.spectator.outgoing.ServerOpenWindowSpectatorOutgoingHandler;
import com.zenith.server.handler.spectator.postoutgoing.JoinGameSpectatorPostHandler;
import com.zenith.util.handler.HandlerRegistry;
import com.zenith.websocket.WebSocketServer;
import net.daporkchop.lib.binary.oio.appendable.PAppendable;
import net.daporkchop.lib.binary.oio.reader.UTF8FileReader;
import net.daporkchop.lib.binary.oio.writer.UTF8FileWriter;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.logging.LogAmount;
import net.daporkchop.lib.logging.Logger;
import net.daporkchop.lib.logging.Logging;
import net.daporkchop.lib.logging.impl.DefaultLogger;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author DaPorkchop_
 */
public class Constants {
    public static final String VERSION = "0.2.8";

    public static final JsonParser JSON_PARSER = new JsonParser();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final DefaultLogger DEFAULT_LOG = Logging.logger;
    public static final Logger AUTH_LOG = DEFAULT_LOG.channel("Auth");
    public static final Logger CACHE_LOG = DEFAULT_LOG.channel("Cache");
    public static final Logger CLIENT_LOG = DEFAULT_LOG.channel("Client");
    public static final Logger CHAT_LOG = DEFAULT_LOG.channel("Chat");
    public static final Logger GUI_LOG = DEFAULT_LOG.channel("GUI");
    public static final Logger MODULE_LOG = DEFAULT_LOG.channel("Module");
    public static final Logger SERVER_LOG = DEFAULT_LOG.channel("Server");
    public static final Logger WEBSOCKET_LOG = DEFAULT_LOG.channel("WebSocket");
    public static final Logger DISCORD_LOG = DEFAULT_LOG.channel("Discord");

    public static final File CONFIG_FILE = new File("config.json");
    public static final String SERVER_RESTARTING = "Server restarting";
    public static final String SYSTEM_DISCONNECT = "System disconnect";
    public static final String MANUAL_DISCONNECT = "Manual Disconnect";

    public static boolean isReconnectableDisconnect(final String reason) {
        return !(reason.equals(SYSTEM_DISCONNECT) || reason.equals(MANUAL_DISCONNECT));
    }

    public static Config CONFIG;
    public static final DataCache CACHE;
    public static final WebSocketServer WEBSOCKET_SERVER;
    public static final DiscordBot DISCORD_BOT;
    public static final EventBus EVENT_BUS;
    public static final ExecutorService MODULE_EXECUTOR_SERVICE;


    public static final HandlerRegistry<PorkClientSession> CLIENT_HANDLERS = new HandlerRegistry.Builder<PorkClientSession>()
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
            .registerInbound(new UnloadChunkHandler())
            .registerInbound(new UnlockRecipesHandler())
            .registerInbound(new UpdateTileEntityHandler())
            .registerInbound(new ServerCombatHandler())
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
            .build();

    public static final HandlerRegistry<PorkServerConnection> SERVER_PLAYER_HANDLERS = new HandlerRegistry.Builder<PorkServerConnection>()
            .setLogger(SERVER_LOG)
            .allowUnhandled(true)
            //
            // Inbound packets
            //
            .registerInbound(new LoginStartHandler())
            .registerInbound(new ServerChatHandler())
            .registerInbound(new ServerKeepaliveHandler())
            //PLAYER MOVEMENT
            .registerInbound(new PlayerPositionHandler())
            .registerInbound(new PlayerPositionRotationHandler())
            .registerInbound(new PlayerRotationHandler())
            .registerInbound(new PlayerSwingArmPacketHandler())
            //
            // Outbound packets
            //
            .registerOutbound(new LoginSuccessOutgoingHandler())
            //
            // Post-outbound packets
            //
            .registerPostOutbound(new JoinGamePostHandler())
            .build();

    public static final HandlerRegistry<PorkServerConnection> SERVER_SPECTATOR_HANDLERS = new HandlerRegistry.Builder<PorkServerConnection>()
            .setLogger(SERVER_LOG)
            .allowUnhandled(false)

            .registerInbound(new LoginStartHandler())
            .registerInbound(new ServerKeepaliveHandler())
            .registerInbound(new ServerSpectatorChatHandler())
            .registerInbound(new PlayerPositionRotationSpectatorHandler())
            .registerInbound(new PlayerPositionSpectatorHandler())
            .registerInbound(new PlayerRotationSpectatorHandler())
            .registerInbound(new ServerSpectatorChatHandler())

            .registerOutbound(new LoginSuccessOutgoingHandler())
            .registerOutbound(new ServerOpenWindowSpectatorOutgoingHandler())

            .registerPostOutbound(new JoinGameSpectatorPostHandler())
            .build();

    static {
        String date = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss").format(Date.from(Instant.now()));
        File logFolder = PFiles.ensureDirectoryExists(new File("log"));
        DEFAULT_LOG.addFile(new File(logFolder, String.format("%s.log", date)), LogAmount.NORMAL)
                .enableANSI()
                .setFormatParser(AutoMCFormatParser.DEFAULT)
                .setLogAmount(LogAmount.NORMAL);

        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            DEFAULT_LOG.alert(String.format("Uncaught exception in thread \"%s\"!", thread), e);
        });

        loadConfig();

        if (CONFIG.log.printDebug)  {
            DEFAULT_LOG.setLogAmount(LogAmount.DEBUG);
        }
        if (CONFIG.log.storeDebug) {
            DEFAULT_LOG.addFile(new File(logFolder, String.format("%s-debug.log", date)), LogAmount.DEBUG);
        }

        SHOULD_RECONNECT = CONFIG.client.extra.autoReconnect.enabled;

        CACHE = new DataCache();
        WEBSOCKET_SERVER = new WebSocketServer();
        DISCORD_BOT = new DiscordBot();
        EVENT_BUS = new EventBus(Runnable::run);
        MODULE_EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(1);
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
        DEFAULT_LOG.info("Saving config...");

        if (CONFIG == null) {
            DEFAULT_LOG.warn("Config is not set, saving default config!");
            CONFIG = new Config().doPostLoad();
        }

        try (PAppendable out = new UTF8FileWriter(PFiles.ensureFileExists(CONFIG_FILE))) {
            GSON.toJson(CONFIG, out);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save config!", e);
        }

        DEFAULT_LOG.info("Config saved.");
    }
}
