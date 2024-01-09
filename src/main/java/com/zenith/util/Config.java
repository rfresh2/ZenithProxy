package com.zenith.util;

import com.google.gson.annotations.SerializedName;
import com.zenith.feature.whitelist.PlayerEntry;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import static java.util.Arrays.asList;


public final class Config {
    public Authentication authentication = new Authentication();
    public Client client = new Client();
    public Debug debug = new Debug();
    public Server server = new Server();
    public InteractiveTerminal interactiveTerminal = new InteractiveTerminal();
    public InGameCommands inGameCommands = new InGameCommands();
    public Discord discord = new Discord();
    public Database database = new Database();
    public AutoUpdater autoUpdater = new AutoUpdater();

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

        public enum AccountType {
            @SerializedName("msa") MSA,
            @SerializedName("device_code") DEVICE_CODE,
            @SerializedName("local_webserver") LOCAL_WEBSERVER
        }
    }

    public static final class Client {
        public Extra extra = new Extra();
        public Server server = new Server();
        public boolean autoConnect = false; // auto-connect proxy on process start
        public ViaVersion viaversion = new ViaVersion();
        public String bindAddress = "0.0.0.0";
        public boolean maxPlaytimeReconnect = false;
        public long maxPlaytimeReconnectMins = 1440;

        public static final class ViaVersion {
            public boolean enabled = false;
            public boolean autoProtocolVersion = true;
            public int protocolVersion = 762; // 1.19.4
        }

        public static final class Extra {
            public AntiAFK antiafk = new AntiAFK();
            public Spook spook = new Spook();
            public Utility utility = new Utility();
            public AutoReconnect autoReconnect = new AutoReconnect();
            public AutoRespawn autoRespawn = new AutoRespawn();
            public Spammer spammer = new Spammer();
            public AutoReply autoReply = new AutoReply();
            public Stalk stalk = new Stalk();
            public AutoEat autoEat = new AutoEat();
            public AutoFish autoFish = new AutoFish();
            public KillAura killAura = new KillAura();
            public AutoTotem autoTotem = new AutoTotem();
            public AntiLeak antiLeak = new AntiLeak();
            public Chat chat = new Chat();
            public boolean visualRangeAlert = true;
            public boolean visualRangeIgnoreFriends = false;
            public boolean visualRangeAlertMention = false;
            public boolean visualRangePositionTracking = false;
            public boolean visualRangeLeftAlert = true;
            public boolean visualRangeLeftLogoutAlert = true;
            public ArrayList<PlayerEntry> friendsList = new ArrayList<>();
            public boolean clientConnectionMessages = true;
            public boolean autoConnectOnLogin = true;
            public boolean prioBan2b2tCheck = true;
            public boolean prioStatusChangeMention = true;
            public boolean killMessage = true;
            public ActionLimiter actionLimiter = new ActionLimiter();

            public static final class Chat {
                public ArrayList<PlayerEntry> ignoreList = new ArrayList<>();
                public boolean hideChat = false;
                public boolean hideWhispers = false;
                public boolean hideDeathMessages = false;
                public boolean showConnectionMessages = false;

            }

            public static final class AutoTotem {
                public boolean enabled = true;
                public int healthThreshold = 20;
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
                public boolean switchWeapon = true;
                public boolean targetArmorStands = false;
                public int attackDelayTicks = 10;
                public double attackRange = 3.5;
            }

            public static final class AutoEat {
                public boolean enabled = true;
                public int healthThreshold = 10;
                public int hungerThreshold = 10;
                public boolean warning = true;
            }

            public static final class Stalk {
                public boolean enabled = false;
                public ArrayList<PlayerEntry> stalking = new ArrayList<>();
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
                    public long jumpDelayTicks = 1000L;
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

            public static final class Utility {
                public Actions actions = new Actions();

                public static final class Actions {
                    public AutoDisconnect autoDisconnect = new AutoDisconnect();
                    public ActiveHours activeHours = new ActiveHours();
                }

                public static final class AutoDisconnect {
                    public boolean enabled = false;
                    public boolean autoClientDisconnect = false;
                    public int health = 5;
                    public boolean thunder = false;
                    public boolean cancelAutoReconnect = false;
                }

                public static final class ActiveHours {
                    public boolean enabled = false;
                    public boolean forceReconnect = false;
                    public String timeZoneId = "Universal";
                    public ArrayList<ActiveTime> activeTimes = new ArrayList<>();

                    public static class ActiveTime {
                        public int hour;
                        public int minute;

                        public static ActiveTime fromString(final String arg) {
                            final String[] split = arg.split(":");
                            final int hour = Integer.parseInt(split[0]);
                            final int minute = Integer.parseInt(split[1]);
                            ActiveTime activeTime = new ActiveTime();
                            activeTime.hour = hour;
                            activeTime.minute = minute;
                            return activeTime;
                        }

                        @Override
                        public String toString() {
                            return (hour < 10 ? "0" + hour : hour) + ":" + (minute < 10 ? "0" + minute : minute);
                        }

                        @Override
                        public boolean equals(Object o) {
                            if (this == o) return true;
                            if (o == null || getClass() != o.getClass()) return false;
                            ActiveTime that = (ActiveTime) o;
                            return hour == that.hour && minute == that.minute;
                        }

                        @Override
                        public int hashCode() {
                            return Objects.hash(hour, minute);
                        }

                    }
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
                public ArrayList<String> messages = new ArrayList<>(asList(
                        "ZenithProxy on top!",
                        "I just skipped queue thanks to ZenithProxy!",
                        "I love undertime slopper!",
                        ">odpay supremacy"
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
                public boolean allowInteract = true;
                public boolean allowChat = true; // outbound chats, including commands
            }
        }

        public static final class Server {
            public String address = "connect.2b2t.org";
            public int port = 25565;
        }
    }

    public static final class Debug {
        public Packet packet = new Packet();
        public Server server = new Server();

        public static final class Packet {
            public boolean received = false;
            public boolean receivedBody = false;
            public boolean preSent = false;
            public boolean preSentBody = false;
            public boolean postSent = false;
            public boolean postSentBody = false;
        }

        public static final class Server {
            public Cache cache = new Cache();

            public static final class Cache {
                public boolean sendingmessages = true;
                public boolean unknownplayers = false;
            }
        }
    }

    public static final class Server {
        public Bind bind = new Bind();
        public int compressionThreshold = 256;
        public boolean enabled = true;
        public Extra extra = new Extra();
        public Ping ping = new Ping();
        public boolean verifyUsers = true;
        public boolean kickPrevious = false;
        public String proxyIP = "localhost";
        public int queueStatusRefreshMinutes = 5; // how often to refresh queue lengths
        public boolean healthCheck = true;
        public long playerListsRefreshIntervalMins = 1440L; // one day as default

        public Spectator spectator = new Spectator();

        public static final class Spectator {
            public boolean allowSpectator = true;
            public String spectatorEntity = "cat";
            public boolean spectatorPublicChatEnabled = true;
            // skin shown in-game for spectators
            public UUID spectatorUUID = UUID.fromString("c9560dfb-a792-4226-ad06-db1b6dc40b95");

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
            public Timeout timeout = new Timeout();
            public Whitelist whitelist = new Whitelist();

            public static final class Whitelist {
                public boolean enable = true;
                public ArrayList<PlayerEntry> whitelist = new ArrayList<>();
                public String kickmsg = "no whitelist?";
            }

            public static final class Timeout {
                public boolean enable = true;
                public int seconds = 30;
            }
        }

        public static final class Ping {
            public boolean enabled = true;
            public boolean onlinePlayers = true;
            public boolean favicon = true;
            public int maxPlayers = Integer.MAX_VALUE;
            public boolean lanBroadcast = true;
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
        public boolean manageProfileImage = true;
        public boolean manageNickname = true;
        public boolean manageDescription = true;
        public boolean showNonWhitelistLoginIP = true;
        public boolean isUpdating = false; // internal use for update command state persistence
        public QueueWarning queueWarning = new QueueWarning();
        public ChatRelay chatRelay = new ChatRelay();
        public ConnectionProxy connectionProxy = new ConnectionProxy();
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
        public int readPool = 0; // no database actually needs a read pool currently
        public QueueWait queueWait = new QueueWait();
        public Connections connections = new Connections();
        public Chats chats = new Chats();
        public Deaths deaths = new Deaths();
        public QueueLength queueLength = new QueueLength();
        public Restarts restarts = new Restarts();
        public PlayerCount playerCount = new PlayerCount();
        public Tablist tablist = new Tablist();
        public Lock lock = new Lock();

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
        public boolean autoUpdate = true;
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
