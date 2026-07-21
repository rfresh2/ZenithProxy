package com.zenith.network.client.handler.incoming;

import com.zenith.event.module.PlayerHealthChangedEvent;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.*;

public class SetHealthHandler implements ClientEventLoopPacketHandler<ClientboundSetHealthPacket, ClientSession> {

    // Last health/food/saturation forwarded to the controlling player.
    // Tracked on the client session's netty thread (where apply() runs), so no synchronization needed.
    private float lastForwardedHealth = Float.NaN;
    private int lastForwardedFood = Integer.MIN_VALUE;
    private float lastForwardedSaturation = Float.NaN;

    /**
     * Some servers (e.g. 2b2t/Folia) send a ClientboundSetHealthPacket every server tick even when
     * health/food/saturation are unchanged. When a client is controlling the proxy, forwarding every
     * one of these is harmless for a vanilla client, but prediction-based anticheats such as GrimAC
     * emit a transaction (ClientboundPing) sandwich for *every* SetHealth they see. At ~20 redundant
     * health packets per second this doubles the transaction rate and delivers them in bursts, which
     * corrupts Grim's movement clock and produces constant Simulation setbacks — automated clients
     * (pathfinder bots) then crawl at half speed or get stuck in a setback loop.
     *
     * We still update the cache and fire events for every packet (via applyAsync on the event loop),
     * but we suppress *forwarding* a health packet to the connected client(s) when it is byte-for-byte
     * identical to the one we last forwarded. A freshly connected controller receives its current
     * health through the login cache sync (EntityPlayer#addPackets), so nothing observable changes for
     * the client; only the redundant per-tick stream is dropped.
     */
    @Override
    public ClientboundSetHealthPacket apply(final ClientboundSetHealthPacket packet, final ClientSession session) {
        if (packet == null) return null;
        // Update cache / fire events for every packet, exactly as before.
        ClientEventLoopPacketHandler.super.apply(packet, session);

        final boolean unchanged = packet.getHealth() == lastForwardedHealth
            && packet.getFood() == lastForwardedFood
            && packet.getSaturation() == lastForwardedSaturation;

        lastForwardedHealth = packet.getHealth();
        lastForwardedFood = packet.getFood();
        lastForwardedSaturation = packet.getSaturation();

        // Returning null drops the packet from the forward-to-client path in ClientSession#callPacketReceived.
        return unchanged ? null : packet;
    }

    @Override
    public boolean applyAsync(@NonNull ClientboundSetHealthPacket packet, @NonNull ClientSession session) {
        var player = CACHE.getPlayerCache().getThePlayer();
        if (packet.getHealth() != player.getHealth()) {
            EVENT_BUS.postAsync(
                new PlayerHealthChangedEvent(packet.getHealth(), player.getHealth()));
        }

        if (packet.getFood() != player.getFood())
            CACHE_LOG.debug("Player food: {}", packet.getFood());
        if (packet.getSaturation() != player.getSaturation())
            CACHE_LOG.debug("Player saturation: {}", packet.getSaturation());
        if (packet.getHealth() != player.getHealth())
            CACHE_LOG.debug("Player health: {}", packet.getHealth());

        player
            .setFood(packet.getFood())
            .setSaturation(packet.getSaturation())
            .setHealth(packet.getHealth());
        return true;
    }
}
