package com.zenith.feature.pathfinder;

import com.zenith.Proxy;
import com.zenith.feature.pathfinder.calc.IPath;
import com.zenith.mc.block.BlockPos;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.Particle;
import org.geysermc.mcprotocollib.protocol.data.game.level.particle.ParticleType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelParticlesPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.zenith.Globals.*;

public class IngamePathRenderer {
    private final Timer renderPathTimer = Timers.tickTimer();

    public void onTick() {
        if (BARITONE.getPathingBehavior().isPathing() && CONFIG.client.extra.pathfinder.renderPath) {
            renderPath();
        }
    }

    private void renderPath() {
        if (Proxy.getInstance().getActiveConnections().isEmpty()) return;
        if (renderPathTimer.tick(CONFIG.client.extra.pathfinder.pathRenderIntervalTicks)) {
            try {
                renderPath0();
            } catch (Exception e) {
                PATH_LOG.error("Error rendering path", e);
            }
        }
    }

    private void renderPath0() {
        var pathOptional = BARITONE.getPathingBehavior().getPath();
        if (pathOptional.isEmpty()) return;
        IPath path = pathOptional.get();
        int pathPosition = BARITONE.getPathingBehavior().getCurrent().getPosition();
        List<ClientboundLevelParticlesPacket> packets = CONFIG.client.extra.pathfinder.renderPathDetailed
            ? buildPathPacketsDetailed(path.positions(), pathPosition)
            : buildPathPacketsSimple(path.positions(), pathPosition);

        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var session = connections[i];
            for (int j = 0; j < packets.size(); j++) {
                session.sendAsync(packets.get(j));
            }
        }
    }

    private List<ClientboundLevelParticlesPacket> buildPathPacketsSimple(List<BlockPos> path, int pathPosition) {
        var particle = new Particle(ParticleType.SMALL_FLAME, null);
        return path.stream()
            .skip(pathPosition)
            .map(pos -> new ClientboundLevelParticlesPacket(
                particle, true, true,
                pos.x() + 0.5f, pos.y() + 0.5f, pos.z() + 0.5f,
                0, 0, 0, 0f,
                1
            ))
            .toList();
    }

    private List<ClientboundLevelParticlesPacket> buildPathPacketsDetailed(List<BlockPos> path, int pathPosition) {
        if (path.isEmpty()) return Collections.emptyList();
        if (pathPosition >= path.size()) return Collections.emptyList();
        var middlePosParticle = new Particle(ParticleType.SOUL_FIRE_FLAME, null);
        var lineParticle = new Particle(ParticleType.SMALL_FLAME, null);
        List<ClientboundLevelParticlesPacket> packets = new ArrayList<>(Math.max(1, path.size() - pathPosition));
        BlockPos prevPos = path.get(pathPosition);
        packets.add(new ClientboundLevelParticlesPacket(
            middlePosParticle, true, true,
            prevPos.x() + 0.5f, prevPos.y() + 0.5f, prevPos.z() + 0.5f,
            0, 0, 0, 0f,
            1
        ));
        for (int i = pathPosition+1; i < path.size(); i++) {
            BlockPos blockPos = path.get(i);
            packets.add(new ClientboundLevelParticlesPacket(
                middlePosParticle, true, true,
                blockPos.x() + 0.5f, blockPos.y() + 0.5f, blockPos.z() + 0.5f,
                0, 0, 0, 0f,
                1
            ));
            // create "line" particle every 0.2 between prev and current
            double xDiff = blockPos.x() - prevPos.x();
            double yDiff = blockPos.y() - prevPos.y();
            double zDiff = blockPos.z() - prevPos.z();
            double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
            double xStep = xDiff / distance;
            double yStep = yDiff / distance;
            double zStep = zDiff / distance;
            double x = prevPos.x() + 0.5;
            double y = prevPos.y() + 0.5;
            double z = prevPos.z() + 0.5;
            for (double j = 0; j < distance; j += 0.2) {
                x += xStep * 0.2;
                y += yStep * 0.2;
                z += zStep * 0.2;
                packets.add(new ClientboundLevelParticlesPacket(
                    lineParticle, true, true,
                    x, y, z,
                    0, 0, 0, 0f,
                    1
                ));
            }
            prevPos = blockPos;
        }
        return packets;
    }
}
