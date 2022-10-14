package com.zenith.pathing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import lombok.Data;

@Data
public class Position {
    private final double x;
    private final double y;
    private final double z;

    public BlockPos toBlockPos() {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public ClientPlayerPositionPacket toPlayerPositionPacket() {
        return new ClientPlayerPositionPacket(false, x, y, z);
    }

    public Position addX(double delta) {
        return new Position(x + delta, y, z);
    }

    public Position addZ(double delta) {
        return new Position(x, y, z + delta);
    }
}
