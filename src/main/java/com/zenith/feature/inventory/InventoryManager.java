package com.zenith.feature.inventory;

import com.zenith.Proxy;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.inventory.actions.CloseContainer;
import com.zenith.feature.inventory.actions.InventoryAction;
import com.zenith.feature.inventory.actions.SetHeldItem;
import com.zenith.util.RequestFuture;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import org.jspecify.annotations.NullMarked;

import java.util.Collections;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;
import static java.util.Objects.requireNonNullElse;

@NullMarked
public class InventoryManager {
    // after modules and InputManager, before player simulation
    public static final int TICK_PRIORITY = -5000;
    private static final InventoryActionRequest DEFAULT_ACTION_REQUEST = new InventoryActionRequest(new Object(), Collections.emptyList(), Integer.MIN_VALUE, null);
    private static final RequestFuture DEFAULT_REQUEST_FUTURE = new RequestFuture();
    private final Timer tickTimer = Timers.tickTimer();
    private InventoryActionRequest currentActionRequest = DEFAULT_ACTION_REQUEST;
    private RequestFuture currentRequestFuture = DEFAULT_REQUEST_FUTURE;

    public InventoryManager() {
        EVENT_BUS.subscribe(
            this,
            of(ClientBotTick.class, TICK_PRIORITY, this::handleTick)
        );
    }

    static long t = 0;
    /**
     * Requests inventory actions to be executed at the end of the current tick, and subsequent ticks if applicable
     */
    public synchronized RequestFuture submit(final InventoryActionRequest actionRequest) {
        if (actionRequest.getPriority() <= currentActionRequest.getPriority() || hasActiveRequest())
            return RequestFuture.rejected;
        currentRequestFuture.complete(false);
        currentActionRequest = actionRequest;
        currentRequestFuture = new RequestFuture();
        return currentRequestFuture;
    }

    private synchronized void handleTick(final ClientBotTick event) {
        t++;
        if (currentActionRequest == DEFAULT_ACTION_REQUEST) return;
        int delay = requireNonNullElse(
            currentActionRequest.getActionDelayTicks(),
            CONFIG.client.inventory.actionDelayTicks
        );
        if (delay > 0) {
            if (tickTimer.tick(delay)) {
                executeNextAction();
            }
        } else {
            while (currentActionRequest != DEFAULT_ACTION_REQUEST) {
                executeNextAction();
            }
        }
    }

    private void executeNextAction() {
        var action = currentActionRequest.next();
        if (action != null) {
            if (action.containerId() != CACHE.getPlayerCache().getInventoryCache().getOpenContainerId()) {
                CLIENT_LOG.debug("[Inventory Manager] [{}] Skipping action {} requester: {} requested container: {} != {}",
                    t,
                    action,
                    currentActionRequest.getOwnerName(),
                    action.containerId(),
                    CACHE.getPlayerCache().getInventoryCache().getOpenContainerId());
            } else {
                var packet = action.packet();
                if (packet != null) {
                    // todo: setting for toggling inv debug logging
                    CLIENT_LOG.debug("[Inventory Manager] [{}] Executing action: {} requester: {}",
                        t,
                        action,
                        currentActionRequest.getOwnerName());
                    Proxy.getInstance().getClient().sendAwait(packet);
                    InventoryAction actionNextTick = currentActionRequest.peek();
                    if (action instanceof SetHeldItem || actionNextTick instanceof SetHeldItem) {
                        // no delay needed for set carried item
                        tickTimer.skip();
                    }
                }
            }
        } else {
            CLIENT_LOG.debug("[Inventory Manager] [{}] Executing no action, requester: {}", t, currentActionRequest.getOwnerName());
        }
        if (currentActionRequest.isCompleted()) {
            currentRequestFuture.complete(true);
            currentActionRequest = DEFAULT_ACTION_REQUEST;
            currentRequestFuture = DEFAULT_REQUEST_FUTURE;
            if (CONFIG.client.inventory.ncpStrict) {
                var closeActionPacket = new CloseContainer().packet();
                if (closeActionPacket != null) {
                    Proxy.getInstance().getClient().sendAwait(closeActionPacket);
                }
            }
        }
    }

    public int requestedContainerId() {
        var nextAction = currentActionRequest.peek();
        if (nextAction == null) return 0;
        return nextAction.containerId();
    }

    public boolean hasActiveRequest() {
        return currentActionRequest != DEFAULT_ACTION_REQUEST && currentActionRequest.isExecuting();
    }
}
