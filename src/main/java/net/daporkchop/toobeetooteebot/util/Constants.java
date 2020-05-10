/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2020 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.toobeetooteebot.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.binary.oio.appendable.PAppendable;
import net.daporkchop.lib.binary.oio.reader.UTF8FileReader;
import net.daporkchop.lib.binary.oio.writer.UTF8FileWriter;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.logging.LogAmount;
import net.daporkchop.lib.logging.Logger;
import net.daporkchop.lib.logging.Logging;
import net.daporkchop.lib.logging.impl.DefaultLogger;
import net.daporkchop.lib.minecraft.text.parser.MinecraftFormatParser;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.client.handler.incoming.AdvancementsHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.BlockChangeHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.BossBarHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.ChatHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.ChunkDataHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.ClientKeepaliveHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.GameStateHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.JoinGameHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.LoginSuccessHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.MultiBlockChangeHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.PlayerHealthHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.PlayerPosRotHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.RespawnHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.SetSlotHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.SetWindowItemsHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.StatisticsHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.TabListDataHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.TabListEntryHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.UnloadChunkHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.UnlockRecipesHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.UpdateTileEntityHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityAttachHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityCollectItemHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityDestroyHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityEffectHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityEquipmentHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityHeadLookHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityMetadataHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityPositionHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityPositionRotationHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityPropertiesHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityRemoveEffectListener;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityRotationHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntitySetPassengersHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.entity.EntityTeleportHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.spawn.SpawnExperienceOrbHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.spawn.SpawnMobHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.spawn.SpawnObjectHandler;
import net.daporkchop.toobeetooteebot.client.handler.incoming.spawn.SpawnPaintingPacket;
import net.daporkchop.toobeetooteebot.client.handler.incoming.spawn.SpawnPlayerHandler;
import net.daporkchop.toobeetooteebot.server.PorkServerConnection;
import net.daporkchop.toobeetooteebot.server.handler.incoming.LoginStartHandler;
import net.daporkchop.toobeetooteebot.server.handler.incoming.ServerChatHandler;
import net.daporkchop.toobeetooteebot.server.handler.incoming.ServerKeepaliveHandler;
import net.daporkchop.toobeetooteebot.server.handler.incoming.movement.PlayerPositionHandler;
import net.daporkchop.toobeetooteebot.server.handler.incoming.movement.PlayerPositionRotationHandler;
import net.daporkchop.toobeetooteebot.server.handler.incoming.movement.PlayerRotationHandler;
import net.daporkchop.toobeetooteebot.server.handler.outgoing.LoginSuccessOutgoingHandler;
import net.daporkchop.toobeetooteebot.server.handler.postoutgoing.JoinGamePostHandler;
import net.daporkchop.toobeetooteebot.util.cache.DataCache;
import net.daporkchop.toobeetooteebot.util.handler.HandlerRegistry;
import net.daporkchop.toobeetooteebot.websocket.WebSocketServer;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class Constants {
    public final String VERSION = "0.2.8";

    public final JsonParser JSON_PARSER = new JsonParser();
    public final Gson       GSON        = new GsonBuilder().setPrettyPrinting().create();

    public final DefaultLogger DEFAULT_LOG   = Logging.logger;
    public final Logger        AUTH_LOG      = DEFAULT_LOG.channel("Auth");
    public final Logger        CACHE_LOG     = DEFAULT_LOG.channel("Cache");
    public final Logger        CLIENT_LOG    = DEFAULT_LOG.channel("Client");
    public final Logger        CHAT_LOG      = DEFAULT_LOG.channel("Chat");
    public final Logger        GUI_LOG       = DEFAULT_LOG.channel("GUI");
    public final Logger        MODULE_LOG    = DEFAULT_LOG.channel("Module");
    public final Logger        SERVER_LOG    = DEFAULT_LOG.channel("Server");
    public final Logger        WEBSOCKET_LOG = DEFAULT_LOG.channel("WebSocket");

    public final File CONFIG_FILE = new File("config.json");

    public       Config          CONFIG;
    public final DataCache       CACHE;
    public final WebSocketServer WEBSOCKET_SERVER;

    public final HandlerRegistry<PorkClientSession> CLIENT_HANDLERS = new HandlerRegistry.Builder<PorkClientSession>()
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

    public final HandlerRegistry<PorkServerConnection> SERVER_HANDLERS = new HandlerRegistry.Builder<PorkServerConnection>()
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
                .setFormatParser(MinecraftFormatParser.DEFAULT)
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
    }

    public volatile boolean SHOULD_RECONNECT;

    public synchronized void loadConfig() {
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

    public synchronized void saveConfig() {
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
