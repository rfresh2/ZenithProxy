package com.zenith.event.proxy;

import com.zenith.util.ComponentSerializer;
import lombok.Data;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Data
public class PrivateMessageSendEvent {
    @Nullable private final UUID senderUUID;
    private final String senderName;
    private final String stringContents;
    @Getter(lazy = true)
    private final Component contents = ComponentSerializer.minedown("&c" + senderName + " > " + stringContents + "&r");

    public PrivateMessageSendEvent(@Nullable UUID senderUUID, String senderName, String stringContents) {
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.stringContents = stringContents;
    }

    public PrivateMessageSendEvent(String senderName, String stringContents) {
        this(null, senderName, stringContents);
    }
}
