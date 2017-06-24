package net.daporkchop.toobeetooteebot.util;

import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * settings
 * kek
 * ignore this
 * kek
 * again
 * kek
 * ok
 * i need to stop
 * saying kek
 * kek
 * note to self: make a meaningful header on this class
 * kek
 */
public class Config {
    public static final YMLParser parser;
    public static String username;
    public static String password;
    public static boolean doAuth;
    public static String token;
    public static boolean doDiscord;
    public static String ip;
    public static int port;
    public static String clientId;
    public static boolean doWebsocket;
    public static int websocketPort;
    public static boolean doStatCollection;
    public static boolean processChat;
    public static boolean doAutoRespawn;
    public static boolean doAntiAFK;
    public static boolean doSpammer;
    public static int spamDelay;
    public static String[] spamMesages;
    public static boolean doServer;
    public static int serverPort;
    public static String serverHost;
    public static boolean doServerAuth;
    public static boolean doServerWhitelist;
    public static List<String> whitelistedNames;
    public static boolean doGUI;
    public static boolean doAutoRelog;

    static {
        File configFile = new File(System.getProperty("user.dir") + File.separatorChar + "config.yml");
        if (!configFile.exists()) {
            URL inputUrl = TooBeeTooTeeBot.class.getResource("/config.yml");
            try {
                org.apache.commons.io.FileUtils.copyURLToFile(inputUrl, configFile);
            } catch (IOException e) {
                e.printStackTrace();
                Runtime.getRuntime().halt(0);
            }
        }
        parser = new YMLParser(configFile);
        switch (parser.getInt("config-version")) {
            case 1:
                parser.set("server.doServer", true);
                parser.set("server.port", 25565);
                parser.set("server.host", "0.0.0.0");
            case 2:
                parser.set("interface.doGUI", true);
                parser.set("misc.doAutoRelog", true);
            case 3:
                parser.set("server.doAuth", false);
                parser.set("server.whiteListUser", "");
            case 4:
                ArrayList<String> list = new ArrayList<>();
                list.add("");
                parser.set("server.whiteListUser", list);

                parser.set("config-version", 5);
                parser.save();
        }
        username = parser.getString("login.username", "Steve");
        password = parser.getString("login.password", "password");
        doAuth = parser.getBoolean("login.doAuthentication", false);
        token = parser.getString("discord.token", "");
        doDiscord = parser.getBoolean("discord.doDiscord", false);
        ip = parser.getString("client.hostIP", "mc.example.com");
        port = parser.getInt("client.hostPort", 25565);
        doWebsocket = parser.getBoolean("websocket.doWebsocket", false);
        websocketPort = parser.getInt("websocket.port", 8888);
        doStatCollection = parser.getBoolean("stats.doStats", false);
        processChat = parser.getBoolean("chat.doProcess", false);
        doAutoRespawn = parser.getBoolean("misc.autorespawn", true);
        doAntiAFK = parser.getBoolean("misc.antiafk", true);
        doSpammer = parser.getBoolean("chat.spam.doSpam", false);
        doServer = parser.getBoolean("server.doServer", true);
        spamDelay = parser.getInt("chat.spam.delay", 10000);
        serverPort = parser.getInt("server.port", 25565);
        serverHost = parser.getString("server.host", "0.0.0.0");
        doGUI = parser.getBoolean("interface.doGUI", true);
        doAutoRelog = parser.getBoolean("misc.doAutoRelog", true);
        doServerAuth = parser.getBoolean("server.doAuth", false);
        whitelistedNames = parser.getStringList("server.whiteListUser");
        doServerWhitelist = whitelistedNames != null && !(whitelistedNames.isEmpty() || whitelistedNames.get(0).isEmpty());
        System.out.println(doServerWhitelist);
        System.out.println(whitelistedNames.isEmpty());
        System.out.println(whitelistedNames.get(0).isEmpty());
        System.out.println(!(whitelistedNames.isEmpty() || whitelistedNames.get(0).isEmpty()));

        List<String> spam = parser.getStringList("chat.spam.messages");
        spamMesages = spam.toArray(new String[spam.size()]);

        try {
            File clientId = new File(System.getProperty("user.dir") + File.separator + "clientId.txt");
            if (clientId.exists()) {
                Scanner scanner = new Scanner(clientId);
                Config.clientId = scanner.nextLine().trim();
                scanner.close();
            } else {
                PrintWriter writer = new PrintWriter(clientId, "UTF-8");
                writer.println(Config.clientId = UUID.randomUUID().toString());
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
