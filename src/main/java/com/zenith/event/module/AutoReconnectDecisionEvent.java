package com.zenith.event.module;

import com.github.rfresh2.CancellableEvent;
import com.zenith.event.client.ClientDisconnectEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Fired when a disconnect occurs and AutoReconnect is deciding whether to schedule the reconnect
 *
 * If this event is cancelled, the reconnect will be cancelled and not occur
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AutoReconnectDecisionEvent extends CancellableEvent {
    private final ClientDisconnectEvent disconnectEvent;
}
