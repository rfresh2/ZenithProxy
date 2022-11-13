package com.zenith.pathing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import lombok.Data;

@Data
public class Position {
    private final double x;
    private final double y;
    private final double z;

    public static int floor(double value) {
        int i = (int) value;
        return value < (double) i ? i - 1 : i;
    }

    public ClientPlayerPositionPacket toPlayerPositionPacket() {
        return new ClientPlayerPositionPacket(false, x, y, z);
    }

    public BlockPos toBlockPos() {
        return new BlockPos(floor(x), floor(y), floor(z));
    }

    public Position addX(double delta) {
        return new Position(x + delta, y, z);
    }

    public Position addY(double delta) {
        return new Position(x, y + delta, z);
    }

    public Position addZ(double delta) {
        return new Position(x, y, z + delta);
    }
}
