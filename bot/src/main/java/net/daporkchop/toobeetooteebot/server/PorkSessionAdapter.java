/*
 * Adapted from the Wizardry License
 *
 * Copyright (c) 2016-2018 DaPorkchop_
 *
 * Permission is hereby granted to any persons and/or organizations using this software to copy, modify, merge, publish, and distribute it.
 * Said persons and/or organizations are not allowed to use the software or any derivatives of the work for commercial use or any other means to generate income, nor are they allowed to claim this software as their own.
 *
 * The persons and/or organizations are also disallowed from sub-licensing and/or trademarking this software without explicit permission from DaPorkchop_.
 *
 * Any persons and/or organizations using this software must disclose their source code and have it publicly available, include this license, provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.toobeetooteebot.server;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.data.game.MessageType;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerBossBarPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityAttachPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEffectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityPropertiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntitySetPassengersPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPaintingPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.PacketSentEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import net.daporkchop.toobeetooteebot.Caches;
import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;
import net.daporkchop.toobeetooteebot.entity.PotionEffect;
import net.daporkchop.toobeetooteebot.entity.api.Entity;
import net.daporkchop.toobeetooteebot.entity.api.EntityRotation;
import net.daporkchop.toobeetooteebot.entity.impl.EntityMob;
import net.daporkchop.toobeetooteebot.entity.impl.EntityObject;
import net.daporkchop.toobeetooteebot.entity.impl.EntityPainting;
import net.daporkchop.toobeetooteebot.entity.impl.EntityPlayer;
import net.daporkchop.toobeetooteebot.util.Config;
import net.daporkchop.toobeetooteebot.util.RefStrings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;

public class PorkSessionAdapter extends SessionAdapter {
    public PorkClient client;
    public TooBeeTooTeeBot bot;

    public PorkSessionAdapter(PorkClient client, TooBeeTooTeeBot bot) {
        super();
        this.client = client;
        this.bot = bot;
    }

    @Override
    public void packetReceived(PacketReceivedEvent event) {
        if (event.getPacket() instanceof LoginStartPacket
                && (((MinecraftProtocol) this.client.session.getPacketProtocol()).getSubProtocol() == SubProtocol.LOGIN
                || ((MinecraftProtocol) this.client.session.getPacketProtocol()).getSubProtocol() == SubProtocol.HANDSHAKE)) {
            LoginStartPacket pck = event.getPacket();
            if (Config.doServerWhitelist && !Config.whitelistedNames.contains(pck.getUsername())) {
                event.getSession().send(new ServerDisconnectPacket("\u00a76why tf do you thonk you can just use my bot??? reeeeeeeeee       - DaPorkchop_"));
                event.getSession().disconnect(null);
                try {
                    Files.write(Paths.get("whitelist.txt"), ("\n" + pck.getUsername() + " just tried to connect!!! ip:" + event.getSession().getHost()).getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.client.username = pck.getUsername();
            return;
        }
        if (this.client.loggedIn && ((MinecraftProtocol) this.client.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) {
            if (event.getPacket() instanceof ClientKeepAlivePacket || event.getPacket() instanceof ClientTeleportConfirmPacket) {
                return;
            }
            if (this.client.arrayIndex == 0) {
                if (event.getPacket() instanceof ClientPlayerPositionRotationPacket) {
                    ClientPlayerPositionRotationPacket pck = event.getPacket();
                    Caches.x = pck.getX();
                    Caches.y = pck.getY();
                    Caches.z = pck.getZ();
                    Caches.yaw = (float) pck.getYaw();
                    Caches.pitch = (float) pck.getPitch();
                    Caches.updatePlayerLocRot();
                    Caches.onGround = pck.isOnGround();
                    ServerPlayerPositionRotationPacket toSend = new ServerPlayerPositionRotationPacket(Caches.x, Caches.y, Caches.z, Caches.yaw, Caches.pitch, this.bot.r.nextInt(100));
                    for (PorkClient porkClient : this.bot.clients) {
                        if (porkClient.arrayIndex == 0 || ((MinecraftProtocol) porkClient.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) {
                            continue;
                        }
                        porkClient.session.send(toSend);
                    }
                    this.bot.client.getSession().send(pck);
                } else if (event.getPacket() instanceof ClientPlayerPositionPacket) {
                    ClientPlayerPositionPacket pck = event.getPacket();
                    Caches.x = pck.getX();
                    Caches.y = pck.getY();
                    Caches.z = pck.getZ();
                    Caches.updatePlayerLocRot();
                    Caches.onGround = pck.isOnGround();
                    ServerPlayerPositionRotationPacket toSend = new ServerPlayerPositionRotationPacket(Caches.x, Caches.y, Caches.z, Caches.yaw, Caches.pitch, this.bot.r.nextInt(100));
                    for (PorkClient porkClient : this.bot.clients) {
                        if (porkClient.arrayIndex == 0 || ((MinecraftProtocol) porkClient.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) {
                            continue;
                        }
                        porkClient.session.send(toSend);
                    }
                    this.bot.client.getSession().send(pck);
                } else if (event.getPacket() instanceof ClientPlayerRotationPacket) {
                    ClientPlayerRotationPacket pck = event.getPacket();
                    Caches.x = pck.getX();
                    Caches.y = pck.getY();
                    Caches.z = pck.getZ();
                    Caches.yaw = (float) pck.getYaw();
                    Caches.pitch = (float) pck.getPitch();
                    Caches.updatePlayerLocRot();
                    Caches.onGround = pck.isOnGround();
                    ServerPlayerPositionRotationPacket toSend = new ServerPlayerPositionRotationPacket(Caches.x, Caches.y, Caches.z, Caches.yaw, Caches.pitch, this.bot.r.nextInt(100));
                    for (PorkClient porkClient : this.bot.clients) {
                        if (porkClient.arrayIndex == 0 || ((MinecraftProtocol) porkClient.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) {
                            continue;
                        }
                        porkClient.session.send(toSend);
                    }
                    this.bot.client.getSession().send(pck);
                } else if (event.getPacket() instanceof ClientChatPacket) {
                    ClientChatPacket pck = event.getPacket();
                    if (pck.getMessage().startsWith("!")) {
                        if ("!setmainclient".equals(pck.getMessage())) {
                            this.client.session.send(new ServerChatPacket("You're already at index 0!", MessageType.CHAT));
                            return;
                        } else if ("!sendchunks".equals(pck.getMessage()))  {
                            this.sendChunks(event.getSession());
                        } else if (pck.getMessage().startsWith("!!")) {
                            this.bot.client.getSession().send(new ClientChatPacket(pck.message.substring(1)));
                            return;
                        } else if (pck.message.startsWith("!dc")) {
                            this.bot.client.getSession().disconnect("Reboot!");
                            this.bot.server.close();
                            if (pck.message.startsWith("!dchard")) {
                                Runtime.getRuntime().exit(0);
                            }
                            return;
                        }/* else if (pck.message.startsWith("!gm "))   {
                            switch (pck.message.substring(4))   {
                                case "0":
                                case "s":
                                    Caches.gameMode = GameMode.SURVIVAL;
                                    break;
                                case "1":
                                case "c":
                                    Caches.gameMode = GameMode.CREATIVE;
                                    break;
                                case "spectator":
                                    Caches.gameMode = GameMode.SPECTATOR;
                                    break;
                                default:
                                    client.session.send(new ServerChatPacket("Invalid game mode!"));
                                    return;
                            }
                            client.session.send(new ServerChatPacket("Changed game mode to: " + Caches.gameMode.name()));
                            client.session.send(new ServerNotifyClientPacket(ClientNotification.CHANGE_GAMEMODE, Caches.gameMode));
                            return;
                        }*/

                        ServerChatPacket toSend = new ServerChatPacket(pck.getMessage(), MessageType.CHAT);
                        for (PorkClient client : this.bot.clients) {
                            if (((MinecraftProtocol) client.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) {
                                client.session.send(toSend);
                            }
                        }
                        return;
                    } else {
                        this.bot.client.getSession().send(event.getPacket());
                        return;
                    }
                } else {
                    this.bot.client.getSession().send(event.getPacket());
                    return;
                }
            } else if (event.getPacket() instanceof ClientChatPacket) {
                ClientChatPacket pck = event.getPacket();
                if (pck.getMessage().startsWith("!")) {
                    if ("!setmainclient".equals(pck.getMessage())) {
                        if (this.bot.clients.size() < 2) {
                            this.client.session.send(new ServerChatPacket("There's only one client connected!", MessageType.CHAT));
                            return;
                        }

                        Collections.swap(this.bot.clients, 0, this.client.arrayIndex);
                        this.bot.clients.get(this.client.arrayIndex).arrayIndex = this.client.arrayIndex;
                        this.bot.clients.get(0).arrayIndex = 0;
                        this.client.session.send(new ServerChatPacket("Set your position in the array to 0! You now can move yourself!", MessageType.CHAT));
                        return;
                    } else if ("!sendchunks".equals(pck.getMessage()))  {
                        this.sendChunks(event.getSession());
                    } else if (pck.getMessage().startsWith("!!")) {
                        this.client.session.send(new ClientChatPacket(pck.message.substring(1)));
                        return;
                    }

                    ServerChatPacket toSend = new ServerChatPacket(pck.getMessage(), MessageType.CHAT);
                    for (PorkClient client : this.bot.clients) {
                        if (((MinecraftProtocol) client.session.getPacketProtocol()).getSubProtocol() == SubProtocol.GAME) {
                            client.session.send(toSend);
                        }
                    }
                    return;
                } else {
                    this.bot.client.getSession().send(event.getPacket());
                    return;
                }
            } else {
                this.bot.client.getSession().send(event.getPacket());
                return;
            }
        }
    }

    @Override
    public void packetSent(PacketSentEvent event) {
        if (event.getPacket() instanceof LoginSuccessPacket) {
            this.client.loggedIn = true;
            System.out.println("UUID: " + ((LoginSuccessPacket) event.getPacket()).getProfile().getIdAsString() + "\nBot UUID: " + TooBeeTooTeeBot.bot.protocol.getProfile().getIdAsString() + "\nUser name: " + ((LoginSuccessPacket) event.getPacket()).getProfile().getName() + "\nBot name: " + TooBeeTooTeeBot.bot.protocol.getProfile().getName());
        } else if (event.getPacket() instanceof ServerJoinGamePacket) {
            if (!this.client.sentChunks) {
                this.sendChunks(event.getSession());
            }
        }
    }

    public void sendChunks(Session session)    {
        this.client.session.send(new ServerPluginMessagePacket("MC|Brand", RefStrings.BRAND_ENCODED));
        for (Column chunk : Caches.cachedChunks.values()) {
            this.client.session.send(new ServerChunkDataPacket(chunk));
        }
        System.out.println("Sent " + Caches.cachedChunks.size() + " chunks!");
        ServerPlayerListEntryPacket playerListEntryPacket = new ServerPlayerListEntryPacket(PlayerListEntryAction.ADD_PLAYER, TooBeeTooTeeBot.bot.playerListEntries.toArray(new PlayerListEntry[TooBeeTooTeeBot.bot.playerListEntries.size()]));
        this.client.session.send(new ServerPlayerPositionRotationPacket(Caches.x, Caches.y, Caches.z, Caches.yaw, Caches.pitch, this.bot.r.nextInt(1000) + 10));
        this.client.session.send(playerListEntryPacket);
        //client.session.send(new ServerNotifyClientPacket(ClientNotification.CHANGE_GAMEMODE, Caches.gameMode));
        for (Entity entity : Caches.cachedEntities.values()) {
            switch (entity.type) {
                case MOB:
                    EntityMob mob = (EntityMob) entity;
                    session.send(new ServerSpawnMobPacket(entity.entityId,
                            entity.uuid,
                            mob.mobType,
                            entity.x,
                            entity.y,
                            entity.z,
                            mob.yaw,
                            mob.pitch,
                            mob.headYaw,
                            mob.motX,
                            mob.motY,
                            mob.motZ,
                            mob.metadata));
                    for (PotionEffect effect : mob.potionEffects) {
                        session.send(new ServerEntityEffectPacket(entity.entityId,
                                effect.effect,
                                effect.amplifier,
                                effect.duration,
                                effect.ambient,
                                effect.showParticles));
                    }
                    for (Map.Entry<EquipmentSlot, ItemStack> entry : mob.equipment.entrySet()) {
                        session.send(new ServerEntityEquipmentPacket(entity.entityId,
                                entry.getKey(),
                                entry.getValue()));
                    }
                    if (!mob.properties.isEmpty()) {
                        session.send(new ServerEntityPropertiesPacket(entity.entityId,
                                mob.properties));
                    }
                    break;
                case PLAYER:
                    EntityPlayer playerSending = (EntityPlayer) entity;
                    session.send(new ServerSpawnPlayerPacket(entity.entityId,
                            entity.uuid,
                            entity.x,
                            entity.y,
                            entity.z,
                            playerSending.yaw,
                            playerSending.pitch,
                            playerSending.metadata));
                case REAL_PLAYER:
                    EntityPlayer player = (EntityPlayer) entity;
                    for (PotionEffect effect : player.potionEffects) {
                        session.send(new ServerEntityEffectPacket(entity.entityId,
                                effect.effect,
                                effect.amplifier,
                                effect.duration,
                                effect.ambient,
                                effect.showParticles));
                    }
                    for (Map.Entry<EquipmentSlot, ItemStack> entry : player.equipment.entrySet()) {
                        session.send(new ServerEntityEquipmentPacket(entity.entityId,
                                entry.getKey(),
                                entry.getValue()));
                    }
                    if (!player.properties.isEmpty()) {
                        session.send(new ServerEntityPropertiesPacket(entity.entityId,
                                player.properties));
                    }
                    break;
                case OBJECT:
                    EntityObject object = (EntityObject) entity;
                    session.send(new ServerSpawnObjectPacket(entity.entityId,
                            entity.uuid,
                            object.objectType,
                            object.data,
                            entity.x,
                            entity.y,
                            entity.z,
                            object.yaw,
                            object.pitch));
                    break;
                case PAINTING:
                    EntityPainting painting = (EntityPainting) entity;
                    session.send(new ServerSpawnPaintingPacket(entity.entityId,
                            entity.uuid,
                            painting.paintingType,
                            new Position(
                                    (int) entity.x,
                                    (int) entity.y,
                                    (int) entity.z
                            ),
                            painting.direction));
                    break;
            }
        }
        for (Entity entity : Caches.cachedEntities.values()) {
            if (entity.passengerIds.length > 0) {
                session.send(new ServerEntitySetPassengersPacket(entity.entityId,
                        entity.passengerIds));
            }
            if (entity instanceof EntityRotation) {
                EntityRotation rotation = (EntityRotation) entity;
                if (rotation.isLeashed) {
                    session.send(new ServerEntityAttachPacket(entity.entityId,
                            rotation.leashedID));
                }
            }
        }
        System.out.println("Sent " + Caches.cachedEntities.size() + " entities!");
        for (ServerBossBarPacket packet : Caches.cachedBossBars.values()) {
            session.send(packet);
        }
        System.out.println("Sent " + Caches.cachedBossBars.size() + " boss bars!");
        this.client.sentChunks = true;
    }
}
