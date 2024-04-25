package com.zenith.event.module;

import com.github.rfresh2.CancellableEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

@Data
@EqualsAndHashCode(callSuper = true)
public class OutboundChatEvent extends CancellableEvent {
    private final ServerboundChatPacket packet;
}
