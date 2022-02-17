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
import com.zenith.discord.DiscordBot;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.binary.oio.appendable.PAppendable;
import net.daporkchop.lib.binary.oio.reader.UTF8FileReader;
import net.daporkchop.lib.binary.oio.writer.UTF8FileWriter;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.logging.LogAmount;
import net.daporkchop.lib.logging.Logger;
import net.daporkchop.lib.logging.Logging;
import net.daporkchop.lib.logging.impl.DefaultLogger;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;
import com.zenith.client.PorkClientSession;
import com.zenith.client.handler.incoming.AdvancementsHandler;
import com.zenith.client.handler.incoming.BlockChangeHandler;
import com.zenith.client.handler.incoming.BossBarHandler;
import com.zenith.client.handler.incoming.ChatHandler;
import com.zenith.client.handler.incoming.ChunkDataHandler;
import com.zenith.client.handler.incoming.ClientKeepaliveHandler;
import com.zenith.client.handler.incoming.GameStateHandler;
import com.zenith.client.handler.incoming.JoinGameHandler;
import com.zenith.client.handler.incoming.LoginSuccessHandler;
import com.zenith.client.handler.incoming.MultiBlockChangeHandler;
import com.zenith.client.handler.incoming.PlayerHealthHandler;
import com.zenith.client.handler.incoming.PlayerPosRotHandler;
import com.zenith.client.handler.incoming.RespawnHandler;
import com.zenith.client.handler.incoming.SetSlotHandler;
import com.zenith.client.handler.incoming.SetWindowItemsHandler;
import com.zenith.client.handler.incoming.StatisticsHandler;
import com.zenith.client.handler.incoming.TabListDataHandler;
import com.zenith.client.handler.incoming.TabListEntryHandler;
import com.zenith.client.handler.incoming.UnloadChunkHandler;
import com.zenith.client.handler.incoming.UnlockRecipesHandler;
import com.zenith.client.handler.incoming.UpdateTileEntityHandler;
import com.zenith.client.handler.incoming.entity.EntityAttachHandler;
import com.zenith.client.handler.incoming.entity.EntityCollectItemHandler;
import com.zenith.client.handler.incoming.entity.EntityDestroyHandler;
import com.zenith.client.handler.incoming.entity.EntityEffectHandler;
import com.zenith.client.handler.incoming.entity.EntityEquipmentHandler;
import com.zenith.client.handler.incoming.entity.EntityHeadLookHandler;
import com.zenith.client.handler.incoming.entity.EntityMetadataHandler;
import com.zenith.client.handler.incoming.entity.EntityPositionHandler;
import com.zenith.client.handler.incoming.entity.EntityPositionRotationHandler;
import com.zenith.client.handler.incoming.entity.EntityPropertiesHandler;
import com.zenith.client.handler.incoming.entity.EntityRemoveEffectListener;
import com.zenith.client.handler.incoming.entity.EntityRotationHandler;
import com.zenith.client.handler.incoming.entity.EntitySetPassengersHandler;
import com.zenith.client.handler.incoming.entity.EntityTeleportHandler;
import com.zenith.client.handler.incoming.spawn.SpawnExperienceOrbHandler;
import com.zenith.client.handler.incoming.spawn.SpawnMobHandler;
import com.zenith.client.handler.incoming.spawn.SpawnObjectHandler;
import com.zenith.client.handler.incoming.spawn.SpawnPaintingPacket;
import com.zenith.client.handler.incoming.spawn.SpawnPlayerHandler;
import com.zenith.server.PorkServerConnection;
import com.zenith.server.handler.incoming.LoginStartHandler;
import com.zenith.server.handler.incoming.ServerChatHandler;
import com.zenith.server.handler.incoming.ServerKeepaliveHandler;
import com.zenith.server.handler.incoming.movement.PlayerPositionHandler;
import com.zenith.server.handler.incoming.movement.PlayerPositionRotationHandler;
import com.zenith.server.handler.incoming.movement.PlayerRotationHandler;
import com.zenith.server.handler.outgoing.LoginSuccessOutgoingHandler;
import com.zenith.server.handler.postoutgoing.JoinGamePostHandler;
import com.zenith.util.cache.DataCache;
import com.zenith.util.handler.HandlerRegistry;
import com.zenith.websocket.WebSocketServer;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

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

    public static Config CONFIG;
    public static final DataCache CACHE;
    public static final WebSocketServer WEBSOCKET_SERVER;
    public static final DiscordBot DISCORD_BOT;
    public static final EventBus EVENT_BUS;

    public static final HandlerRegistry<PorkClientSession> CLIENT_HANDLERS = new HandlerRegistry.Builder<PorkClientSession>()
            .setLogger(CLIENT_LOG)
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
            .registerInbound(new RespawnHandler())
            .registerInbound(new SetSlotHandler())
            .registerInbound(new SetWindowItemsHandler())
            .registerInbound(new StatisticsHandler())
            .registerInbound(new TabListDataHandler())
            .registerInbound(new TabListEntryHandler())
            .registerInbound(new UnloadChunkHandler())
            .registerInbound(new UnlockRecipesHandler())
            .registerInbound(new UpdateTileEntityHandler())
            //ENTITY
            .registerInbound(new EntityAttachHandler())
            .registerInbound(new EntityCollectItemHandler())
            .registerInbound(new EntityDestroyHandler())
            .registerInbound(new EntityEffectHandler())
            .registerInbound(new EntityEquipmentHandler())
            .registerInbound(new EntityHeadLookHandler())
            .registerInbound(new EntityMetadataHandler())
            .registerInbound(new EntityPositionHandler())
            .registerInbound(new EntityPositionRotationHandler())
            .registerInbound(new EntityPropertiesHandler())
            .registerInbound(new EntityRemoveEffectListener())
            .registerInbound(new EntityRotationHandler())
            .registerInbound(new EntitySetPassengersHandler())
            .registerInbound(new EntityTeleportHandler())
            //SPAWN
            .registerInbound(new SpawnExperienceOrbHandler())
            .registerInbound(new SpawnMobHandler())
            .registerInbound(new SpawnObjectHandler())
            .registerInbound(new SpawnPaintingPacket())
            .registerInbound(new SpawnPlayerHandler())
            .build();

    public static final HandlerRegistry<PorkServerConnection> SERVER_HANDLERS = new HandlerRegistry.Builder<PorkServerConnection>()
            .setLogger(SERVER_LOG)
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
            //
            // Outbound packets
            //
            .registerOutbound(new LoginSuccessOutgoingHandler())
            //
            // Post-outbound packets
            //
            .registerPostOutbound(new JoinGamePostHandler())
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
