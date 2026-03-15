package com.zenith.mc.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.zenith.mc.block.properties.api.StringRepresentable;
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public enum Direction implements StringRepresentable {
    DOWN(Vector3i.from(0, -1, 0), AxisDirection.NEGATIVE, Direction.Axis.Y),
    UP(Vector3i.from(0, 1, 0), AxisDirection.POSITIVE, Direction.Axis.Y),
    NORTH(Vector3i.from(0, 0, -1), AxisDirection.NEGATIVE, Direction.Axis.Z),
    SOUTH(Vector3i.from(0, 0, 1), AxisDirection.POSITIVE, Direction.Axis.Z),
    WEST(Vector3i.from(-1, 0, 0), AxisDirection.NEGATIVE, Direction.Axis.X),
    EAST(Vector3i.from(1, 0, 0), AxisDirection.POSITIVE, Direction.Axis.X);

    @Getter
    private final Vector3i normal;
    @Getter
    private final Direction.Axis axis;
    @Getter
    private final Direction.AxisDirection axisDirection;
    Direction(Vector3i normal, Direction.AxisDirection axisDirection, Direction.Axis axis) {
        this.normal = normal;
        this.axisDirection = axisDirection;
        this.axis = axis;
    }

    public static final List<Direction> HORIZONTALS = ImmutableList.of(
        Direction.NORTH,
        Direction.EAST,
        Direction.SOUTH,
        Direction.WEST
    );

    public int x() {
        return this.normal.getX();
    }

    public int y() {
        return this.normal.getY();
    }

    public int z() {
        return this.normal.getZ();
    }

    public Direction.Plane getPlane() {
        return this.axis.getPlane();
    }

    public Direction invert() {
        return switch (this) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
        };
    }

    public org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction mcpl() {
        return switch (this) {
            case DOWN -> org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction.DOWN;
            case UP -> org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction.UP;
            case NORTH -> org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction.NORTH;
            case SOUTH -> org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction.SOUTH;
            case WEST -> org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction.WEST;
            case EAST -> org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction.EAST;
        };
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }

    public static enum Axis implements StringRepresentable {
        X("x") {
            @Override
            public int choose(int x, int y, int z) {
                return x;
            }

            @Override
            public double choose(double x, double y, double z) {
                return x;
            }
        },
        Y("y") {
            @Override
            public int choose(int x, int y, int z) {
                return y;
            }

            @Override
            public double choose(double x, double y, double z) {
                return y;
            }
        },
        Z("z") {
            @Override
            public int choose(int x, int y, int z) {
                return z;
            }

            @Override
            public double choose(double x, double y, double z) {
                return z;
            }
        };

        public static final Direction.Axis[] VALUES = values();
        private final String name;

        Axis(final String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public boolean isVertical() {
            return this == Y;
        }

        public boolean isHorizontal() {
            return this == X || this == Z;
        }

        public boolean test(Direction direction) {
            return direction != null && direction.getAxis() == this;
        }

        public Direction.Plane getPlane() {
            return switch (this) {
                case X, Z -> Direction.Plane.HORIZONTAL;
                case Y -> Direction.Plane.VERTICAL;
            };
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public abstract int choose(int x, int y, int z);

        public abstract double choose(double x, double y, double z);
    }

    public static enum AxisDirection {
        POSITIVE(1, "Towards positive"),
        NEGATIVE(-1, "Towards negative");

        private final int step;
        private final String name;

        private AxisDirection(final int step, final String name) {
            this.step = step;
            this.name = name;
        }

        /**
         * @return the offset for this AxisDirection. 1 for POSITIVE, -1 for NEGATIVE
         */
        public int getStep() {
            return this.step;
        }

        public String getName() {
            return this.name;
        }

        public String toString() {
            return this.name;
        }

        public Direction.AxisDirection opposite() {
            return this == POSITIVE ? NEGATIVE : POSITIVE;
        }
    }

    public enum Plane implements Iterable<Direction>, Predicate<Direction> {
        HORIZONTAL(new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}, new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}),
        VERTICAL(new Direction[]{Direction.UP, Direction.DOWN}, new Direction.Axis[]{Direction.Axis.Y});

        private final Direction[] faces;
        private final Direction.Axis[] axis;

        Plane(final Direction[] faces, final Direction.Axis[] axis) {
            this.faces = faces;
            this.axis = axis;
        }

        public boolean test(Direction direction) {
            return direction != null && direction.getAxis().getPlane() == this;
        }

        @Override
        public Iterator<Direction> iterator() {
            return Iterators.forArray(this.faces);
        }

        public Stream<Direction> stream() {
            return Arrays.stream(this.faces);
        }

        public int length() {
            return this.faces.length;
        }
    }
}
