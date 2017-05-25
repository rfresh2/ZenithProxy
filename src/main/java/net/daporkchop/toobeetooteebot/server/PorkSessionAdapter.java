package net.daporkchop.toobeetooteebot.server;

import com.github.steveice10.mc.protocol.data.game.MessageType;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.player.PositionElement;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.PacketSentEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;

import java.util.Collections;

public class PorkSessionAdapter extends SessionAdapter {
    public PorkClient client;
    public TooBeeTooTeeBot bot;

    public PorkSessionAdapter(PorkClient client, TooBeeTooTeeBot bot) {
        this.client = client;
        this.bot = bot;
    }

    @Override
    public void packetReceived(PacketReceivedEvent event) {
        if (event.getPacket() instanceof LoginStartPacket) {
            LoginStartPacket pck = (LoginStartPacket) event.getPacket();
            client.username = pck.getUsername();
            return;
        }
        if (client.loggedIn) {
            if (event.getPacket() instanceof ClientKeepAlivePacket || event.getPacket() instanceof ClientTeleportConfirmPacket) {
                return;
            }
            if (client.arrayIndex == 0) {
                if (event.getPacket() instanceof ClientPlayerPositionRotationPacket) {
                    ClientPlayerPositionRotationPacket pck = (ClientPlayerPositionRotationPacket) event.getPacket();
                    bot.x = pck.getX();
                    bot.y = pck.getY();
                    bot.z = pck.getZ();
                    bot.yaw = (float) pck.getYaw();
                    bot.pitch = (float) pck.getPitch();
                    bot.onGround = pck.isOnGround();
                    ServerPlayerPositionRotationPacket toSend = new ServerPlayerPositionRotationPacket(bot.x, bot.y, bot.z, bot.yaw, bot.pitch, bot.r.nextInt(100), new PositionElement[0]);
                    for (PorkClient porkClient : bot.clients) {
                        if (porkClient.arrayIndex == 0) {
                            continue;
                        }
                        porkClient.session.send(toSend);
                    }
                    bot.client.getSession().send(pck);
                } else if (event.getPacket() instanceof ClientPlayerPositionPacket) {
                    ClientPlayerPositionPacket pck = (ClientPlayerPositionPacket) event.getPacket();
                    bot.x = pck.getX();
                    bot.y = pck.getY();
                    bot.z = pck.getZ();
                    bot.onGround = pck.isOnGround();
                    ServerPlayerPositionRotationPacket toSend = new ServerPlayerPositionRotationPacket(bot.x, bot.y, bot.z, bot.yaw, bot.pitch, bot.r.nextInt(100), new PositionElement[0]);
                    for (PorkClient porkClient : bot.clients) {
                        if (porkClient.arrayIndex == 0) {
                            continue;
                        }
                        porkClient.session.send(toSend);
                    }
                    bot.client.getSession().send(pck);
                } else if (event.getPacket() instanceof ClientPlayerRotationPacket) {
                    ClientPlayerRotationPacket pck = (ClientPlayerRotationPacket) event.getPacket();
                    bot.x = pck.getX();
                    bot.y = pck.getY();
                    bot.z = pck.getZ();
                    bot.yaw = (float) pck.getYaw();
                    bot.pitch = (float) pck.getPitch();
                    bot.onGround = pck.isOnGround();
                    ServerPlayerPositionRotationPacket toSend = new ServerPlayerPositionRotationPacket(bot.x, bot.y, bot.z, bot.yaw, bot.pitch, bot.r.nextInt(100), new PositionElement[0]);
                    for (PorkClient porkClient : bot.clients) {
                        if (porkClient.arrayIndex == 0) {
                            continue;
                        }
                        porkClient.session.send(toSend);
                    }
                    bot.client.getSession().send(pck);
                } else if (event.getPacket() instanceof ClientChatPacket) {
                    ClientChatPacket pck = (ClientChatPacket) event.getPacket();
                    if (pck.getMessage().startsWith("!")) {
                        if (pck.getMessage().equals("!setmainclient")) {
                            client.session.send(new ServerChatPacket("You're already at index 0!", MessageType.CHAT));
                            return;
                        }

                        ServerChatPacket toSend = new ServerChatPacket(pck.getMessage(), MessageType.CHAT);
                        for (PorkClient client : bot.clients) {
                            client.session.send(toSend);
                        }
                        return;
                    } else {
                        bot.client.getSession().send(event.getPacket());
                        return;
                    }
                } else {
                    bot.client.getSession().send(event.getPacket());
                    return;
                }
            } else if (event.getPacket() instanceof ClientChatPacket) {
                ClientChatPacket pck = (ClientChatPacket) event.getPacket();
                if (pck.getMessage().startsWith("!")) {
                    if (pck.getMessage().equals("!setmainclient")) {
                        if (bot.clients.size() < 2) {
                            client.session.send(new ServerChatPacket("There's only one client connected!", MessageType.CHAT));
                            return;
                        }

                        Collections.swap(bot.clients, 0, client.arrayIndex);
                        bot.clients.get(client.arrayIndex).arrayIndex = client.arrayIndex;
                        bot.clients.get(0).arrayIndex = 0;
                        client.session.send(new ServerChatPacket("Set your position in the array to 0! You now can move yourself!", MessageType.CHAT));
                        return;
                    }

                    ServerChatPacket toSend = new ServerChatPacket(pck.getMessage(), MessageType.CHAT);
                    for (PorkClient client : bot.clients) {
                        client.session.send(toSend);
                    }
                    return;
                } else {
                    bot.client.getSession().send(event.getPacket());
                    return;
                }
            } else {
                bot.client.getSession().send(event.getPacket());
                return;
            }
        }
    }

    @Override
    public void packetSent(PacketSentEvent event) {
        //System.out.println(event.getPacket().getClass().getCanonicalName());
        if (event.getPacket() instanceof LoginSuccessPacket) {
            client.loggedIn = true;
        } else if (event.getPacket() instanceof ServerJoinGamePacket) {
            if (!client.sentChunks) {
                System.out.println(bot.cachedChunks.values().size());
                for (Column chunk : bot.cachedChunks.values()) {
                    client.session.send(new ServerChunkDataPacket(chunk));
                }
                System.out.println("Sent all cached chunks!");
                client.session.send(new ServerPlayerPositionRotationPacket(bot.x, bot.y, bot.z, bot.yaw, bot.pitch, bot.r.nextInt(1000) + 10));
                client.sentChunks = true;
            }
        } else if (event.getPacket() instanceof ClientTeleportConfirmPacket) {
        }
    }
}
