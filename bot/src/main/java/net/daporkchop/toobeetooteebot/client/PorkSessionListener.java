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

package net.daporkchop.toobeetooteebot.client;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.data.game.world.WorldType;
import com.github.steveice10.mc.protocol.data.message.TextMessage;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectingEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.PacketSendingEvent;
import com.github.steveice10.packetlib.event.session.PacketSentEvent;
import com.github.steveice10.packetlib.event.session.SessionListener;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import net.daporkchop.toobeetooteebot.Caches;
import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;
import net.daporkchop.toobeetooteebot.gui.GuiBot;
import net.daporkchop.toobeetooteebot.server.PorkClient;
import net.daporkchop.toobeetooteebot.server.PorkServerAdapter;
import net.daporkchop.toobeetooteebot.util.Config;
import net.daporkchop.toobeetooteebot.util.EntityNotFoundException;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;

public class PorkSessionListener implements SessionListener {
    public TooBeeTooTeeBot bot;

    public PorkSessionListener(TooBeeTooTeeBot tooBeeTooTeeBot) {
        bot = tooBeeTooTeeBot;
    }

    @Override
    public void packetSending(PacketSendingEvent packetSendingEvent) {
        //TODO: we might be able to use this for something
    }

    @Override
    public void packetReceived(PacketReceivedEvent packetReceivedEvent) {
        //System.out.println(packetReceivedEvent.getPacket().getClass().getCanonicalName());
        try {
            ListenerRegistry.handlePacket(packetReceivedEvent.getSession(), packetReceivedEvent.getPacket());

            if (Config.doServer) {
                Iterator<PorkClient> iterator = bot.clients.iterator();
                while (iterator.hasNext()) {
                    PorkClient client = iterator.next();
                    if (((MinecraftProtocol) client.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) { //thx 0x kek
                        client.session.send(packetReceivedEvent.getPacket());
                    }
                }
            }
        } catch (EntityNotFoundException e) {
            //xd xd xd
        } catch (ClassCastException e) {
            System.out.println("[Warning] ClassCast exception in entity decoder");
            e.printStackTrace();
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
    }

    @Override
    public void packetSent(PacketSentEvent packetSentEvent) {
    }

    @Override
    public void connected(ConnectedEvent connectedEvent) {
        System.out.println("Connected to " + Config.ip + ":" + Config.port + "!");
        if (Config.doAntiAFK) {
            bot.timer.schedule(new TimerTask() {
                @Override
                public void run() { //antiafk
                    if (Config.doAntiAFK && bot.clients.size() == 0) {
                        //if (bot.r.nextBoolean()) {
                            bot.client.getSession().send(new ClientPlayerSwingArmPacket(Hand.MAIN_HAND));
                        //} else {
                        //    float yaw = -90 + (90 - -90) * bot.r.nextFloat();
                        //    float pitch = -90 + (90 - -90) * bot.r.nextFloat();
                        //    bot.client.getSession().send(new ClientPlayerRotationPacket(true, yaw, pitch));
                        //}
                    }
                }
            }, 20000, 500);
        }

        if (Config.doSpammer) { //TODO: configurable spam messages
            bot.timer.schedule(new TimerTask() { // i actually want this in a seperate thread, no derp
                @Override
                public void run() { //chat
                    bot.sendChat(Config.spamMesages[bot.r.nextInt(Config.spamMesages.length)]);
                }
            }, 30000, Config.spamDelay);
        }

        bot.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (bot.queuedIngameMessages.size() > 0) {
                    bot.client.getSession().send(new ClientChatPacket(bot.queuedIngameMessages.remove(0)));
                }
            }
        }, 30000, 1000);

        if (Config.doServer && bot.server == null) {
            System.out.println("Starting server...");
            Server server = new Server(Config.serverHost, Config.serverPort, MinecraftProtocol.class, new TcpSessionFactory());
            server.setGlobalFlag(MinecraftConstants.AUTH_PROXY_KEY, Proxy.NO_PROXY);
            server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, Config.doServerAuth);
            server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, new ServerInfoBuilder() {
                @Override
                public ServerStatusInfo buildInfo(Session session) {
                    return new ServerStatusInfo(new VersionInfo(MinecraftConstants.GAME_VERSION, MinecraftConstants.PROTOCOL_VERSION), new PlayerInfo(Integer.MAX_VALUE, bot.clients.size() - 1, getOnline()), new TextMessage("\u00A7c" + bot.protocol.getProfile().getName()), Caches.icon);
                }
            });

            server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, new ServerLoginHandler() {
                @Override
                public void loggedIn(Session session) {
                    session.send(new ServerJoinGamePacket(Caches.eid, false, Caches.gameMode, Caches.dimension, Difficulty.NORMAL, Integer.MAX_VALUE, WorldType.DEFAULT, false));
                }
            });

            server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 256);
            server.addListener(new PorkServerAdapter(bot));
            bot.server = server;
        }

        if (GuiBot.INSTANCE != null) {
            GuiBot.INSTANCE.connect_disconnectButton.setEnabled(true);
        }

        if (GuiBot.INSTANCE != null) {
            GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Connected to " + Config.ip + ":" + Config.port + "</html>");
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
    }

    public GameProfile[] getOnline() {
        ArrayList<GameProfile> arr = new ArrayList<>();

        for (PorkClient session : bot.clients) {
            if (((MinecraftProtocol) session.session.getPacketProtocol()).subProtocol == SubProtocol.GAME) {
                arr.add(((MinecraftProtocol) session.session.getPacketProtocol()).profile);
            }
        }

        return arr.toArray(new GameProfile[arr.size()]);
    }

    @Override
    public void disconnecting(DisconnectingEvent disconnectingEvent) {
        System.out.println("Disconnecting... Reason: " + disconnectingEvent.getReason());
        if (bot.websocketServer != null) {
            bot.websocketServer.sendToAll("chat    \u00A7cDisconnected from server! Reason: " + disconnectingEvent.getReason());
        }
        if (Config.doWebsocket) {
            TooBeeTooTeeBot.bot.loginData.setSerializable("registeredPlayers", TooBeeTooTeeBot.bot.namesToRegisteredPlayers);
            TooBeeTooTeeBot.bot.loginData.save();
        }
        if (Config.doStatCollection) {
            TooBeeTooTeeBot.bot.playData.setSerializable("uuidsToPlayData", TooBeeTooTeeBot.bot.uuidsToPlayData);
            TooBeeTooTeeBot.bot.playData.save();
        }
        if (Config.doServer) {
            TooBeeTooTeeBot.bot.server.getSessions().forEach((session) -> {
                session.disconnect("Bot was kicked from server!!!");
            });
        }
        if (GuiBot.INSTANCE != null) {
            GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Disconnectimg from " + Config.ip + ":" + Config.port + "...</html>");
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
    }

    @Override
    public void disconnected(DisconnectedEvent disconnectedEvent) {
        System.out.println("Disconnected.");

        if (GuiBot.INSTANCE != null) {
            if (!GuiBot.INSTANCE.connect_disconnectButton.isEnabled()) {
                GuiBot.INSTANCE.connect_disconnectButton.setEnabled(true);
                return;
            }
        }

        if (GuiBot.INSTANCE != null) {
            GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Disconnected.</html>");
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

        bot.reLaunch();
    }
}
