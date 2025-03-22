package com.zenith.network.server.handler.player.postoutgoing;

import com.viaversion.viaversion.api.Via;
import com.zenith.Proxy;
import com.zenith.cache.DataCache;
import com.zenith.event.proxy.ProxyClientLoggedInEvent;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import com.zenith.via.ZenithViaInitializer;
import net.raphimc.vialoader.netty.VLPipeline;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Shared.*;

public class LoginPostHandler implements PostOutgoingPacketHandler<ClientboundLoginPacket, ServerSession> {
    @Override
    public void accept(@NonNull ClientboundLoginPacket packet, @NonNull ServerSession session) {
        if (!session.isWhitelistChecked()) {
            // we shouldn't be able to get to this point without whitelist checking, but just in case
            session.disconnect("Login without whitelist check?");
            return;
        }
        if (session.isLoggedIn())
            return; // servers can send multiple login packets during world or skin switches
        checkDisableServerVia(session);
        session.setLoggedIn(); // allows server packets to start being sent to player
        EVENT_BUS.postAsync(new ProxyClientLoggedInEvent(session));
        DataCache.sendCacheData(CACHE.getAllData(), session);
        session.initializeTeam();
        session.syncTeamMembers();
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            if (connection.equals(session)) continue;
            if (connection.hasCameraTarget()) continue;
            session.send(connection.getEntitySpawnPacket());
            session.send(connection.getEntityMetadataPacket());
        }
        if (CONFIG.client.extra.chat.hideChat) {
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<gray>Chat is currently disabled. To enable chat, type <red>/togglechat"), false));
        }
        if (CONFIG.client.extra.chat.hideWhispers) {
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<gray>Whispers are currently disabled. To enable whispers, type <red>/toggleprivatemsgs"), false));
        }
        if (CONFIG.client.extra.chat.showConnectionMessages) {
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<gray>Connection messages enabled. To disable, type <red>/toggleconnectionmsgs"), false));
        }
        if (CONFIG.client.extra.chat.hideDeathMessages) {
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<gray>Death messages are currently disabled. To enable death messages, type <red>/toggledeathmsgs"), false));
        }
        session.sendAsyncAlert("<green>Connected to <red>" + CACHE.getProfileCache().getProfile().getName());
        if (CONFIG.inGameCommands.enable && !CONFIG.inGameCommands.slashCommands) {
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<green>Command Prefix : \"" + CONFIG.inGameCommands.prefix + "\""), false));
            session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>help <gray>- <dark_gray>List Commands"), false));
        }
    }

    private void checkDisableServerVia(ServerSession session) {
        if (CONFIG.server.viaversion.enabled && CONFIG.server.viaversion.autoRemoveFromPipeline) {
            var channel = session.getChannel();
            if (session.getProtocolVersion().getVersion() == MinecraftCodec.CODEC.getProtocolVersion()
                && channel.hasAttr(ZenithViaInitializer.VIA_USER)
                && channel.pipeline().get(VLPipeline.VIA_CODEC_NAME) != null
            ) {
                SERVER_LOG.debug("Disabling ViaVersion for player: {}", session.getProfileCache().getProfile().getName());
                try {
                    var viaUser = channel.attr(ZenithViaInitializer.VIA_USER).get();
                    // remove via codec from channel pipeline
                    channel.pipeline().remove(VLPipeline.VIA_CODEC_NAME);
                    // dispose via connection state
                    Via.getManager().getConnectionManager().onDisconnect(viaUser);
                } catch (final Throwable e) {
                    SERVER_LOG.error("Error disabling ViaVersion for player: {}", session.getProfileCache().getProfile().getName(), e);
                }
            }
        }
    }
}
