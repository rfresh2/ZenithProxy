/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2019 DaPorkchop_
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

package net.daporkchop.toobeetooteebot;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.message.TextMessage;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.SessionFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import lombok.Setter;
import net.daporkchop.lib.http.SimpleHTTP;
import net.daporkchop.lib.logging.LogAmount;
import net.daporkchop.lib.logging.Logging;
import net.daporkchop.lib.minecraft.text.parser.MinecraftFormatParser;
import net.daporkchop.toobeetooteebot.client.PorkClientSession;
import net.daporkchop.toobeetooteebot.gui.Gui;
import net.daporkchop.toobeetooteebot.mc.PorkSessionFactory;
import net.daporkchop.toobeetooteebot.server.PorkServerConnection;
import net.daporkchop.toobeetooteebot.server.PorkServerListener;
import net.daporkchop.toobeetooteebot.util.Constants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author DaPorkchop_
 */
@Getter
public class Bot implements Constants {
    @Getter
    private static Bot instance;

    private final SessionFactory sessionFactory = new PorkSessionFactory(this);
    private final Collection<PorkServerConnection> serverConnections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private MinecraftProtocol protocol;
    private Client client;
    private Server server;
    @Setter
    private BufferedImage serverIcon;

    protected final Gui gui = new Gui();

    public static void main(String... args) {
        {
            String date = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(Date.from(Instant.now()));
            LogAmount amount = LogAmount.valueOf(CONFIG.getString("debug.loglevel", LogAmount.SIMPLE.name()).toUpperCase());
            File logFolder = new File("./log/");
            if (!logFolder.exists())    {
                if (!logFolder.mkdirs())    {
                    throw new IllegalStateException(String.format("Unable to create folder: %s", logFolder.getAbsolutePath()));
                }
            } else if (!logFolder.isDirectory())    {
                throw new IllegalStateException(String.format("Not a directory: %s", logFolder.getAbsolutePath()));
            }
            DEFAULT_LOG.addFile(new File(logFolder, String.format("%s.log", date)), amount)
                    .enableANSI()
                    .setFormatParser(new MinecraftFormatParser())
                    .setLogAmount(amount);

            if (CONFIG.getBoolean("log.ansi", true))    {
                DEFAULT_LOG.enableANSI();
            }
            if (CONFIG.getBoolean("log.storeDebug", true))  {
                DEFAULT_LOG.addFile(new File(logFolder, String.format("%s-debug.log", date)), LogAmount.DEBUG);
            }
        }

        DEFAULT_LOG.info("Starting Pork2b2tBot v%s...", VERSION);

        Bot bot = new Bot();
        instance = bot;
        bot.start();
    }

    public void start() {
        try {
            this.gui.start();
            {
                Thread mainThread = Thread.currentThread();
                Thread commandReaderThread = new Thread(() -> {
                    try (Scanner s = new Scanner(System.in)) {
                        s.nextLine(); //TODO: command processing from CLI
                    }
                    SHOULD_RECONNECT.set(false);
                    if (this.isConnected()) {
                        this.client.getSession().disconnect("user disconnect");
                    }
                    mainThread.interrupt();
                }, "Pork2b2tBot command processor thread");
                commandReaderThread.setDaemon(true);
                commandReaderThread.start();
            }
            { //TODO: clean this up
                Collection<Runnable> modules = new ArrayDeque<>();
                if (CONFIG.getBoolean("client.extra.antiafk.enabled", true)) {
                    MODULE_LOG.trace("Enabling AntiAFK");
                    modules.add(() -> {
                        if (CONFIG.getBoolean("client.extra.antiafk.runEvenIfClientsConnected") || this.serverConnections.isEmpty()) {
                            boolean swingHand = CONFIG.getBoolean("client.extra.antiafk.actions.swingHand", true);
                            boolean rotate = CONFIG.getBoolean("client.extra.antiafk.actions.rotate", true);

                            int action = -1;
                            if (swingHand && rotate) {
                                action = ThreadLocalRandom.current().nextInt(2);
                            } else if (swingHand) {
                                action = 0;
                            } else if (rotate) {
                                action = 1;
                            }
                            switch (action) {
                                case 0:
                                    this.client.getSession().send(new ClientPlayerSwingArmPacket(Hand.MAIN_HAND));
                                    break;
                                case 1:
                                    this.client.getSession().send(new ClientPlayerRotationPacket(
                                            true,
                                            -90 + (90 - -90) * ThreadLocalRandom.current().nextFloat(),
                                            -90 + (90 - -90) * ThreadLocalRandom.current().nextFloat()
                                    ));
                                    break;
                            }
                        }
                    });
                }

                { //populate default messages
                    JsonArray def = new JsonArray();
                    def.add(new JsonPrimitive("#TeamPepsi"));
                    def.add(new JsonPrimitive("https://pepsi.team"));
                    def.add(new JsonPrimitive("https://daporkchop.net"));
                    CONFIG.getArray("client.extra.spammer.messages", def);
                    CONFIG.getInt("client.extra.spammer.delaySeconds", 30);
                }
                if (CONFIG.getBoolean("client.extra.spammer.enabled")) {
                    List<String> messages = CONFIG.getList("client.extra.spammer.messages", JsonElement::getAsString);
                    AtomicInteger i = new AtomicInteger(0);
                    MODULE_LOG.trace("Enabling spammer with %d messages, choosing every %d seconds", messages.size(), CONFIG.getInt("client.extra.spammer.delaySeconds"));
                    modules.add(() -> {
                        if ((i.getAndIncrement() >> 1) == CONFIG.getInt("client.extra.spammer.delaySeconds")) {
                            i.set(0);
                            this.client.getSession().send(new ClientChatPacket(messages.get(ThreadLocalRandom.current().nextInt(messages.size()))));
                        }
                    });
                }
                Thread moduleRunnerThread = new Thread(() -> {
                    try {
                        while (true) {
                            Thread.sleep(500L);
                            if (this.isConnected() && ((MinecraftProtocol) this.client.getSession().getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) {
                                modules.forEach(Runnable::run);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                moduleRunnerThread.setDaemon(true);
                moduleRunnerThread.start();
            }

            this.logIn();
            this.startServer();
            CACHE.reset(true);
            do {
                this.logIn();
                this.connect();

                CONFIG.save();
                //wait for client to disconnect before starting again
                CLIENT_LOG.info("Disconnected. Reason: %s", ((PorkClientSession) this.client.getSession()).getDisconnectReason());
            } while (SHOULD_RECONNECT.get() && CACHE.reset(true) && this.delayBeforeReconnect());
        } catch (Exception e) {
            DEFAULT_LOG.alert(e);
        } finally {
            DEFAULT_LOG.info("Shutting down...");
            if (this.server != null) {
                this.server.close(true);
            }
            CONFIG.save();
        }
    }

    private void connect() {
        synchronized (this) {
            if (this.isConnected()) {
                throw new IllegalStateException("Already connected!");
            }

            String address = CONFIG.getString("client.server.address", "mc.example.com");
            int port = CONFIG.getInt("client.server.port", 25565);

            CLIENT_LOG.info("Connecting to %s:%d...", address, port);
            this.client = new Client(address, port, this.protocol, this.sessionFactory);
            this.client.getSession().connect(true);
        }
    }

    public boolean isConnected() {
        return this.client != null && this.client.getSession() != null && this.client.getSession().isConnected();
    }

    private void startServer() {
        synchronized (this) {
            if (this.server != null) {
                throw new IllegalStateException("Server already started!");
            }
            if (CONFIG.getBoolean("server.enabled")) {
                try (InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode("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"))) {
                    this.serverIcon = ImageIO.read(is);
                } catch (IOException e) {
                    //impossible
                    throw new AssertionError();
                }

                String address = CONFIG.getString("server.bind.address", "0.0.0.0");
                int port = CONFIG.getInt("server.bind.port", 25565);

                SERVER_LOG.info("Starting server on %s:%d...", address, port);
                this.server = new Server(address, port, MinecraftProtocol.class, this.sessionFactory);
                this.server.setGlobalFlag(MinecraftConstants.AUTH_PROXY_KEY, Proxy.NO_PROXY);
                this.server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, CONFIG.getBoolean("server.verifyusers"));
                this.server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, (ServerInfoBuilder) session -> new ServerStatusInfo(
                        new VersionInfo(MinecraftConstants.GAME_VERSION, MinecraftConstants.PROTOCOL_VERSION),
                        new PlayerInfo(
                                CONFIG.getInt("server.ping.maxplayers", Integer.MAX_VALUE),
                                this.serverConnections.stream().filter(con -> ((MinecraftProtocol) con.getSession().getPacketProtocol()).getSubProtocol() == SubProtocol.GAME).collect(Collectors.toList()).size(),
                                this.serverConnections.stream()
                                        .map(con -> (MinecraftProtocol) con.getSession().getPacketProtocol())
                                        .filter(p -> p.getSubProtocol() == SubProtocol.GAME)
                                        .map(MinecraftProtocol::getProfile)
                                        .toArray(GameProfile[]::new)
                        ),
                        new TextMessage(String.format(CONFIG.getString("server.ping.motd", "\u00A7c%s"), this.protocol.getProfile().getName())),
                        this.serverIcon
                ));
                this.server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, (ServerLoginHandler) session -> {
                    session.send(new ServerJoinGamePacket(
                            CACHE.getPlayerCache().getEntityId(),
                            CACHE.getPlayerCache().isHardcore(),
                            CACHE.getPlayerCache().getGameMode(),
                            CACHE.getPlayerCache().getDimension(),
                            CACHE.getPlayerCache().getDifficulty(),
                            CACHE.getPlayerCache().getMaxPlayers(),
                            CACHE.getPlayerCache().getWorldType(),
                            CACHE.getPlayerCache().isReducedDebugInfo()
                    ));
                    SERVER_LOG.info("Player connected: %s", session.getRemoteAddress());
                    ((PorkServerListener) this.server.getListeners().stream().filter(l -> l instanceof PorkServerListener).findAny()
                            .orElseThrow(IllegalStateException::new)).getConnections().get(session).setPlayer(true);
                });
                this.server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, CONFIG.getInt("server.compressionThreshold", 256));
                this.server.addListener(new PorkServerListener(this));
                this.server.bind(false);
            }
        }
    }

    private void logIn() {
        if (this.protocol == null) {
            AUTH_LOG.info("Logging in...");
            if (CONFIG.getBoolean("authentication.doAuthentication")) {
                try {
                    this.protocol = new MinecraftProtocol(
                            CONFIG.getString("authentication.username", "john.doe@example.com"),
                            CONFIG.getString("authentication.password", "hackme")
                    );
                } catch (RequestException e) {
                    throw new RuntimeException(String.format(
                            "Unable to log in using credentials %s:%s",
                            CONFIG.getString("authentication.username"),
                            CONFIG.getString("authentication.password")), e);
                }
            } else {
                this.protocol = new MinecraftProtocol(CONFIG.getString("authentication.username", "Steve"));
                CONFIG.getString("authentication.password", "hackme"); //add password field to config by default
            }
            if (CONFIG.getBoolean("server.enabled") && CONFIG.getBoolean("server.ping.favicon", true)) {
                new Thread(() -> {
                    try (InputStream is = new ByteArrayInputStream(SimpleHTTP.get(String.format("https://crafatar.com/avatars/%s?size=64&overlay&default=MHF_Steve", this.protocol.getProfile().getId().toString())))) {
                        this.serverIcon = ImageIO.read(is);
                    } catch (IOException e) {
                        System.err.printf("Unable to download server icon for \"%s\":\n", this.protocol.getProfile().getName());
                        e.printStackTrace();
                    }
                }, "Server icon downloader thread").start();
            }
            CACHE.getProfileCache().setProfile(this.protocol.getProfile());
            AUTH_LOG.success("Logged in.");
        }
    }

    private boolean delayBeforeReconnect() {
        try {
            for (int i = CONFIG.getInt("client.extra.autoreconnect.delay", 10); SHOULD_RECONNECT.get() && i > 0; i--) {
                CLIENT_LOG.info("Reconnecting in %d", i);
                Thread.sleep(1000L);
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
