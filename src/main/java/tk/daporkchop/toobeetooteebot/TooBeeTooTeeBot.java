package tk.daporkchop.toobeetooteebot;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import com.google.gson.JsonElement;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.TextChannel;

import java.io.File;
import java.io.IOException;
import java.util.*;
import com.google.common.collect.Maps;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TooBeeTooTeeBot {
	
	public static final String[] BLOCK_NAMES = new String[] { "Cobblestone", "Stone", "Netherrack", "Stone Bricks", "Block of Coal", "Block of Iron", "Block of Gold", "Block of Diamond", "Block of Emerald", "Obsidian" };

    public Client client = null;
    
    public Random r = new Random();
    public Timer timer = new Timer();

    public String username;
    public String password;
    public boolean doAuth;

    public JDA jda;
    public String token;
    public TextChannel channel;
    public ArrayList<String> queuedMessages = new ArrayList<>();

    public boolean firstRun = true;

    public WebsocketServer websocketServer;

    public Message tabHeader;
    public Message tabFooter;
    public ArrayList<TabListPlayer> playerListEntries = new ArrayList<>();

    public MinecraftProtocol protocol;

    public String ip;
    public int port;

    public boolean doAFK = true;

    public static TooBeeTooTeeBot INSTANCE;

    public static void main(String[] args)  {
        new TooBeeTooTeeBot().start(args);
        try {
            Scanner scanner = new Scanner(System.in);

            String whatever = scanner.nextLine();
            if (TooBeeTooTeeBot.INSTANCE.client != null && TooBeeTooTeeBot.INSTANCE.client.getSession().isConnected()) {
                TooBeeTooTeeBot.INSTANCE.client.getSession().disconnect("Forced reboot by DaPorkchop_.");
            }
            TooBeeTooTeeBot.INSTANCE.websocketServer.sendToAll("shutdownForced reboot by DaPorkchop_.");
            Thread.sleep(100);
            TooBeeTooTeeBot.INSTANCE.websocketServer.stop();
            System.exit(0);
        } catch (Exception e)   {
            e.printStackTrace();
        }
    }

    public void start(String[] args)    {
        INSTANCE = this;
        try {
            if (firstRun)   {
                Scanner scanner = new Scanner(new File(System.getProperty("user.dir") + File.separator + "logininfo.txt"));
                TooBeeTooTeeBot.INSTANCE.username = scanner.nextLine().trim();
                TooBeeTooTeeBot.INSTANCE.password = scanner.nextLine().trim();
                TooBeeTooTeeBot.INSTANCE.token = scanner.nextLine().trim();
                TooBeeTooTeeBot.INSTANCE.doAuth = Boolean.parseBoolean(scanner.nextLine().trim());
                TooBeeTooTeeBot.INSTANCE.ip = scanner.nextLine().trim();
                TooBeeTooTeeBot.INSTANCE.port = Integer.parseInt(scanner.nextLine().trim());
                scanner.close();

                TooBeeTooTeeBot.INSTANCE.websocketServer = new WebsocketServer(8888);
                TooBeeTooTeeBot.INSTANCE.websocketServer.start();

                jda = new JDABuilder(AccountType.BOT)
                        .setToken(token)
                        .buildBlocking();
                channel = jda.getTextChannelById("305346913488863243");
                
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (queuedMessages.size() > 0)  {
                            StringBuilder builder = new StringBuilder();
                            Iterator<String> iter = queuedMessages.iterator();
                            iter.hasNext(); //idk lol
                            while (builder.length() < 2001)   {
                                StringBuilder copiedBuilder = new StringBuilder(builder.toString());
                                copiedBuilder.append(iter.next() + "\n");
                                if (builder.length() > 2000)    {
                                    channel.sendMessage(copiedBuilder.toString()).queue();
                                    queuedMessages.clear(); //yes, ik that this might lose some messages but idrc
                                    return;
                                } else {
                                    builder = copiedBuilder;
                                }
                                if (!iter.hasNext())    {
                                    break;
                                }
                            }
                            channel.sendMessage(builder.toString()).queue();
                            queuedMessages.clear(); //yes, ik that this might lose some messages but idrc
                        }
                    }
                }, 1000, 1000);
            }

            if (doAuth) {
                System.out.println("Logging in with credentials: " + username + ":" + password);
                protocol = new MinecraftProtocol(username, password);
            } else {
                System.out.println("Loggin in with cracked account, username: " + username);
                protocol = new MinecraftProtocol(username);
            }
            System.out.println("Success!");

            client = new Client(ip, port, protocol, new TcpSessionFactory());
            client.getSession().addListener(new SessionListener() {
                @Override
                public void packetReceived(PacketReceivedEvent packetReceivedEvent) {
                    try {
                        if (packetReceivedEvent.getPacket() instanceof ServerChatPacket) {
                            ServerChatPacket pck = (ServerChatPacket) packetReceivedEvent.getPacket();
                            String messageJson = pck.getMessage().toJsonString();
                            String legacyColorCodes = BaseComponent.toLegacyText(ComponentSerializer.parse(messageJson));
                            String msg = TextFormat.clean(legacyColorCodes);
                            System.out.println("[CHAT] " + msg);
                            try {
                                if (msg.split(" ")[1].startsWith("whispers")) {
                                    //TODO: process messages
                                    return;
                                }
                            } catch (ArrayIndexOutOfBoundsException e)  {
                                //ignore kek
                            }

                            if (msg.startsWith("!")) { //command from PorkProxy
                                if (msg.startsWith("!toggleafk"))   { //useful when manually moving bot around
                                    doAFK = !doAFK;
                                    System.out.println("! Toggled AntiAFK! Current state: " + (doAFK ? "on" : "off"));
                                    client.getSession().send(new ClientChatPacket("! Toggled AntiAFK! Current state: " + (doAFK ? "on" : "off")));
                                    return;
                                }
                            }
                            queuedMessages.add(msg);
                            websocketServer.sendToAll("chat    " + legacyColorCodes.replace("<", "&lt;").replace(">", "&gt;"));
                        } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerHealthPacket) {
                            ServerPlayerHealthPacket pck = (ServerPlayerHealthPacket) packetReceivedEvent.getPacket();
                            timer.schedule(new TimerTask() { // respawn
                                @Override
                                public void run() {
                                    client.getSession().send(new ClientRequestPacket(ClientRequest.RESPAWN));
                                }
                            }, 100);
                        } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerListEntryPacket) {
                            ServerPlayerListEntryPacket pck = (ServerPlayerListEntryPacket) packetReceivedEvent.getPacket();
                            switch (pck.getAction()) {
                                case ADD_PLAYER:
                                    for (PlayerListEntry entry : pck.getEntries()) {
                                        if (entry.getProfile().getName().equals("2pork2bot"))  {
                                            continue;
                                        }
                                        TabListPlayer player = new TabListPlayer(entry.getProfile().getId().toString(), entry.getProfile().getName(), entry.getPing());
                                        playerListEntries.add(player);
                                        websocketServer.sendToAll("tabAdd  " + player.name + "SPLIT" + player.ping);
                                    }
                                    break;
                                case UPDATE_GAMEMODE:
                                    //ignore
                                    break;
                                case UPDATE_LATENCY:
                                    for (PlayerListEntry entry : pck.getEntries()) {
                                        String uuid = entry.getProfile().getId().toString();
                                        for (TabListPlayer toChange : playerListEntries) {
                                            if (uuid.equals(toChange.uuid)) {
                                                toChange.ping = entry.getPing();
                                                websocketServer.sendToAll("tabPing " + toChange.name + "SPLIT" + toChange.ping);
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                case UPDATE_DISPLAY_NAME:
                                    //ignore
                                    break;
                                case REMOVE_PLAYER:
                                    for (PlayerListEntry entry : pck.getEntries()) {
                                        String uuid = entry.getProfile().getId().toString();
                                        int removalIndex = -1;
                                        for (int i = 0; i < playerListEntries.size(); i++)  {
                                            TabListPlayer player = playerListEntries.get(i);
                                            if (uuid.equals(player.uuid))   {
                                                removalIndex = i;
                                                websocketServer.sendToAll("tabDel  " + player.name);
                                                break;
                                            }
                                        }
                                        if (removalIndex != -1) {
                                            playerListEntries.remove(removalIndex);
                                        }
                                    }
                                    break;
                            }
                        } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerListDataPacket) {
                            ServerPlayerListDataPacket pck = (ServerPlayerListDataPacket) packetReceivedEvent.getPacket();
                            tabHeader = pck.getHeader();
                            tabFooter = pck.getFooter();
                            String header = tabHeader.getFullText();
                            String footer = tabFooter.getFullText();
                            websocketServer.sendToAll("tabDiff " + header + "SPLIT" + footer);
                        }
                    } catch (Exception | Error e)   {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }

                @Override
                public void packetSent(PacketSentEvent packetSentEvent) {

                }

                @Override
                public void connected(ConnectedEvent connectedEvent) {
                    System.out.println("Connected to " + ip + ":" + port + "!");
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() { //antiafk
                            if (doAFK) {
                                if (r.nextBoolean()) {
                                    client.getSession().send(new ClientPlayerSwingArmPacket(Hand.MAIN_HAND));
                                } else {
                                    float yaw = -90 + (90 - -90) * r.nextFloat();
                                    float pitch = -90 + (90 - -90) * r.nextFloat();
                                    client.getSession().send(new ClientPlayerRotationPacket(false, yaw, pitch));
                                }
                            }
                        }
                    }, 20000, 500);

                    new Timer().schedule(new TimerTask() { // i actually want this in a seperate thread, no derp
                        @Override
                        public void run() { //chat
                            switch (r.nextInt(27))    {
                                case 0:
                                    sendChat("Did you know? The Did you know? meme is dead!");
                                    break;
                                case 1:
                                    sendChat("Contact me on Discord for new spam message suggestions! DaPorkchop_#2459");
                                    break;
                                case 2:
                                    sendChat("Pepsi > Coke");
                                    break;
                                case 3:
                                    sendChat("Did you know? VoCo is dead!");
                                    break;
                                case 4:
                                    sendChat("Welcome to TOOBEETOOTEEDOTORG! A friendly christian survival server!");
                                    break;
                                case 5:
                                    sendChat("-- HOURLY STATS -- Average queue size: 1000 Average TPS: 0.00");
                                    break;
                                case 6:
                                    sendChat("OMG it's FeetMC!!!");
                                    break;
                                case 7:
                                    sendChat("Daily reminder that Pepsi is better than Coke");
                                    break;
                                case 8:
                                    sendChat("卐卐卐 KILL HITLER 卐卐卐");
                                    break;
                                case 9:
                                    sendChat("team vet train best faction");
                                    break;
                                case 10:
                                    sendChat("Press F3+C for 15 seconds to dupe!");
                                    break;
                                case 11:
                                    sendChat("The cactus dupe is the best dupe!");
                                    break;
                                case 12:
                                    sendChat("I just walked " + (r.nextInt(75) + 3) + " blocks!");
                                    break;
                                case 13:
                                    sendChat("<insert meme here>");
                                    break;
                                case 14:
                                    sendChat("I just drank 1 Pepsi!");
                                    break;
                                case 15:
                                    sendChat("Daily reminder that pressing alt+F4 reduces lag");
                                    break;
                                case 16:
                                    sendChat("Position in queue: " + (r.nextInt(130) + 93));
                                    break;
                                case 17:
                                case 18:
                                case 19:
                                    sendChat("I just mined " + (r.nextInt(15) + 5) + " " + BLOCK_NAMES[r.nextInt(BLOCK_NAMES.length)] + "!");
                                    break;
                                case 20:
                                case 21:
                                case 22:
                                    sendChat("I just placed " + (r.nextInt(15) + 5) + " " + BLOCK_NAMES[r.nextInt(BLOCK_NAMES.length)] + "!");
                                    break;
                                case 23:
                                case 24:
                                case 25:
                                    sendChat("I just picked up " + (r.nextInt(15) + 5) + " " + BLOCK_NAMES[r.nextInt(BLOCK_NAMES.length)] + "!");
                                    break;
                                case 26:
                                    sendChat("kekekekekekekekekekepepsibetterthancokekekekekekekekekek");
                                    break;
                            }
                        }
                    }, 30000, 10000);
                }

                @Override
                public void disconnecting(DisconnectingEvent disconnectingEvent) {
                    System.out.println("Disconnecting... Reason: " + disconnectingEvent.getReason());
                    queuedMessages.add("Disconnecting. Reason: " + disconnectingEvent.getReason());
                    TooBeeTooTeeBot.INSTANCE.websocketServer.sendToAll("shutdown" + disconnectingEvent.getReason());

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e)    {

                    }
                    System.exit(0);
                }

                @Override
                public void disconnected(DisconnectedEvent disconnectedEvent) {
                    System.out.println("Disconnected.");
                    queuedMessages.add("Disconnecting. Reason: " + disconnectedEvent.getReason());
                    TooBeeTooTeeBot.INSTANCE.websocketServer.sendToAll("shutdown" + disconnectedEvent.getReason());

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e)    {

                    }
                    System.exit(0);
                }
            });
            System.out.println("Connecting to " + ip + ":" + port + "...");
            client.getSession().connect(true);
        } catch (Exception e)   {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void sendChat(String message)    {
        ClientChatPacket toSend = new ClientChatPacket("> #TeamPepsi " + message);
        client.getSession().send(toSend);
    }
}

/**
 * A bunch of utilities for dealing with Minecraft color codes
 * Totally not skidded from Nukkit
 * sorry nukkit
 * deal with it
 * kek
 */
enum TextFormat {
    /**
     * Represents black.
     */
    BLACK('0', 0x00),
    /**
     * Represents dark blue.
     */
    DARK_BLUE('1', 0x1),
    /**
     * Represents dark green.
     */
    DARK_GREEN('2', 0x2),
    /**
     * Represents dark blue (aqua).
     */
    DARK_AQUA('3', 0x3),
    /**
     * Represents dark red.
     */
    DARK_RED('4', 0x4),
    /**
     * Represents dark purple.
     */
    DARK_PURPLE('5', 0x5),
    /**
     * Represents gold.
     */
    GOLD('6', 0x6),
    /**
     * Represents gray.
     */
    GRAY('7', 0x7),
    /**
     * Represents dark gray.
     */
    DARK_GRAY('8', 0x8),
    /**
     * Represents blue.
     */
    BLUE('9', 0x9),
    /**
     * Represents green.
     */
    GREEN('a', 0xA),
    /**
     * Represents aqua.
     */
    AQUA('b', 0xB),
    /**
     * Represents red.
     */
    RED('c', 0xC),
    /**
     * Represents light purple.
     */
    LIGHT_PURPLE('d', 0xD),
    /**
     * Represents yellow.
     */
    YELLOW('e', 0xE),
    /**
     * Represents white.
     */
    WHITE('f', 0xF),
    /**
     * Makes the text obfuscated.
     */
    OBFUSCATED('k', 0x10, true),
    /**
     * Makes the text bold.
     */
    BOLD('l', 0x11, true),
    /**
     * Makes a line appear through the text.
     */
    STRIKETHROUGH('m', 0x12, true),
    /**
     * Makes the text appear underlined.
     */
    UNDERLINE('n', 0x13, true),
    /**
     * Makes the text italic.
     */
    ITALIC('o', 0x14, true),
    /**
     * Resets all previous chat colors or formats.
     */
    RESET('r', 0x15);

    /**
     * The special character which prefixes all format codes. Use this if
     * you need to dynamically convert format codes from your custom format.
     */
    public static final char ESCAPE = '\u00A7';

    private static final Pattern CLEAN_PATTERN = Pattern.compile("(?i)" + String.valueOf(ESCAPE) + "[0-9A-FK-OR]");
    private final static Map<Integer, TextFormat> BY_ID = Maps.newTreeMap();
    private final static Map<Character, TextFormat> BY_CHAR = new HashMap<>();

    static {
        for (TextFormat color : values()) {
            BY_ID.put(color.intCode, color);
            BY_CHAR.put(color.code, color);
        }
    }

    private final int intCode;
    private final char code;
    private final boolean isFormat;
    private final String toString;

    TextFormat(char code, int intCode) {
        this(code, intCode, false);
    }

    TextFormat(char code, int intCode, boolean isFormat) {
        this.code = code;
        this.intCode = intCode;
        this.isFormat = isFormat;
        this.toString = new String(new char[]{ESCAPE, code});
    }

    /**
     * Gets the TextFormat represented by the specified format code.
     *
     * @param code Code to check
     * @return Associative {@link TextFormat} with the given code,
     * or null if it doesn't exist
     */
    public static TextFormat getByChar(char code) {
        return BY_CHAR.get(code);
    }

    /**
     * Gets the TextFormat represented by the specified format code.
     *
     * @param code Code to check
     * @return Associative {@link TextFormat} with the given code,
     * or null if it doesn't exist
     */
    public static TextFormat getByChar(String code) {
        if (code == null || code.length() <= 1) {
            return null;
        }

        return BY_CHAR.get(code.charAt(0));
    }

    /**
     * Cleans the given message of all format codes.
     *
     * @param input String to clean.
     * @return A copy of the input string, without any formatting.
     */
    public static String clean(final String input) {
        if (input == null) {
            return null;
        }

        return CLEAN_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Translates a string using an alternate format code character into a
     * string that uses the internal TextFormat.ESCAPE format code
     * character. The alternate format code character will only be replaced if
     * it is immediately followed by 0-9, A-F, a-f, K-O, k-o, R or r.
     *
     * @param altFormatChar   The alternate format code character to replace. Ex: &
     * @param textToTranslate Text containing the alternate format code character.
     * @return Text containing the TextFormat.ESCAPE format code character.
     */
    public static String colorize(char altFormatChar, String textToTranslate) {
        return colorize(altFormatChar, textToTranslate, false);
    }

    /**
     * Translates a string using an alternate format code character into a
     * string that uses the internal TextFormat.ESCAPE format code
     * character. The alternate format code character will only be replaced if
     * it is immediately followed by 0-9, A-F, a-f, K-O, k-o, R or r.
     *
     * @param altFormatChar   The alternate format code character to replace. Ex: &
     * @param textToTranslate Text containing the alternate format code character.
     * @param resetFormatting If RESET should be added before a color change
     * @return Text containing the TextFormat.ESCAPE format code character.
     */
    public static String colorize(char altFormatChar, String textToTranslate, boolean resetFormatting) {
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altFormatChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = TextFormat.ESCAPE;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        String str = new String(b);
        if (resetFormatting) {
            Matcher match = Pattern.compile("§([0-9a-f])").matcher(str);
            int idx = 0;
            while (match.find()) {
                str = str.replace("§" + match.group(idx), "§r§" + match.group(idx));
                idx++;
            }
        }
        return str;
    }

    /**
     * Translates a string, using an ampersand (&) as an alternate format code
     * character, into a string that uses the internal TextFormat.ESCAPE format
     * code character. The alternate format code character will only be replaced if
     * it is immediately followed by 0-9, A-F, a-f, K-O, k-o, R or r.
     *
     * @param textToTranslate Text containing the alternate format code character.
     * @return Text containing the TextFormat.ESCAPE format code character.
     */
    public static String colorize(String textToTranslate) {
        return colorize('&', textToTranslate);
    }

    /**
     * Gets the chat color used at the end of the given input string.
     *
     * @param input Input string to retrieve the colors from.
     * @return Any remaining chat color to pass onto the next line.
     */
    public static String getLastColors(String input) {
        String result = "";
        int length = input.length();

        // Search backwards from the end as it is faster
        for (int index = length - 1; index > -1; index--) {
            char section = input.charAt(index);
            if (section == ESCAPE && index < length - 1) {
                char c = input.charAt(index + 1);
                TextFormat color = getByChar(c);

                if (color != null) {
                    result = color.toString() + result;

                    // Once we find a color or reset we can stop searching
                    if (color.isColor() || color.equals(RESET)) {
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Gets the char value associated with this color
     *
     * @return A char value of this color code
     */
    public char getChar() {
        return code;
    }

    @Override
    public String toString() {
        return toString;
    }

    /**
     * Checks if this code is a format code as opposed to a color code.
     */
    public boolean isFormat() {
        return isFormat;
    }

    /**
     * Checks if this code is a color code as opposed to a format code.
     */
    public boolean isColor() {
        return !isFormat && this != RESET;
    }
}
