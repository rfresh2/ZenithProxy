/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2017 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.daporkchop.toobeetooteebot;

import com.github.steveice10.mc.auth.exception.request.InvalidCredentialsException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.data.message.TextMessage;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import net.daporkchop.toobeetooteebot.client.PorkSessionListener;
import net.daporkchop.toobeetooteebot.gui.GuiBot;
import net.daporkchop.toobeetooteebot.server.PorkClient;
import net.daporkchop.toobeetooteebot.util.Config;
import net.daporkchop.toobeetooteebot.util.DataTag;
import net.daporkchop.toobeetooteebot.util.HTTPUtils;
import net.daporkchop.toobeetooteebot.web.LoggedInPlayer;
import net.daporkchop.toobeetooteebot.web.NotRegisteredPlayer;
import net.daporkchop.toobeetooteebot.web.PlayData;
import net.daporkchop.toobeetooteebot.web.RegisteredPlayer;
import net.daporkchop.toobeetooteebot.web.WebsocketServer;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TooBeeTooTeeBot {
    public static TooBeeTooTeeBot bot;
    public Client client = null;
    public Random r = new Random();
    public Timer timer = new Timer();
    public boolean firstRun = true;
    public WebsocketServer websocketServer;
    public Message tabHeader;
    public Message tabFooter;
    public ArrayList<PlayerListEntry> playerListEntries = new ArrayList<>();
    public MinecraftProtocol protocol;
    public DataTag loginData = new DataTag(new File(System.getProperty("user.dir") + File.separator + "players.dat"));
    public Map<String, RegisteredPlayer> namesToRegisteredPlayers;
    public final Map<String, NotRegisteredPlayer> namesToTempAuths = new ConcurrentHashMap<>();
    public final Map<String, LoggedInPlayer> namesToLoggedInPlayers = new ConcurrentHashMap<>();
    //BEGIN SERVER VARIABLES
    public ArrayList<PorkClient> clients = new ArrayList<>();
    public boolean isLoggedIn = false;
    public final Map<Session, PorkClient> sessionToClient = new ConcurrentHashMap<>();
    //END SERVER VARIABLES
    public Server server = null;
    public final Queue<String> queuedIngameMessages = new ConcurrentLinkedQueue<>();
    public final Map<String, Long> ingamePlayerCooldown = new ConcurrentHashMap<>();
    public final DataTag playData = new DataTag(new File(System.getProperty("user.dir") + File.separator + "online.dat"));
    public HashMap<String, PlayData> uuidsToPlayData;
    public boolean hasDonePostConnect = false;

    public static void main(String[] args) {
        new TooBeeTooTeeBot().start(args);
        try {
            Scanner scanner = new Scanner(System.in);

            scanner.nextLine();
            if (TooBeeTooTeeBot.bot.client != null && TooBeeTooTeeBot.bot.client.getSession().isConnected()) {
                TooBeeTooTeeBot.bot.client.getSession().disconnect("Forced reboot by DaPorkchop_.");
            }
            if (TooBeeTooTeeBot.bot.websocketServer != null)
                TooBeeTooTeeBot.bot.websocketServer.sendToAll("shutdownForced reboot by DaPorkchop_.");
            Thread.sleep(100);
            if (TooBeeTooTeeBot.bot.websocketServer != null)
                TooBeeTooTeeBot.bot.websocketServer.stop();
            if (Config.doWebsocket) {
                bot.loginData.setSerializable("registeredPlayers", (Serializable) bot.namesToRegisteredPlayers);
                bot.loginData.save();
            }
            if (Config.doStatCollection) {
                bot.playData.setSerializable("uuidsToPlayData", bot.uuidsToPlayData);
                bot.playData.save();
            }
            Runtime.getRuntime().halt(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long millisToNextHour(Calendar calendar) {
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        int millis = calendar.get(Calendar.MILLISECOND);
        int minutesToNextHour = 60 - minutes;
        int secondsToNextHour = 60 - seconds;
        int millisToNextHour = 1000 - millis;
        return minutesToNextHour * 60 * 1000 + secondsToNextHour * 1000 + millisToNextHour;
    }

    public static int ensureRange(int value, int min, int max) {
        int toReturn = Math.min(Math.max(value, min), max);
        return toReturn;
    }

    public static String getName(PlayerListEntry entry) {
        return entry.getDisplayName() == null ? entry.getProfile().getName() : entry.getDisplayName().getFullText();
    }

    public void start(String[] args) {
        bot = this;
        try {
            ESCAPE:
            if (!(args.length == 1 && args[0].equals("firstart"))) {
                if (Config.doAuth) {
                    try {
                        File sessionIdCache = new File(System.getProperty("user.dir") + File.separator + "sessionId.txt");
                        if (sessionIdCache.exists()) {
                            System.out.println("Attempting to log in with session ID");
                            Scanner s = new Scanner(sessionIdCache);
                            String sessionID = s.nextLine().trim();
                            s.close();
                            System.out.println("Session ID: " + sessionID);
                            try {
                                protocol = new MinecraftProtocol(Config.username); //create random thing because we need to handle login ourselves
                                AuthenticationService auth = new AuthenticationService(Config.clientId, Proxy.NO_PROXY);
                                auth.setUsername(Config.username);
                                auth.setAccessToken(sessionID);

                                auth.login();
                                protocol.profile = auth.getSelectedProfile();
                                protocol.accessToken = auth.getAccessToken();
                                System.out.println("Done! Account name: " + protocol.getProfile().getName() + ", session ID:" + protocol.getAccessToken());
                                break ESCAPE;
                            } catch (RequestException e) {
                                System.out.println("Bad/expired session ID, attempting login with username and password");

                                protocol = new MinecraftProtocol(Config.username); //create random thing because we need to handle login ourselves
                                AuthenticationService auth = new AuthenticationService(Config.clientId, Proxy.NO_PROXY);
                                auth.setUsername(Config.username);
                                auth.setPassword(Config.password);

                                auth.login();

                                protocol.profile = auth.getSelectedProfile();
                                protocol.accessToken = auth.getAccessToken();
                                System.out.println("Logged in with credentials " + Config.username + ":" + Config.password);
                                System.out.println("Saving session ID: " + protocol.getAccessToken() + " to disk");
                                PrintWriter writer = new PrintWriter("sessionId.txt", "UTF-8");
                                writer.println(protocol.getAccessToken());
                                writer.close();
                                break ESCAPE;
                            }

                        } else {
                            System.out.println("Attempting login with username and password...");
                            protocol = new MinecraftProtocol(Config.username, Config.password);

                            System.out.println("Logged in with credentials " + Config.username + ":" + Config.password);
                            System.out.println("Saving session ID: " + protocol.getAccessToken() + " to disk");
                            PrintWriter writer = new PrintWriter("sessionId.txt", "UTF-8");
                            writer.println(protocol.getAccessToken());
                            writer.close();
                        }
                    } catch (InvalidCredentialsException e) {
                        //ignore
                        System.out.println("Being rate limited, waiting...");
                        Thread.sleep(60000);
                        System.out.println("Please try relaunching, if the rate limit occurs again, please check your account credentials, or wait 10 minutes before attempting again.");
                        System.exit(3);
                    }
                } else {
                    System.out.println("Logging in with cracked account, username: " + Config.username);
                    protocol = new MinecraftProtocol(Config.username);
                }
                System.out.println("Success!");
                System.out.println(protocol.getProfile().getIdAsString());
            }

            if (Config.doServer) {
                System.out.println("Getting server icon...");
                ByteArrayInputStream inputStream = new ByteArrayInputStream(HTTPUtils.downloadImage("https://crafatar.com/avatars/" + protocol.profile.getId() + "?size=64&overlay&default=MHF_Steve"));
                Caches.icon = ImageIO.read(inputStream);
                System.out.println("Done!");
            }

            if (Config.doGUI && GuiBot.INSTANCE == null) {
                GuiBot guiBot = new GuiBot();
                guiBot.createBufferStrategy(1);
                guiBot.setVisible(true);
                return;
            }

            if (firstRun) {
                if (Config.doWebsocket) {
                    Object aaa = loginData.getSerializable("registeredPlayers", new ConcurrentHashMap<String, RegisteredPlayer>());
                    if (aaa instanceof HashMap) {
                        namesToRegisteredPlayers = new ConcurrentHashMap<>();
                        namesToRegisteredPlayers.putAll((HashMap<String, RegisteredPlayer>) aaa);
                    } else {
                        namesToRegisteredPlayers = (ConcurrentHashMap<String, RegisteredPlayer>) aaa;
                    }
                }
                if (Config.doStatCollection) {
                    uuidsToPlayData = (HashMap<String, PlayData>) playData.getSerializable("uuidsToPlayData", new HashMap<String, PlayData>());
                }

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        doPostConnectSetup();
                    }
                }, 10000);

                firstRun = false;
            }

            client = new Client(Config.ip, Config.port, protocol, new TcpSessionFactory());
            client.getSession().addListener(new PorkSessionListener(this));
            System.out.println("Connecting to " + Config.ip + ":" + Config.port + "...");
            client.getSession().connect(true);
        } catch (Exception e) {
            e.printStackTrace();
            Runtime.getRuntime().halt(0);
        }
    }

    public void sendChat(String message) {
        queueMessage("> " + message);
    }

    public void queueMessage(String toQueue) {
        queuedIngameMessages.add(toQueue);
    }

    public void processMsg(String playername, String message) {
        if (ingamePlayerCooldown.getOrDefault(playername, 0L) + 5000 > System.currentTimeMillis()) {
            return;
        } else {
            ingamePlayerCooldown.put(playername, System.currentTimeMillis());
        }
        RegisteredPlayer player = namesToRegisteredPlayers.getOrDefault(playername, null);
        if (player == null) {
            NotRegisteredPlayer tempAuth = namesToTempAuths.getOrDefault(playername, null);
            if (tempAuth == null) {
                queueMessage("/msg " + playername + " You're not registered! Go to http://www.daporkchop.net/pork2b2tbot to register!");
                return;
            } else if (message.startsWith("register")) {
                message = message.substring(9);
                if (message.startsWith(tempAuth.tempAuthUUID)) {
                    String hashedPwd = Hashing.sha256().hashString(tempAuth.pwd, Charsets.UTF_8).toString();
                    RegisteredPlayer newPlayer = new RegisteredPlayer(hashedPwd, tempAuth.name);
                    newPlayer.lastUsed = System.currentTimeMillis();
                    namesToTempAuths.remove(tempAuth.name);
                    namesToRegisteredPlayers.put(tempAuth.name, newPlayer);
                    loginData.setSerializable("registeredPlayers", (Serializable) namesToRegisteredPlayers);
                    queueMessage("/msg " + playername + " Successfully registered! You can now use your username and password on the website!");
                    return;
                } else {
                    queueMessage("/msg " + playername + " Incorrect authentication UUID!");
                    return;
                }
            }
        } else {
            if (message.startsWith("help")) {
                queueMessage("/msg " + playername + " Use '/msg 2pork2bot <player name> <message>' to send them a message! Visit http://www.daporkchop.net/pork2b2tbot for more info!");
                return;
            } else if (message.startsWith("changepass")) {
                String[] messageSplit = message.split(" ");
                String sha1 = Hashing.sha1().hashString(messageSplit[1], Charsets.UTF_8).toString();
                String sha256 = Hashing.sha256().hashString(sha1, Charsets.UTF_8).toString();
                player.passwordHash = sha256;
                queueMessage("/msg " + playername + " Changed password to " + messageSplit[1] + " (sha1: " + sha1 + ")");
            } else {
                String[] messageSplit = message.split(" ");
                LoggedInPlayer loggedInPlayer = namesToLoggedInPlayers.getOrDefault(messageSplit[0], null);
                if (loggedInPlayer == null) {
                    queueMessage("/msg " + playername + " The user " + messageSplit[0] + " could not be found! They might be idle, or they aren't logged in to the website!");
                    return;
                } else {
                    if (loggedInPlayer.clientSocket.isOpen())
                        loggedInPlayer.clientSocket.send("chat    \u00A7d" + playername + "\u00A7d says: \u00A7d" + message.substring(messageSplit[0].length() + 1));
                    queueMessage("/msg " + playername + " Sent message to " + messageSplit[0]);
                    return;
                }
            }
        }
    }

    public void doPostConnectSetup() {
        try {
            if (!hasDonePostConnect) {
                if (Config.doWebsocket) {
                    TooBeeTooTeeBot.bot.websocketServer = new WebsocketServer(Config.websocketPort);
                    TooBeeTooTeeBot.bot.websocketServer.start();
                }

                if (Config.doStatCollection) {
                    Long midnight = LocalDateTime.now().until(LocalDate.now().plusDays(1).atStartOfDay(), ChronoUnit.MILLIS);
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            long currentTime = System.currentTimeMillis();
                            Iterator<Map.Entry<String, PlayData>> iterator = uuidsToPlayData.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<String, PlayData> entry = iterator.next();
                                PlayData data = entry.getValue();
                                if (currentTime - data.lastPlayed >= 2592000000L) { //one month
                                    iterator.remove();
                                    continue;
                                }
                                for (int i = data.playTimeByDay.length - 2; i >= 0; i--) {
                                    data.playTimeByDay[i + 1] = data.playTimeByDay[i];
                                }
                                data.playTimeByDay[0] = 0;
                            }
                        }
                    }, midnight, 1440 * 60 * 60 * 1000);

                    Long nextHour = millisToNextHour(Calendar.getInstance());
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            long currentTime = System.currentTimeMillis();
                            Iterator<Map.Entry<String, PlayData>> iterator = uuidsToPlayData.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<String, PlayData> entry = iterator.next();
                                PlayData data = entry.getValue();
                                for (int i = data.playTimeByHour.length - 2; i >= 0; i--) {
                                    data.playTimeByHour[i + 1] = data.playTimeByHour[i];
                                }
                                data.playTimeByHour[0] = 0;
                            }
                        }
                    }, nextHour, 60 * 60 * 1000);
                }
                hasDonePostConnect = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reLaunch() {
        for (int i = 10; i > 0; i--) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (GuiBot.INSTANCE != null) {
                GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Reconnecting in " + i + "</html>");
                String[] split = GuiBot.INSTANCE.chatDisplay.getText().split("<br>");
                if (split.length > 500) {
                    String toSet = "<html>";
                    for (int j = 1; j < split.length; j++) {
                        toSet += split[j] + "<br>";
                    }
                    toSet = toSet.substring(toSet.length() - 4) + "</html>";
                    GuiBot.INSTANCE.chatDisplay.setText(toSet);
                }
            }

            System.out.println("Reconnecting in " + i);
        }

        cleanUp();

        if (GuiBot.INSTANCE != null) {
            if (!GuiBot.INSTANCE.connect_disconnectButton.isEnabled()) {
                GuiBot.INSTANCE.connect_disconnectButton.setEnabled(true);
                return;
            }
        }

        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Launching everything again!");
                TooBeeTooTeeBot.bot.start(new String[0]);
            }
        }, 1000);
    }

    public void cleanUp() {
        System.out.println("Resetting EVERYTHING...");
        //this.client = null;
        //System.out.println("Reset client");
        this.timer.cancel();
        System.out.println("Cancelled timer");
        this.timer.purge();
        System.out.println("Purged timer");
        this.timer = new Timer();
        System.out.println("Reset timer");
        System.out.println("Reset queued messages");
        this.tabHeader = new TextMessage("");
        this.tabFooter = new TextMessage("");
        if (websocketServer != null) {
            for (PlayerListEntry entry : playerListEntries) {
                websocketServer.sendToAll("tabDel  " + TooBeeTooTeeBot.getName(entry));
            }
        }
        System.out.println("Reset tab header and footer");
        this.playerListEntries.clear();
        System.out.println("Reset player list");
        this.isLoggedIn = false;
        Caches.x = 0;
        Caches.y = 0;
        Caches.z = 0;
        Caches.yaw = 0;
        Caches.pitch = 0;
        Caches.onGround = true;
        System.out.println("Reset position");
        Caches.cachedChunks.clear();
        System.out.println("Reset cached chunks");
        this.queuedIngameMessages.clear();
        System.out.println("Reset queued ingame messages");
        this.ingamePlayerCooldown.clear();
        System.out.println("Reset ingame cooldown timer");
        this.hasDonePostConnect = false;
        System.out.println("Clearing other caches");
        Caches.cachedEntities.clear();
        Caches.cachedBossBars.clear();
        Caches.player.potionEffects.clear();
        System.out.println("Reset complete!");
    }
}
