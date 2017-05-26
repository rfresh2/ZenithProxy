package net.daporkchop.toobeetooteebot;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import net.daporkchop.toobeetooteebot.client.PorkSessionListener;
import net.daporkchop.toobeetooteebot.server.PorkClient;
import net.daporkchop.toobeetooteebot.util.Config;
import net.daporkchop.toobeetooteebot.util.DataTag;
import net.daporkchop.toobeetooteebot.web.*;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.TextChannel;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class TooBeeTooTeeBot {
    public static TooBeeTooTeeBot INSTANCE;
    public Client client = null;
    public Random r = new Random();
    public Timer timer = new Timer();
    public JDA jda;
    public TextChannel channel;
    public ArrayList<String> queuedMessages = null;
    public boolean firstRun = true;
    public WebsocketServer websocketServer;
    public Message tabHeader;
    public Message tabFooter;
    public ArrayList<TabListPlayer> playerListEntries = new ArrayList<>();
    public MinecraftProtocol protocol;
    public boolean doAFK = true;
    public DataTag loginData = new DataTag(new File(System.getProperty("user.dir") + File.separator + "players.dat"));
    public HashMap<String, RegisteredPlayer> namesToRegisteredPlayers;
    public HashMap<String, NotRegisteredPlayer> namesToTempAuths = new HashMap<>();
    public HashMap<String, LoggedInPlayer> namesToLoggedInPlayers = new HashMap<>();
    //BEGIN SERVER VARIABLES
    public ArrayList<PorkClient> clients = new ArrayList<>();
    public boolean isLoggedIn = false;
    public HashMap<Session, PorkClient> sessionToClient = new HashMap<>();
    public double x = 0, y = 0, z = 0;
    public float yaw = 0, pitch = 0;
    public boolean onGround;
    public HashMap<Long, Column> cachedChunks = new HashMap<>();
    //END SERVER VARIABLES
    public Server server;
    public ArrayList<String> queuedIngameMessages = new ArrayList<>();
    public HashMap<String, Long> ingamePlayerCooldown = new HashMap<>();
    public DataTag playData = new DataTag(new File(System.getProperty("user.dir") + File.separator + "online.dat"));
    public HashMap<String, PlayData> uuidsToPlayData;
    protected boolean hasDonePostConnect = false;

    public static void main(String[] args) {
        new TooBeeTooTeeBot().start(args);
        try {
            Scanner scanner = new Scanner(System.in);

            scanner.nextLine();
            if (TooBeeTooTeeBot.INSTANCE.client != null && TooBeeTooTeeBot.INSTANCE.client.getSession().isConnected()) {
                TooBeeTooTeeBot.INSTANCE.client.getSession().disconnect("Forced reboot by DaPorkchop_.");
            }
            if (TooBeeTooTeeBot.INSTANCE.websocketServer != null)
                TooBeeTooTeeBot.INSTANCE.websocketServer.sendToAll("shutdownForced reboot by DaPorkchop_.");
            Thread.sleep(100);
            if (TooBeeTooTeeBot.INSTANCE.websocketServer != null)
                TooBeeTooTeeBot.INSTANCE.websocketServer.stop();
            if (Config.doWebsocket) {
                INSTANCE.loginData.setSerializable("registeredPlayers", INSTANCE.namesToRegisteredPlayers);
                INSTANCE.loginData.save();
            }
            if (Config.doStatCollection) {
                INSTANCE.playData.setSerializable("uuidsToPlayData", INSTANCE.uuidsToPlayData);
                INSTANCE.playData.save();
            }
            System.exit(0);
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

    public void start(String[] args) {
        INSTANCE = this;
        try {
            if (firstRun) {
                if (Config.doWebsocket) {
                    namesToRegisteredPlayers = (HashMap<String, RegisteredPlayer>) loginData.getSerializable("registeredPlayers", new HashMap<String, RegisteredPlayer>());
                }
                if (Config.doStatCollection) {
                    uuidsToPlayData = (HashMap<String, PlayData>) playData.getSerializable("uuidsToPlayData", new HashMap<String, PlayData>());
                }
                if (Config.doDiscord) {
                    queuedMessages = new ArrayList<>();
                }

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        doPostConnectSetup();
                    }
                }, 10000);
            }
            ESCAPE:
            if (protocol == null) {
                if (Config.doAuth) {
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

                            Field profile = MinecraftProtocol.class.getDeclaredField("profile");
                            Field accessToken = MinecraftProtocol.class.getDeclaredField("accessToken");
                            profile.setAccessible(true);
                            accessToken.setAccessible(true);
                            profile.set(protocol, auth.getSelectedProfile());
                            accessToken.set(protocol, auth.getAccessToken());
                            System.out.println("Done! Account name: " + protocol.getProfile().getName() + ", session ID:" + protocol.getAccessToken());
                            break ESCAPE;
                        } catch (RequestException e) {
                            System.out.println("Bad/expired session ID, attempting login with username and password");

                            protocol = new MinecraftProtocol(Config.username); //create random thing because we need to handle login ourselves
                            AuthenticationService auth = new AuthenticationService(Config.clientId, Proxy.NO_PROXY);
                            auth.setUsername(Config.username);
                            auth.setPassword(Config.password);

                            auth.login();

                            Field profile = MinecraftProtocol.class.getDeclaredField("profile");
                            Field accessToken = MinecraftProtocol.class.getDeclaredField("accessToken");
                            profile.setAccessible(true);
                            accessToken.setAccessible(true);
                            profile.set(protocol, auth.getSelectedProfile());
                            accessToken.set(protocol, auth.getAccessToken());
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
                } else {
                    System.out.println("Logging in with cracked account, username: " + Config.username);
                    protocol = new MinecraftProtocol(Config.username);
                }
                System.out.println("Success!");
            }

            client = new Client(Config.ip, Config.port, protocol, new TcpSessionFactory());
            client.getSession().addListener(new PorkSessionListener(this));
            System.out.println("Connecting to " + Config.ip + ":" + Config.port + "...");
            client.getSession().connect(true);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
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
                    loginData.setSerializable("registeredPlayers", namesToRegisteredPlayers);
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
                    TooBeeTooTeeBot.INSTANCE.websocketServer = new WebsocketServer(Config.websocketPort);
                    TooBeeTooTeeBot.INSTANCE.websocketServer.start();
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

                if (Config.doDiscord) {
                    jda = new JDABuilder(AccountType.BOT)
                            .setToken(Config.token)
                            .buildBlocking();
                    channel = jda.getTextChannelById("305346913488863243");

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (queuedMessages.size() > 0) {
                                StringBuilder builder = new StringBuilder();
                                Iterator<String> iter = queuedMessages.iterator();
                                iter.hasNext(); //idk lol
                                while (builder.length() < 2001) {
                                    StringBuilder copiedBuilder = new StringBuilder(builder.toString());
                                    copiedBuilder.append(iter.next() + "\n");
                                    if (builder.length() > 2000) {
                                        channel.sendMessage(copiedBuilder.toString()).queue();
                                        queuedMessages.clear(); //yes, ik that this might lose some messages but idrc
                                        return;
                                    } else {
                                        builder = copiedBuilder;
                                    }
                                    if (!iter.hasNext()) {
                                        break;
                                    }
                                }
                                channel.sendMessage(builder.toString()).queue();
                                queuedMessages.clear(); //yes, ik that this might lose some messages but idrc
                            }
                        }
                    }, 2000, 2000);
                }
                hasDonePostConnect = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
