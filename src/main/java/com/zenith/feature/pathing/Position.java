package com.zenith.feature.pathing;

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
        return Vec3i.of((int) Math.signum(x), (int) Math.signum(y), (int) Math.signum(z));
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

    public Position add(final double x, final double y, final double z) {
        return new Position(getX() + x, getY() + y, getZ() + z);
    }

    public Position minus(final Position position) {
        return new Position(this.x - position.getX(), this.y - position.getY(), this.z - position.getZ());
    }
}
