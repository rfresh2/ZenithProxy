package com.zenith.network.server.handler.player.postoutgoing;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.platform.ViaCodecHandler;
import com.zenith.Proxy;
import com.zenith.cache.DataCache;
import com.zenith.event.player.PlayerLoginEvent;
import com.zenith.network.codec.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.via.ZenithViaInitializer;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.*;
import static com.zenith.util.ComponentSerializer.minimessage;

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
        Proxy.getInstance().getClient().sendAsync(session.getClientInfoCache().getClientInfoPacket());
        session.setLoggedIn(); // allows server packets to start being sent to player
        EVENT_BUS.postAsync(new PlayerLoginEvent.Post(session));
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
        if (CONFIG.server.welcomeMessages) {
            if (CONFIG.client.extra.chat.hideChat) {
                session.sendAsyncMessage(minimessage("<gray>Chat is currently disabled. To enable chat, type <red>/togglechat"));
            }
            if (CONFIG.client.extra.chat.hideWhispers) {
                session.sendAsyncMessage(minimessage("<gray>Whispers are currently disabled. To enable whispers, type <red>/toggleprivatemsgs"));
            }
            if (CONFIG.client.extra.chat.showConnectionMessages) {
                session.sendAsyncMessage(minimessage("<gray>Connection messages enabled. To disable, type <red>/toggleconnectionmsgs"));
            }
            if (CONFIG.client.extra.chat.hideDeathMessages) {
                session.sendAsyncMessage(minimessage("<gray>Death messages are currently disabled. To enable death messages, type <red>/toggledeathmsgs"));
            }
            session.sendAsyncAlert("<green>Connected to <red>" + CACHE.getProfileCache().getProfile().getName());
            if (CONFIG.inGameCommands.enable && !CONFIG.inGameCommands.slashCommands) {
                session.sendAsyncMessage(minimessage("<green>Command Prefix : \"" + CONFIG.inGameCommands.prefix + "\""));
                session.sendAsyncMessage(minimessage("<red>help <gray>- <dark_gray>List Commands"));
            }
        }
    }

    private void checkDisableServerVia(ServerSession session) {
        if (CONFIG.server.viaversion.enabled && CONFIG.server.viaversion.autoRemoveFromPipeline) {
            var channel = session.getChannel();
            if (session.getProtocolVersion().getVersion() == MinecraftCodec.CODEC.getProtocolVersion()
                && channel.hasAttr(ZenithViaInitializer.VIA_USER)
                && channel.pipeline().get(ViaCodecHandler.NAME) != null
            ) {
                SERVER_LOG.debug("Disabling ViaVersion for player: {}", session.getName());
                try {
                    var viaUser = channel.attr(ZenithViaInitializer.VIA_USER).get();
                    // remove via codec from channel pipeline
                    channel.pipeline().remove(ViaCodecHandler.NAME);
                    // dispose via connection state
                    Via.getManager().getConnectionManager().onDisconnect(viaUser);
                } catch (final Throwable e) {
                    SERVER_LOG.error("Error disabling ViaVersion for player: {}", session.getName(), e);
                }
            }
        }
    }
}
