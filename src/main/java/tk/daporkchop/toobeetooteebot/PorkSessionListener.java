package tk.daporkchop.toobeetooteebot;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.data.game.world.WorldType;
import com.github.steveice10.mc.protocol.data.message.TextMessage;
import com.github.steveice10.mc.protocol.data.status.PlayerInfo;
import com.github.steveice10.mc.protocol.data.status.ServerStatusInfo;
import com.github.steveice10.mc.protocol.data.status.VersionInfo;
import com.github.steveice10.mc.protocol.data.status.handler.ServerInfoBuilder;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUnloadChunkPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import tk.daporkchop.toobeetooteebot.server.PorkClient;
import tk.daporkchop.toobeetooteebot.server.PorkServerAdapter;

import java.net.Proxy;
import java.util.Iterator;
import java.util.TimerTask;

public class PorkSessionListener implements SessionListener {
    public TooBeeTooTeeBot bot;

    public PorkSessionListener(TooBeeTooTeeBot tooBeeTooTeeBot) {
        bot = tooBeeTooTeeBot;
    }

    @Override
    public void packetReceived(PacketReceivedEvent packetReceivedEvent) {
        try {
            if (packetReceivedEvent.getPacket() instanceof ServerChatPacket) {
                ServerChatPacket pck = (ServerChatPacket) packetReceivedEvent.getPacket();
                String messageJson = pck.getMessage().toJsonString();
                String legacyColorCodes = BaseComponent.toLegacyText(ComponentSerializer.parse(messageJson));
                String msg = TextFormat.clean(legacyColorCodes);
                if (msg.startsWith("To ")) {
                    //don't bother processing sent DMs
                    return;
                }
                try {
                    String[] split = msg.split(" ");
                    if (!msg.startsWith("<") && split[1].startsWith("whispers")) {
                        bot.processMsg(split[0], msg.substring(split[0].length() + split[1].length() + 2));
                        return;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    //ignore kek
                }

                if (msg.startsWith("!")) { //command from PorkProxy
                    if (msg.startsWith("!toggleafk")) { //useful when manually moving bot around
                        bot.doAFK = !bot.doAFK;
                        System.out.println("! Toggled AntiAFK! Current state: " + (bot.doAFK ? "on" : "off"));
                        bot.queueMessage("! Toggled AntiAFK! Current state: " + (bot.doAFK ? "on" : "off"));
                    }
                    return;
                } else if (msg.startsWith("To ")) {
                    return;
                }
                System.out.println("[CHAT] " + msg);
                bot.queuedMessages.add(msg);
                if (bot.websocketServer != null)
                    bot.websocketServer.sendToAll("chat    " + legacyColorCodes.replace("<", "&lt;").replace(">", "&gt;"));
            } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerHealthPacket) {
                ServerPlayerHealthPacket pck = (ServerPlayerHealthPacket) packetReceivedEvent.getPacket();
                if (pck.getHealth() < 1) {
                    bot.timer.schedule(new TimerTask() { // respawn
                        @Override
                        public void run() {
                            bot.client.getSession().send(new ClientRequestPacket(ClientRequest.RESPAWN));
                            bot.cachedChunks.clear(); //memory leak
                        }
                    }, 100);
                }
            } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerListEntryPacket) {
                ServerPlayerListEntryPacket pck = (ServerPlayerListEntryPacket) packetReceivedEvent.getPacket();
                switch (pck.getAction()) {
                    case ADD_PLAYER:
                        for (PlayerListEntry entry : pck.getEntries()) {
                            if (entry.getProfile().getName().equals("2pork2bot")) {
                                continue;
                            }
                            TabListPlayer player = new TabListPlayer(entry.getProfile().getId().toString(), entry.getProfile().getName(), entry.getPing());
                            bot.playerListEntries.add(player);
                            if (bot.websocketServer != null)
                                bot.websocketServer.sendToAll("tabAdd  " + player.name + " " + player.ping);
                            if (bot.uuidsToPlayData.containsKey(player.uuid)) {
                                PlayData data = bot.uuidsToPlayData.get(player.uuid);
                                data.lastPlayed = System.currentTimeMillis();
                            } else {
                                PlayData data = new PlayData(player.uuid, player.name);
                                bot.uuidsToPlayData.put(data.UUID, data);
                            }
                        }
                        break;
                    case UPDATE_GAMEMODE:
                        //ignore
                        break;
                    case UPDATE_LATENCY:
                        for (PlayerListEntry entry : pck.getEntries()) {
                            String uuid = entry.getProfile().getId().toString();
                            for (TabListPlayer toChange : bot.playerListEntries) {
                                if (uuid.equals(toChange.uuid)) {
                                    toChange.ping = entry.getPing();
                                    if (bot.websocketServer != null)
                                        bot.websocketServer.sendToAll("tabPing " + toChange.name + " " + toChange.ping);
                                    for (PlayData playData : bot.uuidsToPlayData.values())  {
                                        int playTimeDifference = (int) (System.currentTimeMillis() - playData.lastPlayed);
                                        playData.playTimeByHour[0] += playTimeDifference;
                                        playData.playTimeByDay[0] += playTimeDifference;
                                        playData.lastPlayed = System.currentTimeMillis();
                                    }
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
                            for (int i = 0; i < bot.playerListEntries.size(); i++) {
                                TabListPlayer player = bot.playerListEntries.get(i);
                                if (uuid.equals(player.uuid)) {
                                    removalIndex = i;
                                    if (bot.websocketServer != null)
                                        bot.websocketServer.sendToAll("tabDel  " + player.name);
                                    bot.uuidsToPlayData.get(uuid).lastPlayed = System.currentTimeMillis();
                                    break;
                                }
                            }
                            if (removalIndex != -1) {
                                bot.playerListEntries.remove(removalIndex);
                            }
                        }
                        break;
                }
            } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerListDataPacket) {
                ServerPlayerListDataPacket pck = (ServerPlayerListDataPacket) packetReceivedEvent.getPacket();
                bot.tabHeader = pck.getHeader();
                bot.tabFooter = pck.getFooter();
                String header = bot.tabHeader.getFullText();
                String footer = bot.tabFooter.getFullText();
                if (bot.websocketServer != null) {
                    bot.websocketServer.sendToAll("tabDiff " + header + " " + footer);
                }
            } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerPositionRotationPacket) {
                ServerPlayerPositionRotationPacket pck = (ServerPlayerPositionRotationPacket) packetReceivedEvent.getPacket();
                bot.x = pck.getX();
                bot.y = pck.getY();
                bot.z = pck.getZ();
                bot.yaw = pck.getYaw();
                bot.pitch = pck.getPitch();
                bot.client.getSession().send(new ClientTeleportConfirmPacket(pck.getTeleportId()));
            } else if (packetReceivedEvent.getPacket() instanceof ServerChunkDataPacket) {
                ServerChunkDataPacket pck = (ServerChunkDataPacket) packetReceivedEvent.getPacket();
                bot.cachedChunks.put(ChunkPos.getChunkHashFromXZ(pck.getColumn().getX(), pck.getColumn().getZ()), pck.getColumn());
            } else if (packetReceivedEvent.getPacket() instanceof ServerUnloadChunkPacket) {
                ServerUnloadChunkPacket pck = (ServerUnloadChunkPacket) packetReceivedEvent.getPacket();
                bot.cachedChunks.remove(ChunkPos.getChunkHashFromXZ(pck.getX(), pck.getZ()));
            } else if (packetReceivedEvent.getPacket() instanceof ServerUpdateTimePacket) {
                if (!bot.isLoggedIn) {
                    System.out.println("Logged in!");
                    bot.isLoggedIn = true;
                    bot.server.bind(true);
                    System.out.println("Started server!");
                }
            } else if (packetReceivedEvent.getPacket() instanceof ServerBlockChangePacket) {
                ServerBlockChangePacket pck = (ServerBlockChangePacket) packetReceivedEvent.getPacket();
                Column chunk = bot.cachedChunks.getOrDefault(ChunkPos.getChunkHashFromXZ(pck.getRecord().getPosition().getX() >> 4, pck.getRecord().getPosition().getZ() >> 4), null);
            }
            Iterator<PorkClient> iterator = bot.clients.iterator();
            while (iterator.hasNext()) {
                iterator.next().session.send(packetReceivedEvent.getPacket());
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
    }

    @Override
    public void packetSent(PacketSentEvent packetSentEvent) {

    }

    @Override
    public void connected(ConnectedEvent connectedEvent) {
        System.out.println("Connected to " + bot.ip + ":" + bot.port + "!");
        bot.timer.schedule(new TimerTask() {
            @Override
            public void run() { //antiafk
                if (bot.doAFK && bot.clients.size() == 0) {
                    if (bot.r.nextBoolean()) {
                        bot.client.getSession().send(new ClientPlayerSwingArmPacket(Hand.MAIN_HAND));
                    } else {
                        float yaw = -90 + (90 - -90) * bot.r.nextFloat();
                        float pitch = -90 + (90 - -90) * bot.r.nextFloat();
                        bot.client.getSession().send(new ClientPlayerRotationPacket(true, yaw, pitch));
                    }
                }
            }
        }, 20000, 500);

        bot.timer.schedule(new TimerTask() { // i actually want this in a seperate thread, no derp
            @Override
            public void run() { //chat
                if (bot.r.nextBoolean()) {
                    bot.sendChat("Chat viewing, DM sending and more at http://www.daporkchop.net/pork2b2tbot");
                } else {
                    bot.sendChat("Registered users! Use \"/msg 2pork2bot help\" for info on 2pork2bot!");
                }
            }
        }, 30000, 60000);

        bot.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (bot.queuedIngameMessages.size() > 0) {
                    bot.client.getSession().send(new ClientChatPacket(bot.queuedIngameMessages.remove(0)));
                }
            }
        }, 30000, 950);

        System.out.println("Starting server...");
        Server server = new Server("0.0.0.0", 10293, MinecraftProtocol.class, new TcpSessionFactory());
        server.setGlobalFlag(MinecraftConstants.AUTH_PROXY_KEY, Proxy.NO_PROXY);
        server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, false);
        server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, new ServerInfoBuilder() {
            @Override
            public ServerStatusInfo buildInfo(Session session) {
                return new ServerStatusInfo(new VersionInfo(MinecraftConstants.GAME_VERSION, MinecraftConstants.PROTOCOL_VERSION), new PlayerInfo(100, 0, new GameProfile[0]), new TextMessage("2pork2bot"), null);
            }
        });

        server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, new ServerLoginHandler() {
            @Override
            public void loggedIn(Session session) {
                session.send(new ServerJoinGamePacket(0, false, GameMode.SURVIVAL, 0, Difficulty.NORMAL, 10, WorldType.DEFAULT, false));
            }
        });

        server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 256);
        server.addListener(new PorkServerAdapter(bot));
        bot.server = server;
    }

    @Override
    public void disconnecting(DisconnectingEvent disconnectingEvent) {
        System.out.println("Disconnecting... Reason: " + disconnectingEvent.getReason());
        bot.queuedMessages.add("Disconnecting. Reason: " + disconnectingEvent.getReason());
        if (bot.websocketServer != null)
            bot.websocketServer.sendToAll("shutdown" + disconnectingEvent.getReason());
        TooBeeTooTeeBot.INSTANCE.loginData.setSerializable("registeredPlayers", TooBeeTooTeeBot.INSTANCE.namesToRegisteredPlayers);
        TooBeeTooTeeBot.INSTANCE.loginData.save();
        TooBeeTooTeeBot.INSTANCE.playData.setSerializable("uuidsToPlayData", TooBeeTooTeeBot.INSTANCE.uuidsToPlayData);
        TooBeeTooTeeBot.INSTANCE.playData.save();
        try {
        	Thread.sleep(5000);
        } catch (InterruptedExcpetion e) {
        	
        }
        System.exit(0);
    }

    @Override
    public void disconnected(DisconnectedEvent disconnectedEvent) {
        System.out.println("Disconnected.");
        bot.queuedMessages.add("Disconnecting. Reason: " + disconnectedEvent.getReason());
        if (bot.websocketServer != null)
            bot.websocketServer.sendToAll("shutdown" + disconnectedEvent.getReason());
        TooBeeTooTeeBot.INSTANCE.loginData.setSerializable("registeredPlayers", TooBeeTooTeeBot.INSTANCE.namesToRegisteredPlayers);
        TooBeeTooTeeBot.INSTANCE.loginData.save();
        TooBeeTooTeeBot.INSTANCE.playData.setSerializable("uuidsToPlayData", TooBeeTooTeeBot.INSTANCE.uuidsToPlayData);
        TooBeeTooTeeBot.INSTANCE.playData.save();
        try {
        	Thread.sleep(5000);
        } catch (InterruptedExcpetion e) {
        	
        }
        System.exit(0);
    }
}