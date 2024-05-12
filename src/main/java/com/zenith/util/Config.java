package com.zenith.util;

import com.google.gson.annotations.SerializedName;
import com.zenith.feature.whitelist.PlayerEntry;
import com.zenith.module.impl.ActiveHours.ActiveTime;
import lombok.Getter;
import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.ArrayList;

import static java.util.Arrays.asList;


public final class Config {
    public final Authentication authentication = new Authentication();
    public final Client client = new Client();
    public final Debug debug = new Debug();
    public final Server server = new Server();
    public final InteractiveTerminal interactiveTerminal = new InteractiveTerminal();
    public final InGameCommands inGameCommands = new InGameCommands();
    public final Theme theme = new Theme();
    public final Discord discord = new Discord();
    public final Database database = new Database();
    public final AutoUpdater autoUpdater = new AutoUpdater();

    public static final class Authentication {
        public AccountType accountType = AccountType.DEVICE_CODE;
        // only used for MSA
        public String email = "not@set.com";
        public String password = "abc123";
        // updated on successful login
        public String username = "Unknown";
        public boolean prio = false;
        public boolean prioBanned = false;
        public boolean authTokenRefresh = true;
        public int msaLoginAttemptsBeforeCacheWipe = 2;
        public boolean openBrowserOnLogin = true;
        public boolean alwaysRefreshOnLogin = false;
        public int maxRefreshIntervalMins = 360; // 6 hrs
        public boolean useClientConnectionProxy = false;

        public enum AccountType {
            @SerializedName("msa") MSA,
            @SerializedName("device_code") DEVICE_CODE,
            @SerializedName("local_webserver") LOCAL_WEBSERVER,
            @SerializedName("device_code_without_device_token") DEVICE_CODE_WITHOUT_DEVICE_TOKEN,
            @SerializedName("prism") PRISM
        }
    }

    public static final class Theme {
        public ConfigColor primary = ConfigColor.CYAN;
        public ConfigColor success = ConfigColor.MEDIUM_SEA_GREEN;
        public ConfigColor error = ConfigColor.RUBY;
        public ConfigColor inQueue = ConfigColor.MOON_YELLOW;
    }

    public static final class Client {
        public final Extra extra = new Extra();
        public final Server server = new Server();
        public final ConnectionProxy connectionProxy = new ConnectionProxy();
        public int compressionLevel = -1;
        public boolean autoConnect = false; // auto-connect proxy on process start
        public final ClientViaVersion viaversion = new ClientViaVersion();
        public String bindAddress = "0.0.0.0";
        public boolean maxPlaytimeReconnect = false;
        public long maxPlaytimeReconnectMins = 1440;
        public final ClientTimeout timeout = new ClientTimeout();
        public final Ping ping = new Ping();

        public static final class ClientViaVersion {
            public boolean enabled = false;
            public boolean autoProtocolVersion = true;
            public int protocolVersion = 762; // 1.19.4
        }

        public static final class ClientTimeout {
            public boolean enable = true;
            public int seconds = 60;
        }

        public static final class Ping {
            public Mode mode = Mode.TABLIST;
            public int packetPingIntervalSeconds = 10;

            public enum Mode {
                TABLIST, PACKET;
            }
        }

        public static final class Extra {
            public final AntiAFK antiafk = new AntiAFK();
            public final Spook spook = new Spook();
            public final Utility utility = new Utility();
            public final AutoReconnect autoReconnect = new AutoReconnect();
            public final AutoRespawn autoRespawn = new AutoRespawn();
            public final Spammer spammer = new Spammer();
            public final AutoReply autoReply = new AutoReply();
            public final Stalk stalk = new Stalk();
            public final AutoEat autoEat = new AutoEat();
            public final AutoFish autoFish = new AutoFish();
            public final KillAura killAura = new KillAura();
            public final AutoTotem autoTotem = new AutoTotem();
            public final AntiLeak antiLeak = new AntiLeak();
            public final Chat chat = new Chat();
            public final AntiKick antiKick = new AntiKick();
            public final ReplayMod replayMod = new ReplayMod();
            public final ArrayList<PlayerEntry> friendsList = new ArrayList<>();
            public boolean clientConnectionMessages = true;
            public boolean autoConnectOnLogin = true;
            public boolean prioBan2b2tCheck = true;
            public boolean prioStatusChangeMention = true;
            public boolean killMessage = true;
            public boolean logChatMessages = true;
            public final ActionLimiter actionLimiter = new ActionLimiter();
            public final VisualRange visualRange = new VisualRange();
            public final AutoArmor autoArmor = new AutoArmor();

            public static class VisualRange {
                public boolean enabled = true;
                public boolean ignoreFriends = false;
                public boolean enterAlert = true;
                public boolean enterAlertMention = true;
                public boolean leaveAlert = true;
                public boolean logoutAlert = true;
                public boolean replayRecording = false;
                public ReplayRecordingMode replayRecordingMode = ReplayRecordingMode.ENEMY;
                public int replayRecordingCooldownMins = 5;
                public enum ReplayRecordingMode {
                    ENEMY,
                    ALL
                }
            }

            public static class AutoArmor {
                public boolean enabled = false;
            }

            public static class AntiKick {
                public boolean enabled = false;
                public int playerInactivityKickMins = 15;
                public int minWalkDistance = 2;
            }

            public static final class Chat {
                public final ArrayList<PlayerEntry> ignoreList = new ArrayList<>();
                public boolean hideChat = false;
                public boolean hideWhispers = false;
                public boolean hideDeathMessages = false;
                public boolean showConnectionMessages = false;
            }

            public static final class AutoTotem {
                public boolean enabled = true;
                public int healthThreshold = 20;
                public boolean noTotemsAlert = false;
                public boolean noTotemsAlertMention = false;
                public boolean totemPopAlert = false;
                public boolean totemPopAlertMention = false;
            }

            public static final class AntiLeak {
                public boolean enabled = true;
                // checks if numbers in chat are within a range from your coords
                public boolean rangeCheck = true;
                // the factor to divide and multiply your coords by to get the range
                public double rangeFactor = 10.0;
            }

            public static final class KillAura {
                public boolean enabled = true;
                public boolean targetPlayers = false;
                public boolean targetHostileMobs = true;
                public boolean targetNeutralMobs = false;
                public boolean targetCustom = false;
                public boolean onlyNeutralAggressive = true;
                public boolean switchWeapon = true;
                public boolean targetArmorStands = false;
                public int attackDelayTicks = 10;
                public double attackRange = 3.5;
                public final ArrayList<EntityType> customTargets = new ArrayList<>();
            }

            public static final class AutoEat {
                public boolean enabled = true;
                public int healthThreshold = 10;
                public int hungerThreshold = 10;
                public boolean warning = true;
            }

            public static final class Stalk {
                public boolean enabled = false;
                public final ArrayList<PlayerEntry> stalking = new ArrayList<>();
            }

            public static final class AutoFish {
                public boolean enabled = false;
                public long castDelay = 20;
                public float yaw = 0.0f;
                public float pitch = 0.0f;
            }

            public static final class AntiAFK {
                public Actions actions = new Actions();
                public boolean enabled = true;

                public static final class Actions {
                    public boolean walk = true;
                    // we only need about 5-6 blocks in reality but adding a few extra here to be safe
                    // this isn't dependent on chunks loading but is more based on distance
                    public int walkDistance = 8;
                    // avoid going off ledges even when falls are non-fatal
                    public boolean safeWalk = true;
                    public long walkDelayTicks = 400;
                    public boolean swingHand = true;
                    public long swingDelayTicks = 3000;
                    public boolean rotate = true;
                    public long rotateDelayTicks = 300L;
                    public boolean jump = false;
                    public boolean jumpOnlyInWater = true;
                    public long jumpDelayTicks = 1L;
                    public boolean sneak = false;
                }
            }

            public static final class Spook {
                public boolean enabled = false;
                public Long tickDelay = 0L;
                public TargetingMode spookTargetingMode = TargetingMode.VISUAL_RANGE;

                public enum TargetingMode {
                    NEAREST,
                    VISUAL_RANGE
                }
            }

            public static final class ReplayMod {
                public boolean sendRecordingsToDiscord = false;
                public int maxRecordingTimeMins = 0;
                public AutoRecordMode autoRecordMode = AutoRecordMode.NONE;

                @Getter
                public enum AutoRecordMode {
                    NONE("off"),
                    PROXY_CONNECTED("proxyConnected"),
                    PLAYER_CONNECTED("playerConnected");
                    private final String name;

                    AutoRecordMode(String name) {
                        this.name = name;
                    }
                }
            }

            public static final class Utility {
                public final Actions actions = new Actions();

                public static final class Actions {
                    public final AutoDisconnect autoDisconnect = new AutoDisconnect();
                    public final ActiveHours activeHours = new ActiveHours();
                }

                public static final class AutoDisconnect {
                    public boolean enabled = false;
                    public boolean whilePlayerConnected = false;
                    public boolean autoClientDisconnect = false;
                    public int health = 5;
                    public boolean thunder = false;
                    public boolean cancelAutoReconnect = true;
                    // checks friends list, whitelist, and spectator whitelist
                    public boolean onUnknownPlayerInVisualRange = false;
                }

                public static final class ActiveHours {
                    public boolean enabled = false;
                    public boolean forceReconnect = false;
                    public boolean queueEtaCalc = true;
                    public String timeZoneId = "Universal";
                    public final ArrayList<ActiveTime> activeTimes = new ArrayList<>();
                }
            }

            public static final class AutoReconnect {
                public boolean enabled = true;
                public int delaySeconds = 120;
                public int maxAttempts = 200;
            }

            public static final class AutoRespawn {
                public boolean enabled = true;
                public int delayMillis = 100;
            }

            public static final class Spammer {
                public boolean enabled = false;
                public boolean whisper = false;
                public long delayTicks = 200;
                public boolean randomOrder = false;
                public boolean appendRandom = false;
                public final ArrayList<String> messages = new ArrayList<>(asList(
                        "ZenithProxy on top!",
                        "I just skipped queue thanks to ZenithProxy!",
                        "Download ZenithProxy on GitHub today! It's free!"
                ));
            }

            public static final class AutoReply {
                public boolean enabled = false;
                public int cooldownSeconds = 15;
                public String message = "I am currently AFK, check back later or message me on discord.";
            }
            public static class ActionLimiter {
                public boolean enabled = false;
                // be careful with this, auto respawn will still respawn after they disconnect
                //  there is a position check at login so it should be ok, but the respawn will still go through
                public boolean allowRespawn = true;
                public boolean allowMovement = true;
                public int movementDistance = 1000; // distance from home coords
                public int movementHomeX = 0;
                public int movementHomeZ = 0;
                public int movementMinY = -64;
                public boolean allowEnderChest = true;
                public boolean allowBlockBreaking = true;
                // todo: dunno how to block this but still allow other interactions
//                public boolean allowBlockPlacing = true;
                public boolean allowInventory = true;
                public boolean allowUseItem = true;
                public boolean allowBookSigning = true;
                public boolean allowInteract = true;
                public boolean allowChat = true; // outbound chats, including commands
            }
        }

        public static final class Server {
            public String address = "connect.2b2t.org";
            public int port = 25565;
        }

        public static final class ConnectionProxy {
            public boolean enabled = false;
            public ProxyInfo.Type type = ProxyInfo.Type.SOCKS5;
            public String host = "127.0.0.1";
            public int port = 7890;
            public String user = "";
            public String password = "";
        }
    }

    public static final class Debug {
        public final PacketLog packetLog = new PacketLog();
        public final Server server = new Server();
        public boolean clearOldLogs = false;
        public boolean sendChunksBeforePlayerSpawn = false;
        public boolean binaryNbtComponentSerializer = true;
        public boolean kickDisconnect = false;
        public boolean teleportResync = false;

        public static final class PacketLog {
            public boolean enabled = false;
            public PacketLogConfig clientPacketLog = new PacketLogConfig();
            public PacketLogConfig serverPacketLog = new PacketLogConfig();
            // todo: could be more flexible, but this can cover the most basic use cases
            public String packetFilter = "";

            public static final class PacketLogConfig {
                public boolean received = false;
                public boolean receivedBody = false;
                public boolean preSent = false;
                public boolean preSentBody = false;
                public boolean postSent = false;
                public boolean postSentBody = false;
            }
        }

        public static final class Server {
            public final Cache cache = new Cache();

            public static final class Cache {
                public boolean sendingmessages = true;
                public boolean unknownplayers = false;
            }
        }
    }

    public static final class Server {
        public final Bind bind = new Bind();
        public int compressionThreshold = 256;
        public int compressionLevel = -1;
        public boolean enabled = true;
        public final Extra extra = new Extra();
        public final Ping ping = new Ping();
        public final ServerViaVersion viaversion = new ServerViaVersion();
        public boolean verifyUsers = true;
        public boolean kickPrevious = false;
        public String proxyIP = "localhost";
        public int queueStatusRefreshMinutes = 5; // how often to refresh queue lengths
        public boolean healthCheck = true;
        public long playerListsRefreshIntervalMins = 1440L; // one day as default
        public final Spectator spectator = new Spectator();
        public final RateLimiter rateLimiter = new RateLimiter();

        public static final class RateLimiter {
            public boolean enabled = true;
            public int rateLimitSeconds = 2;
        }

        public static final class Spectator {
            public boolean allowSpectator = true;
            public String spectatorEntity = "cat";
            public boolean spectatorPublicChatEnabled = true;

            public ArrayList<PlayerEntry> whitelist = new ArrayList<>();
            // todo: log spectator chats to discord relay and terminal
            //  both from spectators and to spectators from controlling player
            public boolean logSpectatorChats = false;
        }


        public static final class Bind {
            public String address = "0.0.0.0";
            public int port = 25565;
        }

        public static final class Extra {
            public final ServerTimeout timeout = new ServerTimeout();
            public final Whitelist whitelist = new Whitelist();
            public final ESP esp = new ESP();
            public final ChatHistory chatHistory = new ChatHistory();

            public static class ChatHistory {
                public boolean enable = false;
                public int seconds = 30;
                public int maxCount = 10;
                public boolean spectators = true;
            }

            public static final class Whitelist {
                public boolean enable = true;
                public final ArrayList<PlayerEntry> whitelist = new ArrayList<>();
                public String kickmsg = "no whitelist?";
                // Automatically adds the proxy client account to the whitelist if not present
                // does not remove any entries
                public boolean autoAddClient = true;
            }

            public static final class ServerTimeout {
                public boolean enable = true;
                public int seconds = 30;
            }

            public static final class ESP {
                public boolean enable = false;
            }
        }

        public static final class Ping {
            public boolean enabled = true;
            public boolean onlinePlayers = true;
            public boolean onlinePlayerCount = true;
            public boolean favicon = true;
            public int maxPlayers = Integer.MAX_VALUE;
            public boolean lanBroadcast = true;
            public boolean responseCaching = true;
            // could probably be increased 2-3x without issue
            public int responseCacheSeconds = 10;
            public boolean logPings = false;
        }

        public static final class ServerViaVersion {
            public boolean enabled = true;
            public boolean autoRemoveFromPipeline = true;
        }

        public String getProxyAddress() {
            // if the proxy IP is not a DNS name, also return the port appended
            if (!this.proxyIP.contains(":") // port already appended
                && this.proxyIP.contains("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) // IP address
                return this.proxyIP + ":" + this.bind.port;
             else
                return this.proxyIP;
        }
    }

    public static final class InteractiveTerminal {
        public boolean enable = true;
        public boolean logToDiscord = true;
    }
    public static final class InGameCommands {
        public boolean enable = true;
        public boolean slashCommands = true;
        public boolean slashCommandsReplacesServerCommands = false;
        public String prefix = "!";
        public boolean logToDiscord = true;
    }
    public static final class Discord {
        public boolean enable = false;
        public String token = "";
        public String channelId = "";
        public String accountOwnerRoleId = "";
        public String visualRangeMentionRoleId = "";
        public String prefix = ".";
        public boolean reportCoords = true;
        public boolean mentionRoleOnPrioUpdate = true;
        public boolean mentionRoleOnPrioBanUpdate = true;
        public boolean mentionRoleOnDeviceCodeAuth = true;
        public boolean manageProfileImage = true;
        public boolean manageNickname = true;
        public boolean manageDescription = true;
        public boolean showNonWhitelistLoginIP = true;
        public boolean isUpdating = false; // internal use for update command state persistence
        public final QueueWarning queueWarning = new QueueWarning();
        public final ChatRelay chatRelay = new ChatRelay();
        public final ConnectionProxy connectionProxy = new ConnectionProxy();
        public static final class ConnectionProxy {
                public boolean enabled = false;
                public ConnectionProxyType type = ConnectionProxyType.HTTP;
                public String host = "127.0.0.1";
                public int port = 7890;
                public String user = "";
                public String password = "";

                public enum ConnectionProxyType {
                    HTTP, SOCKS4, SOCKS5;
                }
        }
        public static final class QueueWarning {
            public boolean enabled = true;
            public int position = 10; // Queue position to send warning message at
            public boolean mentionRole = false;
        }

        public static class ChatRelay {
            public boolean enable = false;
            public boolean ignoreQueue = true;
            public boolean mentionRoleOnWhisper = true;
            public boolean mentionRoleOnNameMention = true;
            public boolean mentionWhileConnected = false;
            public boolean connectionMessages = false;
            public boolean publicChats = true;
            public boolean whispers = true;
            public boolean serverMessages = true;
            public boolean deathMessages = true;
            public boolean sendMessages = true;
            public String channelId = "";
        }
    }

    public static final class Database {
        public boolean enabled = false;
        public String host = "";
        public int port = 5432;
        public String username = "";
        public String password = "";
        public int writePool = 1;
        public final QueueWait queueWait = new QueueWait();
        public final Connections connections = new Connections();
        public final Chats chats = new Chats();
        public final Deaths deaths = new Deaths();
        public final QueueLength queueLength = new QueueLength();
        public final Restarts restarts = new Restarts();
        public final PlayerCount playerCount = new PlayerCount();
        public final Tablist tablist = new Tablist();
        public final Lock lock = new Lock();

        public static final class QueueWait {
            // queue wait time monitor
            public boolean enabled = true;
        }

        public static final class Lock {
            // use "rediss://" for SSL connection
            public String redisAddress = "redis://localhost:7181";
            public String redisUsername = "";
            public String redisPassword = "";
        }

        public static final class Connections {
            public boolean enabled = true;
        }

        public static final class Chats {
            public boolean enabled = true;
        }

        public static final class Deaths {
            public boolean enabled = true;
            public boolean unknownDeathDiscordMsg = false;
        }

        public static final class QueueLength {
            public boolean enabled = true;
        }

        public static final class Restarts {
            public boolean enabled = true;
        }

        public static final class PlayerCount {
            public boolean enabled = true;
        }

        public static final class Tablist {
            public boolean enabled = true;
        }
    }

    public static final class AutoUpdater {
        public int autoUpdateCheckIntervalSeconds = 300;
        // internal config, don't set this manually
        public boolean shouldReconnectAfterAutoUpdate = false;
    }

    private transient boolean donePostLoad = false;

    public synchronized Config doPostLoad() {
        if (this.donePostLoad) {
            throw new IllegalStateException("Config post-load already done!");
        }
        this.donePostLoad = true;

        return this;
    }
}
