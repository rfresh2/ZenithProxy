package com.zenith.event.module;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.zenith.event.CancellableEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OutboundChatEvent extends CancellableEvent {
    private final ServerboundChatPacket packet;
}
