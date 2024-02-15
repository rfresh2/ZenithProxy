package com.zenith.event.module;

import com.github.rfresh2.CancellableEvent;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OutboundChatEvent extends CancellableEvent {
    private final ServerboundChatPacket packet;
}
