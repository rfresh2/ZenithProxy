package com.zenith.feature.pathing.raycast;

import com.google.common.collect.Iterators;
import com.zenith.cache.data.entity.Entity;
import com.zenith.util.math.MathHelper;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

public enum Direction {
    DOWN(0, 1, -1, "down", Direction.AxisDirection.NEGATIVE, Direction.Axis.Y, Vector3i.from(0, -1, 0)),
    UP(1, 0, -1, "up", Direction.AxisDirection.POSITIVE, Direction.Axis.Y, Vector3i.from(0, 1, 0)),
    NORTH(2, 3, 2, "north", Direction.AxisDirection.NEGATIVE, Direction.Axis.Z, Vector3i.from(0, 0, -1)),
    SOUTH(3, 2, 0, "south", Direction.AxisDirection.POSITIVE, Direction.Axis.Z, Vector3i.from(0, 0, 1)),
    WEST(4, 5, 1, "west", Direction.AxisDirection.NEGATIVE, Direction.Axis.X, Vector3i.from(-1, 0, 0)),
    EAST(5, 4, 3, "east", Direction.AxisDirection.POSITIVE, Direction.Axis.X, Vector3i.from(1, 0, 0));

    private final int data3d;
    private final int oppositeIndex;
    private final int data2d;
    private final String name;
    private final Direction.Axis axis;
    private final Direction.AxisDirection axisDirection;
    private final Vector3i normal;
    private static final Direction[] VALUES = values();
    private static final Direction[] BY_3D_DATA = Arrays.stream(VALUES)
        .sorted(Comparator.comparingInt(direction -> direction.data3d))
        .toArray(i -> new Direction[i]);
    private static final Direction[] BY_2D_DATA = Arrays.stream(VALUES)
        .filter(direction -> direction.getAxis().isHorizontal())
        .sorted(Comparator.comparingInt(direction -> direction.data2d))
        .toArray(i -> new Direction[i]);

    private Direction(int data3d, int oppositeIndex, int data2d, String name, Direction.AxisDirection axisDirection, Direction.Axis axis, Vector3i normal) {
        this.data3d = data3d;
        this.data2d = data2d;
        this.oppositeIndex = oppositeIndex;
        this.name = name;
        this.axis = axis;
        this.axisDirection = axisDirection;
        this.normal = normal;
    }

    public com.github.steveice10.mc.protocol.data.game.entity.object.Direction toMCPL() {
        if (this == DOWN) {
            return com.github.steveice10.mc.protocol.data.game.entity.object.Direction.DOWN;
        } else if (this == UP) {
            return com.github.steveice10.mc.protocol.data.game.entity.object.Direction.UP;
        } else if (this == NORTH) {
            return com.github.steveice10.mc.protocol.data.game.entity.object.Direction.NORTH;
        } else if (this == SOUTH) {
            return com.github.steveice10.mc.protocol.data.game.entity.object.Direction.SOUTH;
        } else if (this == WEST) {
            return com.github.steveice10.mc.protocol.data.game.entity.object.Direction.WEST;
        } else if (this == EAST) {
            return com.github.steveice10.mc.protocol.data.game.entity.object.Direction.EAST;
        } else {
            throw new IllegalStateException("Unable to convert direction to MCPL: " + this);
        }
    }

    public int get3DDataValue() {
        return this.data3d;
    }

    public int get2DDataValue() {
        return this.data2d;
    }

    public Direction.AxisDirection getAxisDirection() {
        return this.axisDirection;
    }

    public static Direction getFacingAxis(Entity entity, Direction.Axis axis) {
        return switch(axis) {
            case X -> EAST.isFacingAngle(entity.getYaw()) ? EAST : WEST;
            case Z -> SOUTH.isFacingAngle(entity.getYaw()) ? SOUTH : NORTH;
            case Y -> entity.getPitch() < 0.0F ? UP : DOWN;
        };
    }

    public Direction getOpposite() {
        return from3DDataValue(this.oppositeIndex);
    }

    public Direction getClockWise(Direction.Axis axis) {
        return switch(axis) {
            case X -> this != WEST && this != EAST ? this.getClockWiseX() : this;
            case Z -> this != NORTH && this != SOUTH ? this.getClockWiseZ() : this;
            case Y -> this != UP && this != DOWN ? this.getClockWise() : this;
        };
    }

    public Direction getCounterClockWise(Direction.Axis axis) {
        return switch(axis) {
            case X -> this != WEST && this != EAST ? this.getCounterClockWiseX() : this;
            case Z -> this != NORTH && this != SOUTH ? this.getCounterClockWiseZ() : this;
            case Y -> this != UP && this != DOWN ? this.getCounterClockWise() : this;
        };
    }

    public Direction getClockWise() {
        return switch(this) {
            case NORTH -> EAST;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            case EAST -> SOUTH;
            default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
        };
    }

    private Direction getClockWiseX() {
        return switch(this) {
            case DOWN -> SOUTH;
            case UP -> NORTH;
            case NORTH -> DOWN;
            case SOUTH -> UP;
            default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
        };
    }

    private Direction getCounterClockWiseX() {
        return switch(this) {
            case DOWN -> NORTH;
            case UP -> SOUTH;
            case NORTH -> UP;
            case SOUTH -> DOWN;
            default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
        };
    }

    private Direction getClockWiseZ() {
        return switch(this) {
            case DOWN -> WEST;
            case UP -> EAST;
            default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
            case WEST -> UP;
            case EAST -> DOWN;
        };
    }

    private Direction getCounterClockWiseZ() {
        return switch(this) {
            case DOWN -> EAST;
            case UP -> WEST;
            default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
            case WEST -> DOWN;
            case EAST -> UP;
        };
    }

    public Direction getCounterClockWise() {
        return switch(this) {
            case NORTH -> WEST;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
            case EAST -> NORTH;
            default -> throw new IllegalStateException("Unable to get CCW facing of " + this);
        };
    }

    public int getStepX() {
        return this.normal.getX();
    }

    public int getStepY() {
        return this.normal.getY();
    }

    public int getStepZ() {
        return this.normal.getZ();
    }

    public Vector3f step() {
        return Vector3f.from((float)this.getStepX(), (float)this.getStepY(), (float)this.getStepZ());
    }

    public String getName() {
        return this.name;
    }

    public Direction.Axis getAxis() {
        return this.axis;
    }

    public static Direction from3DDataValue(int index) {
        return BY_3D_DATA[Math.abs(index % BY_3D_DATA.length)];
    }

    public static Direction from2DDataValue(int horizontalIndex) {
        return BY_2D_DATA[Math.abs(horizontalIndex % BY_2D_DATA.length)];
    }

    @Nullable
    public static Direction fromDelta(int x, int y, int z) {
        if (x == 0) {
            if (y == 0) {
                if (z > 0) {
                    return SOUTH;
                }

                if (z < 0) {
                    return NORTH;
                }
            } else if (z == 0) {
                if (y > 0) {
                    return UP;
                }

                return DOWN;
            }
        } else if (y == 0 && z == 0) {
            if (x > 0) {
                return EAST;
            }

            return WEST;
        }

        return null;
    }

    public static Direction fromYRot(double angle) {
        return from2DDataValue(MathHelper.floorI(angle / 90.0 + 0.5) & 3);
    }

    public static Direction fromAxisAndDirection(Direction.Axis axis, Direction.AxisDirection axisDirection) {
        return switch(axis) {
            case X -> axisDirection == Direction.AxisDirection.POSITIVE ? EAST : WEST;
            case Z -> axisDirection == Direction.AxisDirection.POSITIVE ? SOUTH : NORTH;
            case Y -> axisDirection == Direction.AxisDirection.POSITIVE ? UP : DOWN;
        };
    }

    public float toYRot() {
        return (float)((this.data2d & 3) * 90);
    }

    public static Direction getNearest(double x, double y, double z) {
        return getNearest((float)x, (float)y, (float)z);
    }

    public static Direction getNearest(float x, float y, float z) {
        Direction resultDirection = NORTH;
        float f = Float.MIN_VALUE;

        for(Direction direction : VALUES) {
            float g = x * (float)direction.normal.getX() + y * (float)direction.normal.getY() + z * (float)direction.normal.getZ();
            if (g > f) {
                f = g;
                resultDirection = direction;
            }
        }

        return resultDirection;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static Direction get(Direction.AxisDirection axisDirection, Direction.Axis axis) {
        for(Direction direction : VALUES) {
            if (direction.getAxisDirection() == axisDirection && direction.getAxis() == axis) {
                return direction;
            }
        }

        throw new IllegalArgumentException("No such direction: " + axisDirection + " " + axis);
    }

    public Vector3i getNormal() {
        return this.normal;
    }

    public boolean isFacingAngle(float degrees) {
        float f = degrees * (float) (Math.PI / 180.0);
        float g = (float) -Math.sin(f);
        float h = (float) Math.cos(f);
        return (float)this.normal.getX() * g + (float)this.normal.getZ() * h > 0.0F;
    }

    public static enum Axis implements Predicate<Direction> {
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

        Axis(String name) {
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

        @Override
        public String toString() {
            return this.name;
        }

        public boolean test(@Nullable Direction direction) {
            return direction != null && direction.getAxis() == this;
        }

        public Direction.Plane getPlane() {
            return switch(this) {
                case X, Z -> Direction.Plane.HORIZONTAL;
                case Y -> Direction.Plane.VERTICAL;
            };
        }

        public abstract int choose(int x, int y, int z);

        public abstract double choose(double x, double y, double z);
    }

    public static enum AxisDirection {
        POSITIVE(1, "Towards positive"),
        NEGATIVE(-1, "Towards negative");

        private final int step;
        private final String name;

        private AxisDirection(int step, String name) {
            this.step = step;
            this.name = name;
        }

        public int getStep() {
            return this.step;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public Direction.AxisDirection opposite() {
            return this == POSITIVE ? NEGATIVE : POSITIVE;
        }
    }

    public static enum Plane implements Iterable<Direction>, Predicate<Direction> {
        HORIZONTAL(new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}, new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}),
        VERTICAL(new Direction[]{Direction.UP, Direction.DOWN}, new Direction.Axis[]{Direction.Axis.Y});

        private final Direction[] faces;
        private final Direction.Axis[] axis;

        private Plane(Direction[] faces, Direction.Axis[] axis) {
            this.faces = faces;
            this.axis = axis;
        }

        public boolean test(@Nullable Direction direction) {
            return direction != null && direction.getAxis().getPlane() == this;
        }

        @Override
        public Iterator<Direction> iterator() {
            return Iterators.forArray(this.faces);
        }

        public Stream<Direction> stream() {
            return Arrays.stream(this.faces);
        }
    }
}
