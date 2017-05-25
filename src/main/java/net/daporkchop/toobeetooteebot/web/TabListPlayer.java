package net.daporkchop.toobeetooteebot.web;

public class TabListPlayer {
    public String uuid;
    public String name;
    public int ping;

    public TabListPlayer(String uuid, String name, int ping) {
        this.uuid = uuid;
        this.name = name;
        this.ping = ping;
    }
}
