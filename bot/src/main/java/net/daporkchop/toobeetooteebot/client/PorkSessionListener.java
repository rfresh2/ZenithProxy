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
import com.github.steveice10.mc.protocol.data.game.ClientRequest;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.data.game.world.WorldType;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.data.game.world.notify.ClientNotification;
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
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPaintingPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.*;
import com.github.steveice10.mc.protocol.packet.login.server.LoginDisconnectPacket;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import com.google.common.collect.Lists;
import net.daporkchop.toobeetooteebot.Caches;
import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;
import net.daporkchop.toobeetooteebot.entity.EntityType;
import net.daporkchop.toobeetooteebot.entity.PotionEffect;
import net.daporkchop.toobeetooteebot.entity.api.Entity;
import net.daporkchop.toobeetooteebot.entity.api.EntityEquipment;
import net.daporkchop.toobeetooteebot.entity.api.EntityRotation;
import net.daporkchop.toobeetooteebot.entity.impl.EntityMob;
import net.daporkchop.toobeetooteebot.entity.impl.EntityObject;
import net.daporkchop.toobeetooteebot.entity.impl.EntityPainting;
import net.daporkchop.toobeetooteebot.entity.impl.EntityPlayer;
import net.daporkchop.toobeetooteebot.gui.GuiBot;
import net.daporkchop.toobeetooteebot.server.PorkClient;
import net.daporkchop.toobeetooteebot.server.PorkServerAdapter;
import net.daporkchop.toobeetooteebot.util.ChatUtils;
import net.daporkchop.toobeetooteebot.util.ChunkPos;
import net.daporkchop.toobeetooteebot.util.Config;
import net.daporkchop.toobeetooteebot.util.TextFormat;
import net.daporkchop.toobeetooteebot.web.PlayData;

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
    public void packetReceived(PacketReceivedEvent packetReceivedEvent) {
        //System.out.println(packetReceivedEvent.getPacket().getClass().getCanonicalName());
        try {
            BREAK:
            if (true) {
                if (packetReceivedEvent.getPacket() instanceof ServerChatPacket) {
                    ServerChatPacket pck = packetReceivedEvent.getPacket();
                    String messageJson = pck.getMessage().toJsonString();
                    String legacyColorCodes = ChatUtils.getOldText(messageJson);
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
                            }
                        } catch (ArrayIndexOutOfBoundsException e) {
                            //ignore kek
                        }
                    }

                    if (msg.startsWith("!")) { //command from connected user
                        if (msg.startsWith("!toggleafk")) { //useful when manually moving bot around
                            Config.doAntiAFK = !Config.doAntiAFK;
                            System.out.println("! Toggled AntiAFK! Current state: " + (Config.doAntiAFK ? "on" : "off"));
                            bot.queueMessage("! Toggled AntiAFK! Current state: " + (Config.doAntiAFK ? "on" : "off"));
                        } else if (msg.startsWith("!dc")) {
                            bot.server.close();
                            bot.client.getSession().disconnect("Reboot!");
                            if (msg.startsWith("!dchard")) {
                                Runtime.getRuntime().exit(0);
                            }
                        } else if (msg.startsWith("!!")) {
                            bot.client.getSession().send(new ClientChatPacket(msg.replace("!!", "!")));
                        }
                        return;
                    }
                    System.out.println("[CHAT] " + msg);

                    if (GuiBot.INSTANCE != null) {
                        GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>" + msg.replace("<", "&lt;").replace(">", "&gt;") + "</html>");
                        String[] split = GuiBot.INSTANCE.chatDisplay.getText().split("<br>");
                        if (split.length > 500) {
                            String toSet = "<html>";
                            for (int i = 1; i < split.length; i++) {
                                toSet += split[i] + "<br>";
                            }
                            toSet = toSet.substring(toSet.length() - 4) + "</html>";
                            GuiBot.INSTANCE.chatDisplay.setText(toSet);
                        }
                    }
                    if (bot.websocketServer != null && !(msg.contains("whispers") || msg.startsWith("to"))) {
                        bot.websocketServer.sendToAll("chat    " + legacyColorCodes.replace("<", "&lt;").replace(">", "&gt;"));
                    }

                    Iterator<PorkClient> iterator = bot.clients.iterator();
                    while (iterator.hasNext()) {
                        PorkClient client = iterator.next();
                        if (((MinecraftProtocol) client.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) { //thx 0x kek
                            client.session.send(new ServerChatPacket(legacyColorCodes));
                        }
                    }
                    return;
                } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerHealthPacket) {
                    ServerPlayerHealthPacket pck = packetReceivedEvent.getPacket();
                    if (Config.doAutoRespawn) {
                        if (pck.getHealth() < 1) {
                            bot.timer.schedule(new TimerTask() { // respawn
                                @Override
                                public void run() {
                                    bot.client.getSession().send(new ClientRequestPacket(ClientRequest.RESPAWN));
                                    Caches.cachedChunks.clear(); //memory leak
                                }
                            }, 100);
                        }
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerListEntryPacket) {
                    ServerPlayerListEntryPacket pck = packetReceivedEvent.getPacket();
                    switch (pck.getAction()) {
                        case ADD_PLAYER:
                            LOOP:
                            for (PlayerListEntry entry : pck.getEntries()) {
                                for (PlayerListEntry listEntry : bot.playerListEntries) {
                                    if (listEntry.getProfile().getIdAsString().equals(entry.getProfile().getIdAsString())) {
                                        continue LOOP;
                                    }
                                }
                                bot.playerListEntries.add(entry);
                                if (bot.websocketServer != null) {
                                    bot.websocketServer.sendToAll("tabAdd  " + TooBeeTooTeeBot.getName(entry) + " " + entry.getPing() + " " + entry.getProfile().getIdAsString());
                                }
                                if (Config.doStatCollection) {
                                    String uuid = entry.getProfile().getId().toString();
                                    if (bot.uuidsToPlayData.containsKey(uuid)) {
                                        PlayData data = bot.uuidsToPlayData.get(uuid);
                                        data.lastPlayed = System.currentTimeMillis();
                                    } else {
                                        PlayData data = new PlayData(uuid, TooBeeTooTeeBot.getName(entry));
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
                                for (PlayerListEntry toChange : bot.playerListEntries) {
                                    if (uuid.equals(toChange.getProfile().getId().toString())) {
                                        toChange.ping = entry.getPing();
                                        if (bot.websocketServer != null) {
                                            bot.websocketServer.sendToAll("tabPing " + toChange.getDisplayName() + " " + toChange.getPing());
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
                                    PlayerListEntry player = bot.playerListEntries.get(i);
                                    if (uuid.equals(player.getProfile().getId().toString())) {
                                        removalIndex = i;
                                        if (bot.websocketServer != null) {
                                            bot.websocketServer.sendToAll("tabDel  " + TooBeeTooTeeBot.getName(player));
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
                    ServerPlayerListDataPacket pck = packetReceivedEvent.getPacket();
                    bot.tabHeader = pck.getHeader();
                    bot.tabFooter = pck.getFooter();
                    String header = bot.tabHeader.getFullText();
                    String footer = bot.tabFooter.getFullText();
                    if (bot.websocketServer != null) {
                        bot.websocketServer.sendToAll("tabDiff " + header + " " + footer);
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerPlayerPositionRotationPacket) {
                    ServerPlayerPositionRotationPacket pck = packetReceivedEvent.getPacket();
                    Caches.x = pck.getX();
                    Caches.y = pck.getY();
                    Caches.z = pck.getZ();
                    Caches.yaw = pck.getYaw();
                    Caches.pitch = pck.getPitch();
                    bot.client.getSession().send(new ClientTeleportConfirmPacket(pck.getTeleportId()));
                } else if (packetReceivedEvent.getPacket() instanceof ServerChunkDataPacket) {
                    if (Config.doServer) {
                        ServerChunkDataPacket pck = packetReceivedEvent.getPacket();
                        Caches.cachedChunks.put(ChunkPos.getChunkHashFromXZ(pck.getColumn().getX(), pck.getColumn().getZ()), pck.getColumn());
                    }
                    //System.out.println("Received chunk");
                } else if (packetReceivedEvent.getPacket() instanceof ServerUnloadChunkPacket) {
                    if (Config.doServer) {
                        ServerUnloadChunkPacket pck = packetReceivedEvent.getPacket();
                        Caches.cachedChunks.remove(ChunkPos.getChunkHashFromXZ(pck.getX(), pck.getZ()));
                    }
                    //System.out.println("Unloaded chunk");
                } else if (packetReceivedEvent.getPacket() instanceof ServerUpdateTimePacket) {
                    if (!bot.isLoggedIn) {
                        System.out.println("Logged in!");
                        bot.isLoggedIn = true;
                        if (GuiBot.INSTANCE != null) {
                            GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Logged in!</html>");
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
                        if (!bot.server.isListening()) {
                            bot.server.bind(true);
                            System.out.println("Started server!");
                            if (GuiBot.INSTANCE != null) {
                                GuiBot.INSTANCE.chatDisplay.setText(GuiBot.INSTANCE.chatDisplay.getText().substring(0, GuiBot.INSTANCE.chatDisplay.getText().length() - 7) + "<br>Started server!</html>");
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
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerBlockChangePacket) { //update cached chunks
                    if (Config.doServer) {
                        ServerBlockChangePacket pck = packetReceivedEvent.getPacket();
                        int chunkX = pck.getRecord().getPosition().getX() >> 4;
                        int chunkZ = pck.getRecord().getPosition().getZ() >> 4;
                        int subchunkY = TooBeeTooTeeBot.ensureRange(pck.getRecord().getPosition().getY() >> 4, 0, 15);
                        Column column = Caches.cachedChunks.getOrDefault(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), null);
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
                            Caches.cachedChunks.put(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), column);
                        } catch (IndexOutOfBoundsException e) {
                            System.out.println((Math.abs(Math.abs(pck.getRecord().getPosition().getX()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getX() >> 4)) * 16))) + " " + subchunkRelativeY + " " + (Math.abs(Math.abs(pck.getRecord().getPosition().getZ()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getZ() >> 4)) * 16))) + " " + (subchunkRelativeY << 8 | chunkZ << 4 | chunkX));
                        }
                        Caches.cachedChunks.put(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), column);
                        //System.out.println("chunk " + chunkX + ":" + subchunkY + ":" + chunkZ + " relative pos " + (Math.abs(Math.abs(pck.getRecord().getPosition().getX()) - (Math.abs(Math.abs(pck.getRecord().getPosition().getX() >> 4)) * 16))) + ":" + TooBeeTooTeeBot.ensureRange(subchunkRelativeY, 0, 15) + "(" + subchunkRelativeY + "):" + (Math.abs(pck.getRecord().getPosition().getZ()) - (Math.abs(chunkZ) * 16)) + " original position " + pck.getRecord().getPosition().toString());
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerMultiBlockChangePacket) { //update cached chunks with passion
                    if (Config.doServer) {
                        ServerMultiBlockChangePacket pck = packetReceivedEvent.getPacket();
                        int chunkX = pck.getRecords()[0] //there HAS to be at least one element
                                .getPosition().getX() >> 4; //this cuts away the additional relative chunk coordinates
                        int chunkZ = pck.getRecords()[0] //there HAS to be at least one element
                                .getPosition().getZ() >> 4; //this cuts away the additional relative chunk coordinates
                        Column column = Caches.cachedChunks.getOrDefault(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), null);
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
                        Caches.cachedChunks.put(ChunkPos.getChunkHashFromXZ(chunkX, chunkZ), column);
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerJoinGamePacket) {
                    ServerJoinGamePacket pck = packetReceivedEvent.getPacket();
                    Caches.dimension = pck.getDimension();
                    Caches.eid = pck.getEntityId();
                    Caches.gameMode = pck.getGameMode();
                } else if (packetReceivedEvent.getPacket() instanceof ServerNotifyClientPacket) {
                    ServerNotifyClientPacket pck = packetReceivedEvent.getPacket();
                    if (pck.notification == ClientNotification.CHANGE_GAMEMODE) {
                        Caches.gameMode = (GameMode) pck.value;
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerRespawnPacket) {
                    ServerRespawnPacket pck = packetReceivedEvent.getPacket();
                    Caches.dimension = pck.getDimension();
                    Caches.cachedChunks.clear();
                } else if (packetReceivedEvent.getPacket() instanceof LoginDisconnectPacket) {
                    LoginDisconnectPacket pck = packetReceivedEvent.getPacket();
                    System.out.println("Kicked during login! Reason: " + pck.getReason().getFullText());
                    bot.client.getSession().disconnect(pck.getReason().getFullText());
                } else if (packetReceivedEvent.getPacket() instanceof ServerSpawnMobPacket) {
                    ServerSpawnMobPacket pck = packetReceivedEvent.getPacket();
                    EntityMob mob = new EntityMob();
                    mob.type = EntityType.MOB;
                    mob.entityId = pck.entityId;
                    mob.uuid = pck.uuid;
                    mob.mobType = pck.type;
                    mob.x = pck.x;
                    mob.y = pck.y;
                    mob.z = pck.z;
                    mob.pitch = pck.pitch;
                    mob.yaw = pck.yaw;
                    mob.headYaw = pck.headYaw;
                    mob.motX = pck.motX;
                    mob.motY = pck.motY;
                    mob.motZ = pck.motZ;
                    mob.metadata = pck.metadata;
                    Caches.cachedEntities.put(pck.entityId, mob);
                } else if (packetReceivedEvent.getPacket() instanceof ServerSpawnObjectPacket) {
                    ServerSpawnObjectPacket pck = packetReceivedEvent.getPacket();
                    EntityObject mob = new EntityObject();
                    mob.type = EntityType.OBJECT;
                    mob.entityId = pck.entityId;
                    mob.uuid = pck.uuid;
                    mob.objectType = pck.type;
                    mob.x = pck.x;
                    mob.y = pck.y;
                    mob.z = pck.z;
                    mob.pitch = pck.pitch;
                    mob.yaw = pck.yaw;
                    mob.data = pck.data;
                    mob.motX = pck.motX;
                    mob.motY = pck.motY;
                    mob.motZ = pck.motZ;
                    Caches.cachedEntities.put(pck.entityId, mob);
                } else if (packetReceivedEvent.getPacket() instanceof ServerSpawnPaintingPacket) {
                    ServerSpawnPaintingPacket pck = packetReceivedEvent.getPacket();
                    EntityPainting mob = new EntityPainting();
                    mob.type = EntityType.PAINTING;
                    mob.entityId = pck.entityId;
                    mob.uuid = pck.uuid;
                    mob.paintingType = pck.paintingType;
                    mob.x = pck.position.x;
                    mob.y = pck.position.y;
                    mob.z = pck.position.z;
                    mob.direction = pck.direction;
                    Caches.cachedEntities.put(pck.entityId, mob);
                } else if (packetReceivedEvent.getPacket() instanceof ServerSpawnPlayerPacket) {
                    ServerSpawnPlayerPacket pck = packetReceivedEvent.getPacket();
                    EntityPlayer mob = new EntityPlayer();
                    mob.type = EntityType.PLAYER;
                    mob.entityId = pck.entityId;
                    mob.uuid = pck.uuid;
                    mob.x = pck.x;
                    mob.y = pck.y;
                    mob.z = pck.z;
                    mob.pitch = pck.pitch;
                    mob.yaw = pck.yaw;
                    mob.metadata = pck.metadata;
                    Caches.cachedEntities.put(pck.entityId, mob);
                } else if (packetReceivedEvent.getPacket() instanceof ServerEntityDestroyPacket) {
                    ServerEntityDestroyPacket pck = packetReceivedEvent.getPacket();
                    for (int eid : pck.entityIds) {
                        if (Caches.cachedEntities.remove(eid) == null) { //Not needed for vanilla AFAIK, but you never know
                            //I'm not bothering with adding checks on all packets though
                            System.out.println("[Warning] Attempted to remove non-existant entity with ID " + eid);
                        }
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerEntityAttachPacket) {
                    ServerEntityAttachPacket pck = packetReceivedEvent.getPacket();
                    EntityRotation entityRotation = (EntityRotation) Caches.cachedEntities.get(pck.entityId);
                    if (pck.attachedToId == -1) {
                        entityRotation.isLeashed = false;
                    } else {
                        entityRotation.isLeashed = true;
                        entityRotation.leashedID = pck.attachedToId;
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerEntityCollectItemPacket) {
                    ServerEntityCollectItemPacket pck = packetReceivedEvent.getPacket();
                    Caches.cachedEntities.remove(pck.collectedEntityId);
                } else if (packetReceivedEvent.getPacket() instanceof ServerEntityEffectPacket) {
                    ServerEntityEffectPacket pck = packetReceivedEvent.getPacket();
                    PotionEffect effect = new PotionEffect();
                    effect.effect = pck.effect;
                    effect.amplifier = pck.amplifier;
                    effect.duration = pck.duration;
                    effect.ambient = pck.ambient;
                    effect.showParticles = pck.showParticles;
                    ((EntityEquipment) Caches.cachedEntities.get(pck.entityId)).potionEffects.add(effect);
                } else if (packetReceivedEvent.getPacket() instanceof ServerEntityEquipmentPacket) {
                    ServerEntityEquipmentPacket pck = packetReceivedEvent.getPacket();
                    EntityEquipment equipment = (EntityEquipment) Caches.cachedEntities.get(pck.entityId);
                    equipment.equipment.put(pck.slot, pck.item);
                } else if (packetReceivedEvent.getPacket() instanceof ServerEntityHeadLookPacket) {
                    ServerEntityHeadLookPacket pck = packetReceivedEvent.getPacket();
                    EntityRotation rotation = (EntityRotation) Caches.cachedEntities.get(pck.entityId);
                    rotation.headYaw = pck.headYaw;
                } else if (packetReceivedEvent.getPacket() instanceof ServerEntityMetadataPacket) {
                    ServerEntityMetadataPacket pck = packetReceivedEvent.getPacket();
                    Entity entity = Caches.cachedEntities.get(pck.entityId);
                    ArrayList<EntityMetadata> oldMeta = Lists.newArrayList(entity.metadata);
                    ArrayList<EntityMetadata> newMeta = new ArrayList<>();
                    OLDCHECK:
                    for (EntityMetadata oldCheck : oldMeta) { //add old fields and merge
                        for (EntityMetadata newCheck : pck.metadata) {
                            if (newCheck.id == oldCheck.id) {
                                newMeta.add(newCheck);
                                continue OLDCHECK;
                            }
                        }
                        newMeta.add(oldCheck);
                    }
                    NEWCHECK:
                    for (EntityMetadata newCheck : pck.metadata) {
                        for (EntityMetadata oldCheck : oldMeta) {
                            if (oldCheck.id == newCheck.id) {
                                continue NEWCHECK;
                            }
                        }

                        newMeta.add(newCheck);
                    }
                    entity.metadata = newMeta.toArray(new EntityMetadata[newMeta.size()]);
                } else if (packetReceivedEvent.getPacket() instanceof ServerEntityMovementPacket) {
                    ServerEntityMovementPacket pck = packetReceivedEvent.getPacket();
                    Entity entity = Caches.cachedEntities.get(pck.entityId);
                    if (pck.pos) {
                        entity.x += pck.moveX / 4096.0D;
                        entity.y += pck.moveY / 4096.0D;
                        entity.z += pck.moveZ / 4096.0D;
                    }
                    if (pck.rot) {
                        if (entity instanceof EntityRotation) {
                            ((EntityRotation) entity).yaw = pck.yaw;
                            ((EntityRotation) entity).pitch = pck.pitch;
                        }
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerEntityPropertiesPacket) {
                    ServerEntityPropertiesPacket pck = packetReceivedEvent.getPacket();
                    ((EntityEquipment) Caches.cachedEntities.get(pck.entityId)).properties = pck.attributes;
                } else if (packetReceivedEvent.getPacket() instanceof ServerEntityRemoveEffectPacket) {
                    ServerEntityRemoveEffectPacket pck = packetReceivedEvent.getPacket();
                    EntityEquipment equipment = (EntityEquipment) Caches.cachedEntities.get(pck.entityId);
                    for (Iterator<PotionEffect> iterator = equipment.potionEffects.iterator(); iterator.hasNext(); ) {
                        if (iterator.next().effect == pck.effect) {
                            iterator.remove();
                            break;
                        }
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerEntitySetPassengersPacket) {
                    ServerEntitySetPassengersPacket pck = packetReceivedEvent.getPacket();
                    EntityEquipment equipment = (EntityEquipment) Caches.cachedEntities.get(pck.entityId);
                    if (pck.passengerIds == null || pck.passengerIds.length == 0) {
                        equipment.passengerIds = null;
                    } else {
                        equipment.passengerIds = pck.passengerIds;
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerEntityTeleportPacket) {
                    ServerEntityTeleportPacket pck = packetReceivedEvent.getPacket();
                    Entity entity = Caches.cachedEntities.get(pck.entityId);
                    entity.x = pck.x;
                    entity.y = pck.y;
                    entity.z = pck.z;

                    if (entity instanceof EntityRotation) {
                        ((EntityRotation) entity).yaw = pck.yaw;
                        ((EntityRotation) entity).pitch = pck.pitch;
                    }
                } else if (packetReceivedEvent.getPacket() instanceof ServerVehicleMovePacket) {
                    ServerVehicleMovePacket pck = packetReceivedEvent.getPacket();
                    Entity entity = Entity.getEntityBeingRiddenBy(Caches.eid);
                }
            }
            if (Config.doServer) {
                Iterator<PorkClient> iterator = bot.clients.iterator();
                while (iterator.hasNext()) {
                    PorkClient client = iterator.next();
                    if (((MinecraftProtocol) client.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) { //thx 0x kek
                        client.session.send(packetReceivedEvent.getPacket());
                    }
                }
            }
        } catch (ClassCastException e) {
            System.out.println("[Warning] ClassCast exception in entity decoder");
            e.printStackTrace();
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
    }

    @Override
    public void packetSent(PacketSentEvent packetSentEvent) {
        //System.out.println("Sending " + packetSentEvent.getPacket().getClass().getCanonicalName());
    }

    @Override
    public void connected(ConnectedEvent connectedEvent) {
        System.out.println("Connected to " + Config.ip + ":" + Config.port + "!");
        if (Config.doAntiAFK) {
            bot.timer.schedule(new TimerTask() {
                @Override
                public void run() { //antiafk
                    if (Config.doAntiAFK && bot.clients.size() == 0) {
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
            TooBeeTooTeeBot.INSTANCE.loginData.setSerializable("registeredPlayers", TooBeeTooTeeBot.INSTANCE.namesToRegisteredPlayers);
            TooBeeTooTeeBot.INSTANCE.loginData.save();
        }
        if (Config.doStatCollection) {
            TooBeeTooTeeBot.INSTANCE.playData.setSerializable("uuidsToPlayData", TooBeeTooTeeBot.INSTANCE.uuidsToPlayData);
            TooBeeTooTeeBot.INSTANCE.playData.save();
        }
        if (Config.doServer) {
            TooBeeTooTeeBot.INSTANCE.server.getSessions().forEach((session) -> {
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
