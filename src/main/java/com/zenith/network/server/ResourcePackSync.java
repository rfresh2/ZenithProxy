package com.zenith.network.server;

import com.zenith.cache.data.config.ResourcePack;
import com.zenith.event.player.PlayerConfigurationEvent;
import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundResourcePackPushPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundResourcePackPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.UUID;

import static com.zenith.Globals.EVENT_BUS;
import static com.zenith.Globals.SERVER_LOG;

public class ResourcePackSync {
    private final ServerSession session;
    private final ArrayDeque<ResourcePack> pending = new ArrayDeque<>();
    private @Nullable UUID currentPackId;
    private boolean started;
    @Getter private boolean complete;

    public ResourcePackSync(ServerSession session) {
        this.session = session;
    }

    public void start(Collection<ResourcePack> packs) {
        if (started) return;
        started = true;
        // Snapshot the cache: other connections and upstream updates must not alter this exchange.
        pending.addAll(packs);
        sendNext();
    }

    public void handleResponse(ServerboundResourcePackPacket packet) {
        if (currentPackId == null || !currentPackId.equals(packet.getId())) {
            SERVER_LOG.warn("Received unexpected resource pack response from {} for pack {}", session, packet.getId());
            return;
        }
        switch (packet.getStatus()) {
            case ACCEPTED, DOWNLOADED -> { }
            case SUCCESSFULLY_LOADED, DECLINED, FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> sendNext();
        }
    }

    private void sendNext() {
        var pack = pending.poll();
        if (pack == null) {
            currentPackId = null;
            complete = true;
            EVENT_BUS.post(new PlayerConfigurationEvent.Exiting(session));
            session.sendAsync(new ClientboundFinishConfigurationPacket());
        } else {
            currentPackId = pack.id();
            session.sendAsync(new ClientboundResourcePackPushPacket(pack.id(), pack.url(), pack.hash(), false, pack.prompt()));
        }
    }
}
