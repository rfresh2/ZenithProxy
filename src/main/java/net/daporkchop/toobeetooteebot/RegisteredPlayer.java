package net.daporkchop.toobeetooteebot;

import java.io.Serializable;

public class RegisteredPlayer implements Serializable {
    public long lastUsed;
    public String passwordHash;
    public String name;

    public RegisteredPlayer(String passwordHash, String name)    {
        this.passwordHash = passwordHash;
        this.name = name;
    }
}
