package com.zenith.via;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.viaversion.viaversion.configuration.AbstractViaConfig;
import com.zenith.Proxy;
import net.kyori.adventure.text.Component;
import net.raphimc.vialoader.impl.platform.ViaVersionPlatformImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
        LOGGER.warn("Kicking player {} with reason: {}", uuid, s);
        if (uuid == null) return false;
        final AtomicBoolean kicked = new AtomicBoolean(false);
        Proxy.getInstance().getActiveConnections().stream()
            .filter(connection -> uuid.equals(connection.getLoginProfileUUID()))
            .forEach(connection -> {
                connection.disconnect(s);
                kicked.set(true);
            });
        return kicked.get();
    }

    @Override
    public void sendMessage(UUID uuid, String msg) {
        LOGGER.info("Sending message: {} to player: {}", msg, uuid);
        if (uuid != null) {
            Proxy.getInstance().getActiveConnections().stream()
                .filter(connection -> uuid.equals(connection.getLoginProfileUUID()))
                .forEach(connection -> connection.send(new ClientboundSystemChatPacket(Component.text(msg), false)));
        }
    }
}
