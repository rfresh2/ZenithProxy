package com.zenith.via;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.configuration.AbstractViaConfig;
import com.zenith.Proxy;
import com.zenith.network.server.ServerConnection;
import net.kyori.adventure.text.Component;
import net.raphimc.vialoader.impl.platform.ViaVersionPlatformImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

public class ZenithViaPlatform extends ViaVersionPlatformImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger("ViaVersion");
    public ZenithViaPlatform() {
        super(null);
    }

    @Override
    protected AbstractViaConfig createConfig() {
        var config = new ZenithViaConfig(new File(getDataFolder(), "viaversion.yml"));
        config.reload();
        return config;
    }

    @Override
    public boolean kickPlayer(UUID uuid, String s) {
        // the UUID does not match the logged in player's UUID
        // viaversion sets it to the UUID we sent in the GameProfile packet, which is the proxy's UUID instead of the connecting player's
        // or for spectators, we send the same UUID for each of them. so im not sure if this will work correctly at all
        var connection = getServerConnection(uuid);
        if (connection.isPresent() && !connection.get().isSpectator()) {
            LOGGER.warn("Kicking player {} with reason: {}", uuid, s);
            connection.get().disconnect(s);
            return true;
        } else {
            LOGGER.warn("Kicking player with reason: {}", s);
            return false; // via will still kick them by closing the tcp connection
        }
    }

    @Override
    public void sendMessage(UUID uuid, String msg) {
        var connection = getServerConnection(uuid);
        if (connection.isPresent()) {
            LOGGER.info("Sending message: {} to player: {}", msg, uuid);
            connection.get().send(new ClientboundSystemChatPacket(Component.text(msg), false));
        } else {
            LOGGER.warn("Failed to send message: {}", msg);
        }
    }

    private Optional<ServerConnection> getServerConnection(final UUID viaUuid) {
        if (viaUuid == null) return Optional.empty();
        UserConnection connectedClient = Via.getManager().getConnectionManager().getConnectedClient(viaUuid);
        var channel = connectedClient.getChannel();
        return Proxy.getInstance().getActiveConnections().stream()
            .filter(connection -> connection.getSession().getChannel() == channel)
            .findFirst();
    }
}
