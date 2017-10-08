package net.daporkchop.toobeetooteebot.text;

public enum ChatType {
    CHAT((byte) 0),
    SYSTEM((byte) 1),
    GAME_INFO((byte) 2);

    private final byte id;

    ChatType(byte id) {
        this.id = id;
    }

    public static ChatType byId(byte idIn) {
        for (ChatType chattype : values()) {
            if (idIn == chattype.id) {
                return chattype;
            }
        }

        return CHAT;
    }

    public byte getId() {
        return this.id;
    }
}