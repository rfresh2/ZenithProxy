package com.github.steveice10.mc.protocol.packet.login.server;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.util.ReflectionToString;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.packet.Packet;
import net.daporkchop.toobeetooteebot.TooBeeTooTeeBot;

import java.io.IOException;

public class LoginSuccessPacket implements Packet {
    private GameProfile profile;

    private LoginSuccessPacket() {
    }

    public LoginSuccessPacket(GameProfile profile) {
        this.profile = TooBeeTooTeeBot.INSTANCE.protocol.getProfile();
    }

    public GameProfile getProfile() {
        return this.profile;
    }

    public void read(NetInput in) throws IOException {
        this.profile = new GameProfile(in.readString(), in.readString());
    }

    public void write(NetOutput out) throws IOException {
        out.writeString(this.profile.getIdAsString());
        out.writeString(this.profile.getName());
    }

    public boolean isPriority() {
        return true;
    }

    public String toString() {
        return ReflectionToString.toString(this);
    }
}
