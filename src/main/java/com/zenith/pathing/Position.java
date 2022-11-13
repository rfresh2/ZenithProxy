package com.zenith.pathing;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import lombok.Data;
import net.daporkchop.lib.math.vector.Vec3i;

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

    public Vec3i toDirectionVector() {
        if (x > 0) {
            return Vec3i.of(1, 0, 0);
        } else if (x < 0) {
            return Vec3i.of(-1, 0, 0);
        } else if (y > 0) {
            return Vec3i.of(0, 1, 0);
        } else if (y < 0) {
            return Vec3i.of(0, -1, 0);
        } else if (z > 0) {
            return Vec3i.of(0, 0, 1);
        } else if (z < 0) {
            return Vec3i.of(0, 0, -1);
        } else {
            return Vec3i.of(0, 0, 0);
        }
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

    public Position minus(final Position position) {
        return new Position(this.x - position.getX(), this.y - position.getY(), this.z - position.getZ());
    }
}
