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

package net.daporkchop.toobeetooteebot.util;

import java.util.Arrays;
import java.util.List;

/**
 * @author DaPorkchop_
 */
public final class Config {
    public Authentication authentication = new Authentication();
    public Client client = new Client();
    public Debug debug = new Debug();
    public Gui gui = new Gui();
    public Log log = new Log();
    public Server server = new Server();
    public Websocket websocket = new Websocket();
    private transient boolean donePostLoad = false;

    public synchronized Config doPostLoad() {
        if (donePostLoad) {
            throw new IllegalStateException("Config post-load already done!");
        }
        donePostLoad = true;

        return this;
    }

    public static final class Authentication {
        public boolean doAuthentication = false;
        public String email = "john.doe@example.com";
        public String password = "my_secure_password";
        public String username = "Steve";
    }

    public static final class Client {
        public Extra extra = new Extra();
        public Server server = new Server();

        public static final class Extra {
            public AntiAFK antiafk = new AntiAFK();
            public AutoReconnect autoReconnect = new AutoReconnect();
            public AutoRespawn autoRespawn = new AutoRespawn();
            public Spammer spammer = new Spammer();

            public static final class AntiAFK {
                public Actions actions = new Actions();
                public boolean enabled = true;
                public boolean runEvenIfClientsConnected = false;

                public static final class Actions {
                    public boolean rotate = true;
                    public boolean swingHand = true;
                }
            }

            public static final class AutoReconnect {
                public boolean enabled = true;
                public int delaySecondsOffline = 1;
                public int delaySeconds = 30;

                public int linearIncrease = 4;
            }

            public static final class AutoRespawn {
                public boolean enabled = false;
                public int delayMillis = 100;
            }

            public static final class Spammer {
                public int delaySeconds = 30;
                public boolean enabled = false;
                public List<String> messages = Arrays.asList(
                        "#TeamPepsi",
                        "https://pepsi.team",
                        "https://daporkchop.net"
                );
            }
        }

        public static final class Server {
            public String address = "2b2t.org";
            public int port = 25565;
        }
    }

    public static final class Debug {
        public Packet packet = new Packet();
        public boolean printDataFields = false;
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

    public static final class Gui {
        public boolean enabled = false;
        public int messageCount = 512;
    }

    public static final class Log {
        public boolean printDebug = false;
        public boolean storeDebug = true;
    }

    public static final class Server {
        public Bind bind = new Bind();
        public int compressionThreshold = 256;
        public boolean enabled = false;
        public Extra extra = new Extra();
        public Ping ping = new Ping();
        public boolean verifyUsers = false;
        public boolean kickPrevious = false;

        public static final class Bind {
            public String address = "0.0.0.0";
            public int port = 25565;
        }

        public static final class Extra {
            public Timeout timeout = new Timeout();
            public Whitelist whitelist = new Whitelist();

            public static final class Whitelist {
                public boolean enable = false;
                public List<String> allowedUsers = Arrays.asList(
                        "DaPorkchop_",
                        "Notch"
                );
                public String kickmsg = "get out of here you HECKING scrub";
            }

            public static final class Timeout {
                public boolean enable = true;
                public long millis = 5000L;
                public long interval = 100L;
            }
        }

        public static final class Ping {
            public boolean favicon = true;
            public int maxPlayers = Integer.MAX_VALUE;
            public String motd = "§c%s";
        }
    }

    public static final class Websocket {
        public Bind bind = new Bind();
        public Client client = new Client();
        public boolean enable = false;

        public static final class Bind {
            public String address = "0.0.0.0";
            public int port = 8080;
        }

        public static final class Client {
            public int maxChatCount = 512;
        }
    }
}
