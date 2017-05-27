package net.daporkchop.toobeetooteebot.client;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.ServerLoginHandler;
import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.data.game.world.WorldType;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
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
import com.github.steveice10.mc.protocol.packet.ingame.server.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.*;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;
import net.daporkchop.toobeetooteebot.server.PorkClient;
import net.daporkchop.toobeetooteebot.server.PorkServerAdapter;
import net.daporkchop.toobeetooteebot.util.ChunkPos;
import net.daporkchop.toobeetooteebot.util.Config;
import net.daporkchop.toobeetooteebot.util.TextFormat;
import net.daporkchop.toobeetooteebot.web.PlayData;
import net.daporkchop.toobeetooteebot.web.TabListPlayer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

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
            BREAK:
            if (true) {
                if (packetReceivedEvent.getPacket() instanceof ServerChatPacket) {
                    ServerChatPacket pck = (ServerChatPacket) packetReceivedEvent.getPacket();
                    String messageJson = pck.getMessage().toJsonString();
                    String legacyColorCodes = BaseComponent.toLegacyText(ComponentSerializer.parse(messageJson));
                    String msg = TextFormat.clean(legacyColorCodes);
                    if (Config.processChat) {
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
                    }

                    if (msg.startsWith("!")) { //command from connected user
                        if (msg.startsWith("!toggleafk")) { //useful when manually moving bot around
                            bot.doAFK = !bot.doAFK;
                            System.out.println("! Toggled AntiAFK! Current state: " + (bot.doAFK ? "on" : "off"));
                            bot.queueMessage("! Toggled AntiAFK! Current state: " + (bot.doAFK ? "on" : "off"));
                        }
                        return;
                    }
                    System.out.println("[CHAT] " + msg);
                    if (Config.doDiscord) {
                        bot.queuedMessages.add(msg);
                    }
                    if (bot.websocketServer != null) {
                        bot.websocketServer.sendToAll("chat    " + legacyColorCodes.replace("<", "&lt;").replace(">", "&gt;"));
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerHealthPacket) {
                    ServerPlayerHealthPacket pck = (ServerPlayerHealthPacket) packetReceivedEvent.getPacket();
                    if (Config.doAutoRespawn) {
                        if (pck.getHealth() < 1) {
                            bot.timer.schedule(new TimerTask() { // respawn
                                @Override
                                public void run() {
                                    bot.client.getSession().send(new ClientRequestPacket(ClientRequest.RESPAWN));
                                    bot.cachedChunks.clear(); //memory leak
                                }
                            }, 100);
                        }
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
                                if (bot.websocketServer != null) {
                                    bot.websocketServer.sendToAll("tabAdd  " + player.name + " " + player.ping);
                                }
                                if (Config.doStatCollection) {
                                    if (bot.uuidsToPlayData.containsKey(player.uuid)) {
                                        PlayData data = bot.uuidsToPlayData.get(player.uuid);
                                        data.lastPlayed = System.currentTimeMillis();
                                    } else {
                                        PlayData data = new PlayData(player.uuid, player.name);
                                        bot.uuidsToPlayData.put(data.UUID, data);
                                    }
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
                                        if (bot.websocketServer != null) {
                                            bot.websocketServer.sendToAll("tabPing " + toChange.name + " " + toChange.ping);
                                        }
                                        if (Config.doStatCollection) {
                                            for (PlayData playData : bot.uuidsToPlayData.values()) {
                                                int playTimeDifference = (int) (System.currentTimeMillis() - playData.lastPlayed);
                                                playData.playTimeByHour[0] += playTimeDifference;
                                                playData.playTimeByDay[0] += playTimeDifference;
                                                playData.lastPlayed = System.currentTimeMillis();
                                            }
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
                                        if (bot.websocketServer != null) {
                                            bot.websocketServer.sendToAll("tabDel  " + player.name);
                                        }
                                        if (Config.doStatCollection) {
                                            bot.uuidsToPlayData.get(uuid).lastPlayed = System.currentTimeMillis();
                                        }
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
                    if (Config.doServer) {
                        ServerChunkDataPacket pck = (ServerChunkDataPacket) packetReceivedEvent.getPacket();
                        bot.cachedChunks.put(ChunkPos.getChunkHashFromXZ(pck.getColumn().getX(), pck.getColumn().getZ()), pck.getColumn());
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerUnloadChunkPacket) {
                    if (Config.doServer) {
                        ServerUnloadChunkPacket pck = (ServerUnloadChunkPacket) packetReceivedEvent.getPacket();
                        bot.cachedChunks.remove(ChunkPos.getChunkHashFromXZ(pck.getX(), pck.getZ()));
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerUpdateTimePacket) {
                    if (!bot.isLoggedIn) {
                        System.out.println("Logged in!");
                        bot.isLoggedIn = true;
                        bot.server.bind(true);
                        System.out.println("Started server!");
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerBlockChangePacket) { //update cached chunks
                    if (Config.doServer) {
                        ServerBlockChangePacket pck = (ServerBlockChangePacket) packetReceivedEvent.getPacket();
                        int chunkX = pck.getRecord().getPosition().getX() >> 4;
                        int chunkZ = pck.getRecord().getPosition().getZ() >> 4;
                        int subchunkY = TooBeeTooTeeBot.ensureRange(pck.getRecord().getPosition().getY() >> 4, 0, 15);
                        Column column = bot.cachedChunks.getOrDefault(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), null);
                        if (column == null) {
                            //unloaded or invalid chunk, ignore pls
                            System.out.println("null chunk, this is probably a server bug");
                            break BREAK;
                        }
                        Chunk subChunk = column.getChunks()[subchunkY];
                        int subchunkRelativeY = Math.abs(pck.getRecord().getPosition().getY() - 16 * subchunkY);
                        try {
                            subChunk.getBlocks().set(Math.abs(Math.abs(pck.getRecord().getPosition().getX()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getX() >> 4)) * 16)), TooBeeTooTeeBot.ensureRange(subchunkRelativeY, 0, 15), Math.abs(Math.abs(pck.getRecord().getPosition().getZ()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getZ() >> 4)) * 16)), pck.getRecord().getBlock());
                            column.getChunks()[subchunkY] = subChunk;
                            bot.cachedChunks.put(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), column);
                        } catch (IndexOutOfBoundsException e) {
                            System.out.println((Math.abs(Math.abs(pck.getRecord().getPosition().getX()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getX() >> 4)) * 16))) + " " + subchunkRelativeY + " " + (Math.abs(Math.abs(pck.getRecord().getPosition().getZ()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getZ() >> 4)) * 16))) + " " + (subchunkRelativeY << 8 | chunkZ << 4 | chunkX));
                        }
                        bot.cachedChunks.put(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), column);
                        //System.out.println("chunk " + chunkX + ":" + subchunkY + ":" + chunkZ + " relative pos " + (Math.abs(Math.abs(pck.getRecord().getPosition().getX()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getX() >> 4)) * 16))) + ":" + TooBeeTooTeeBot.ensureRange(subchunkRelativeY, 0, 15) + "(" + subchunkRelativeY + "):" + (Math.abs(pck.getRecord().getPosition().getZ()) - (Math.abs(chunkZ) * 16)) + " original position " + pck.getRecord().getPosition().toString());
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerMultiBlockChangePacket) { //update cached chunks with passion
                    if (Config.doServer) {
                        ServerMultiBlockChangePacket pck = (ServerMultiBlockChangePacket) packetReceivedEvent.getPacket();
                        int chunkX = pck.getRecords()[0] //there HAS to be at least one element
                                .getPosition().getX() >> 4; //this cuts away the additional relative chunk coordinates
                        int chunkZ = pck.getRecords()[0] //there HAS to be at least one element
                                .getPosition().getZ() >> 4; //this cuts away the additional relative chunk coordinates
                        Column column = bot.cachedChunks.getOrDefault(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), null);
                        if (column == null) {
                            //unloaded or invalid chunk, ignore pls
                            System.out.println("null chunk multi, this is probably a server bug");
                            break BREAK;
                        }
                        for (BlockChangeRecord record : pck.getRecords()) {
                            int relativeChunkX = Math.abs(Math.abs(record.getPosition().getX()) - (Math.abs(Math.abs(record.getPosition().getX() >> 4)) * 16));
                            int relativeChunkZ = Math.abs(Math.abs(record.getPosition().getZ()) - (Math.abs(Math.abs(record.getPosition().getZ() >> 4)) * 16));
                            int subchunkY = TooBeeTooTeeBot.ensureRange(record.getPosition().getY() >> 4, 0, 15);
                            Chunk subChunk = column.getChunks()[subchunkY];
                            int subchunkRelativeY = Math.abs(record.getPosition().getY() - 16 * subchunkY);
                            try {
                                subChunk.getBlocks().set(relativeChunkX, TooBeeTooTeeBot.ensureRange(subchunkRelativeY, 0, 15), relativeChunkZ, record.getBlock());
                                column.getChunks()[subchunkY] = subChunk;
                            } catch (IndexOutOfBoundsException e) {
                                System.out.println(relativeChunkX + " " + subchunkRelativeY + " " + relativeChunkZ + " " + (subchunkRelativeY << 8 | relativeChunkZ << 4 | relativeChunkX));
                            }
                        }
                        bot.cachedChunks.put(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), column);
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerJoinGamePacket) {
                    ServerJoinGamePacket pck = (ServerJoinGamePacket) packetReceivedEvent.getPacket();
                    bot.dimension = pck.getDimension();
                } else if (packetReceivedEvent.getPacket() instanceof ServerRespawnPacket) {
                    ServerRespawnPacket pck = (ServerRespawnPacket) packetReceivedEvent.getPacket();
                    bot.dimension = pck.getDimension();
                }
            }
            if (Config.doServer) {
                Iterator<PorkClient> iterator = bot.clients.iterator();
                while (iterator.hasNext()) {
                    iterator.next().session.send(packetReceivedEvent.getPacket());
                }
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
        System.out.println("Connected to " + Config.ip + ":" + Config.port + "!");
        if (Config.doAntiAFK) {
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
        }

        if (Config.doSpammer) { //TODO: configurable spam messages
            bot.timer.schedule(new TimerTask() { // i actually want this in a seperate thread, no derp
                @Override
                public void run() { //chat
                    bot.sendChat(Config.spamMesages[bot.r.nextInt(Config.spamMesages.length - 1)]);
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

        if (Config.doServer) {
            System.out.println("Starting server...");
            Server server = new Server(Config.serverHost, Config.serverPort, MinecraftProtocol.class, new TcpSessionFactory());
            server.setGlobalFlag(MinecraftConstants.AUTH_PROXY_KEY, Proxy.NO_PROXY);
            server.setGlobalFlag(MinecraftConstants.VERIFY_USERS_KEY, false);
            server.setGlobalFlag(MinecraftConstants.SERVER_INFO_BUILDER_KEY, new ServerInfoBuilder() {
                @Override
                public ServerStatusInfo buildInfo(Session session) {
                    return new ServerStatusInfo(new VersionInfo(MinecraftConstants.GAME_VERSION, MinecraftConstants.PROTOCOL_VERSION), new PlayerInfo(100, bot.clients.size(), new GameProfile[0]), new TextMessage("\u00A7c" + bot.protocol.getProfile().getName()), null);
                }
            });

            server.setGlobalFlag(MinecraftConstants.SERVER_LOGIN_HANDLER_KEY, new ServerLoginHandler() {
                @Override
                public void loggedIn(Session session) {
                    session.send(new ServerJoinGamePacket(0, false, GameMode.SURVIVAL, bot.dimension, Difficulty.NORMAL, 10, WorldType.DEFAULT, false));
                }
            });

            server.setGlobalFlag(MinecraftConstants.SERVER_COMPRESSION_THRESHOLD, 256);
            server.addListener(new PorkServerAdapter(bot));
            bot.server = server;
        }
    }

    @Override
    public void disconnecting(DisconnectingEvent disconnectingEvent) {
        System.out.println("Disconnecting... Reason: " + disconnectingEvent.getReason());
        bot.queuedMessages.add("Disconnecting. Reason: " + disconnectingEvent.getReason());
        if (bot.websocketServer != null) {
            bot.websocketServer.sendToAll("shutdown" + disconnectingEvent.getReason());
        }
        if (Config.doWebsocket) {
            TooBeeTooTeeBot.INSTANCE.loginData.setSerializable("registeredPlayers", TooBeeTooTeeBot.INSTANCE.namesToRegisteredPlayers);
            TooBeeTooTeeBot.INSTANCE.loginData.save();
        }
        if (Config.doStatCollection) {
            TooBeeTooTeeBot.INSTANCE.playData.setSerializable("uuidsToPlayData", TooBeeTooTeeBot.INSTANCE.uuidsToPlayData);
            TooBeeTooTeeBot.INSTANCE.playData.save();
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {

        }
    }

    @Override
    public void disconnected(DisconnectedEvent disconnectedEvent) {
        System.out.println("Disconnected.");
        Runtime.getRuntime().halt(0);
    }
}