package tk.daporkchop.toobeetooteebot;

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import tk.daporkchop.toobeetooteebot.server.PorkClient;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* * A bunch of utilities for dealing with Minecraft color codes
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

public class TooBeeTooTeeBot {

	public static final String[] BLOCK_NAMES = new String[] { "Cobblestone", "Stone", "Netherrack", "Stone Bricks", "Block of Coal", "Block of Iron", "Block of Gold", "Block of Diamond", "Block of Emerald", "Obsidian" };
    public static TooBeeTooTeeBot INSTANCE;
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
    public DataTag dataTag = new DataTag(new File(System.getProperty("user.dir") + File.separator + "players.dat"));
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
    protected boolean hasDonePostConnect = false;

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
            INSTANCE.dataTag.setSerializable("registeredPlayers", INSTANCE.namesToRegisteredPlayers);
            INSTANCE.dataTag.save();
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

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        doPostConnectSetup();
                    }
                }, 10000);
            }

            if (doAuth) {
                File sessionIdCache = new File(System.getProperty("user.dir") + File.separator + "sessionId.txt");
                if (sessionIdCache.exists())    {
                    System.out.println("Attempting to log in with session ID");
                    Scanner s = new Scanner(sessionIdCache);
                    String sessionID = s.nextLine().trim();
                    s.close();
                    System.out.println("Session ID: " + sessionID);
                    try {
                        protocol = new MinecraftProtocol(username, sessionID, true);
                    } catch (RequestException e)    {
                        System.out.println("Bad/expired session ID, attempting login with username and password");
                        protocol = new MinecraftProtocol(username, password);

                        System.out.println("Logged in with credentials " + username + ":" + password);
                        System.out.println("Saving session ID: " + sessionID + " to disk");
                        PrintWriter writer = new PrintWriter("sessionId.txt", "UTF-8");
                        writer.println(protocol.getAccessToken());
                        writer.close();
                    }
                } else {
                    System.out.println("Attempting login with username and password...");
                    protocol = new MinecraftProtocol(username, password);

                    System.out.println("Logged in with credentials " + username + ":" + password);
                    System.out.println("Saving session ID: " + protocol.getAccessToken() + " to disk");
                    PrintWriter writer = new PrintWriter("sessionId.txt", "UTF-8");
                    writer.println(protocol.getAccessToken());
                    writer.close();
                }
            } else {
                System.out.println("Loggin in with cracked account, username: " + username);
                protocol = new MinecraftProtocol(username);
            }
            System.out.println("Success!");

            client = new Client(ip, port, protocol, new TcpSessionFactory());
            client.getSession().addListener(new PorkSessionListener(this));
            System.out.println("Connecting to " + ip + ":" + port + "...");
            client.getSession().connect(true);
        } catch (Exception e)   {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void sendChat(String message)    {
        ClientChatPacket toSend = new ClientChatPacket("> #TeamPepsi " + message);
        //client.getSession().send(toSend);
    }

    public void processMsg(String playername, String message)   {
        //TODO: limit commands per player per time
        RegisteredPlayer player = namesToRegisteredPlayers.getOrDefault(playername, null);
        if (player == null) {
            NotRegisteredPlayer tempAuth = namesToTempAuths.getOrDefault(playername, null);
            if (tempAuth == null)   {
                client.getSession().send(new ClientChatPacket("/msg " + playername + " You're not registered! Go to http://www.daporkchop.net/pork2b2tbot to register!"));
                return;
            } else if (message.startsWith("register")) {
                message = message.substring(9);
                if (message.startsWith(tempAuth.tempAuthUUID))   {
                    String hashedPwd = Hashing.sha256().hashString(tempAuth.pwd, Charsets.UTF_8).toString();
                    RegisteredPlayer newPlayer = new RegisteredPlayer(hashedPwd, tempAuth.name);
                    newPlayer.lastUsed = System.currentTimeMillis();
                    namesToTempAuths.remove(tempAuth.name);
                    namesToRegisteredPlayers.put(tempAuth.name, newPlayer);
                    dataTag.setSerializable("registeredPlayers", namesToRegisteredPlayers);
                    client.getSession().send(new ClientChatPacket("/msg " + playername + " Successfully registered! You can now use your username and password on the website!"));
                    return;
                } else {
                    client.getSession().send(new ClientChatPacket("/msg " + playername + " Incorrect authentication UUID!"));
                    return;
                }
            }
        } else {
            if (message.startsWith("help")) {
                client.getSession().send(new ClientChatPacket("/msg " + playername + " Use '/msg 2pork2bot <player name> <message>' to send them a message! Visit http://www.daporkchop.net/pork2b2tbot for more info!"));
                return;
            } else if (message.startsWith("changepass")) {
                String[] messageSplit = message.split(" ");
                String sha1 = Hashing.sha1().hashString(messageSplit[1], Charsets.UTF_8).toString();
                String sha256 = Hashing.sha256().hashString(sha1, Charsets.UTF_8).toString();
                player.passwordHash = sha256;
                client.getSession().send(new ClientChatPacket("/msg " + playername + " Changed password to " + messageSplit[1] + " (sha1: " + sha1 + ")"));
            } else {
                //TODO: manage a chache of logged in users
                String[] messageSplit = message.split(" ");
                LoggedInPlayer loggedInPlayer = namesToLoggedInPlayers.getOrDefault(messageSplit[0], null);
                if (loggedInPlayer == null) {
                    client.getSession().send(new ClientChatPacket("/msg " + playername + " The user " + messageSplit[0] + " could not be found! They might be idle, or they aren't logged in to the website!"));
                    return;
                } else {
                    loggedInPlayer.clientSocket.send("chat    \u00A7d" + playername + " says: " + message.substring(messageSplit[0].length() + 1));
                    client.getSession().send(new ClientChatPacket("/msg " + playername + " Sent message to " + messageSplit[0]));
                    return;
                }
                //TODO: message queue
            }
        }
    }

    public void doPostConnectSetup()    {
        try {
            if (!hasDonePostConnect) {
                TooBeeTooTeeBot.INSTANCE.websocketServer = new WebsocketServer(8888);
                TooBeeTooTeeBot.INSTANCE.websocketServer.start();

                namesToRegisteredPlayers = (HashMap<String, RegisteredPlayer>) dataTag.getSerializable("registeredPlayers", new HashMap<String, RegisteredPlayer>());

                jda = new JDABuilder(AccountType.BOT)
                        .setToken(token)
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
                hasDonePostConnect = true;
            }
        } catch (Exception e)   {
            e.printStackTrace();
        }
    }
}
