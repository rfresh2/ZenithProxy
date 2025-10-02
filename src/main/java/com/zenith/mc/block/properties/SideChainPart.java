package com.zenith.mc.block.properties;

import com.zenith.mc.block.properties.api.StringRepresentable;

public enum SideChainPart implements StringRepresentable {
    UNCONNECTED("unconnected"),
    RIGHT("right"),
    CENTER("center"),
    LEFT("left");

    private final String name;

    private SideChainPart(final String string2) {
        this.name = string2;
    }

    public String toString() {
        return this.getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public boolean isConnected() {
        return this != UNCONNECTED;
    }

    public boolean isConnectionTowards(SideChainPart sideChainPart) {
        return this == CENTER || this == sideChainPart;
    }

    public boolean isChainEnd() {
        return this != CENTER;
    }

    public SideChainPart whenConnectedToTheRight() {
        return switch (this) {
            case UNCONNECTED, LEFT -> LEFT;
            case RIGHT, CENTER -> CENTER;
        };
    }

    public SideChainPart whenConnectedToTheLeft() {
        return switch (this) {
            case UNCONNECTED, RIGHT -> RIGHT;
            case CENTER, LEFT -> CENTER;
        };
    }

    public SideChainPart whenDisconnectedFromTheRight() {
        return switch (this) {
            case UNCONNECTED, LEFT -> UNCONNECTED;
            case RIGHT, CENTER -> RIGHT;
        };
    }

    public SideChainPart whenDisconnectedFromTheLeft() {
        return switch (this) {
            case UNCONNECTED, RIGHT -> UNCONNECTED;
            case CENTER, LEFT -> LEFT;
        };
    }
}
