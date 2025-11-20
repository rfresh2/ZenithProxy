package com.zenith.feature.player;

import com.zenith.event.client.ClientBotTick;
import org.jspecify.annotations.NullMarked;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

@NullMarked
public class InputManager {
    // after modules and inventory manager, before player simulation
    public static final int TICK_PRIORITY = -10000;
    private static final InputRequest DEFAULT_MOVEMENT_INPUT_REQUEST = new InputRequest(new Object(), null, null, null, Integer.MIN_VALUE);
    private static final InputRequestFuture DEFAULT_REQUEST_FUTURE = new InputRequestFuture();
    private InputRequest currentMovementInputRequest = DEFAULT_MOVEMENT_INPUT_REQUEST;
    private InputRequestFuture currentMovementInputRequestFuture = DEFAULT_REQUEST_FUTURE;

    public InputManager() {
        EVENT_BUS.subscribe(
            this,
            of(ClientBotTick.class, TICK_PRIORITY, this::handleTick)
        );
    }

    /**
     * Requests movement to be executed at the end of the current tick
     */
    public synchronized InputRequestFuture submit(final InputRequest movementInputRequest) {
        if (movementInputRequest.priority() <= currentMovementInputRequest.priority() && hasActiveRequest())
            return InputRequestFuture.rejected;
        currentMovementInputRequestFuture.complete(false);
        currentMovementInputRequest = movementInputRequest;
        currentMovementInputRequestFuture = new InputRequestFuture();
        return currentMovementInputRequestFuture;
    }

    private synchronized void handleTick(final ClientBotTick event) {
        if (currentMovementInputRequest == DEFAULT_MOVEMENT_INPUT_REQUEST) return;
        if (CONFIG.debug.inputManagerDebugLogs) {
            CLIENT_LOG.debug("[Input Manager] Executing movement input: {} requester: {}",
                currentMovementInputRequest.input(),
                currentMovementInputRequest.owner() != null
                    ? currentMovementInputRequest.owner().getClass().getSimpleName()
                    : "Unknown"
            );
        }
        BOT.requestMovement(currentMovementInputRequest, currentMovementInputRequestFuture);
        currentMovementInputRequest = DEFAULT_MOVEMENT_INPUT_REQUEST;
        currentMovementInputRequestFuture.complete(true);
        currentMovementInputRequestFuture = DEFAULT_REQUEST_FUTURE;
    }

    public boolean hasActiveRequest() {
        return currentMovementInputRequest != DEFAULT_MOVEMENT_INPUT_REQUEST;
    }
}
