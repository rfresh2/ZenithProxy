package com.zenith.via;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.configuration.AbstractViaConfig;
import com.viaversion.viaversion.platform.UserConnectionViaVersionPlatform;
import com.zenith.Proxy;
import com.zenith.network.server.ServerSession;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

import static com.zenith.Globals.VERSION;

public class ZenithViaPlatform extends UserConnectionViaVersionPlatform {
    private static final Logger LOGGER = LoggerFactory.getLogger("ViaVersion");
    public ZenithViaPlatform() {
        super(null);
    }

    @Override
    public String getPlatformName() {
        return "ZenithProxy";
    }

    @Override
    public String getPlatformVersion() {
        return VERSION;
    }

    @Override
    protected AbstractViaConfig createConfig() {
        var config = new ZenithViaConfig(new File(getDataFolder(), "viaversion.yml"));
        config.reload();
        return config;
    }

    @Override
    public java.util.logging.Logger createLogger(final String s) {
        return java.util.logging.Logger.getLogger(s);
    }

    @Override
    public boolean isProxy() {
        return true;
    }

    @Override
    public boolean kickPlayer(UserConnection connection, String s) {
        // the UUID does not match the logged in player's UUID
        // viaversion sets it to the UUID we sent in the GameProfile packet, which is the proxy's UUID instead of the connecting player's
        // or for spectators, we send the same UUID for each of them. so im not sure if this will work correctly at all
        var serverConnection = getServerConnection(connection);
        if (serverConnection.isPresent() && !serverConnection.get().isSpectator()) {
            LOGGER.warn("Kicking player {} with reason: {}", serverConnection.get().getLoginProfileUUID(), s);
            serverConnection.get().disconnect(s);
            return true;
        } else {
            LOGGER.warn("Kicking player with reason: {}", s);
            return false; // via will still kick them by closing the tcp connection
        }
    }

    @Override
    public void sendMessage(UserConnection connection, String msg) {
        var serverConnection = getServerConnection(connection);
        if (serverConnection.isPresent()) {
            LOGGER.info("Sending message: {} to player: {}", msg, serverConnection.get().getLoginProfileUUID());
            serverConnection.get().sendAsyncMessage(Component.text(msg));
        } else {
            LOGGER.warn("Failed to send message: {}", msg);
        }
    }

    @Override
    public void sendCustomPayloadToClient(UserConnection connection, String channel, byte[] message) {
        var serverConnection = getServerConnection(connection);
        if (serverConnection.isPresent()) {
            LOGGER.info("Sending custom payload: {} to player: {}", channel, serverConnection.get().getLoginProfileUUID());
            serverConnection.get().send(new ClientboundCustomPayloadPacket(Key.key(channel), message));
        } else {
            LOGGER.warn("Failed to send player custom payload: {}", channel);
        }
    }

    @Override
    public void sendCustomPayload(UserConnection connection, String channel, byte[] message) {
        var nettyChannel = connection.getChannel();
        var client = Proxy.getInstance().getClient();
        if (client.getChannel() == nettyChannel) {
            LOGGER.info("Sending custom payload: {} to server", channel);
            client.send(new ServerboundCustomPayloadPacket(Key.key(channel), message));
        } else {
            LOGGER.warn("Failed to send server custom payload: {}", channel);
        }

    }

    private Optional<ServerSession> getServerConnection(final UserConnection userConnection) {
        var channel = userConnection.getChannel();
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            if (connection.getChannel() == channel) {
                return Optional.of(connection);
            }
        }
        return Optional.empty();
    }
}
