package com.zenith.feature.player;

import com.zenith.util.RequestFuture;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.zenith.Globals.DEFAULT_LOG;

public class InputRequestFuture extends RequestFuture {
    @Getter
    private volatile ClickResult clickResult = ClickResult.None.INSTANCE;
    private volatile List<Consumer<InputRequestFuture>> executedListeners = Collections.emptyList();

    public static final InputRequestFuture rejected = wrap(immediateFuture(false));

    public void setClickResult(ClickResult clickResult) {
        if (isCompleted()) return;
        this.clickResult = clickResult;
    }

    /**
     * Add a callback function to be run if the input was completed, accepted, and executed.
     * Called after the client bot tick event.
     */
    public synchronized void addInputExecutedListener(Consumer<InputRequestFuture> listener) {
        if (isCompleted()) return;
        if (executedListeners.isEmpty()) {
            executedListeners = new ArrayList<>(1);
        }
        executedListeners.add(listener);
    }

    public synchronized void notifyListeners() {
        if (executedListeners.isEmpty()) return;
        executedListeners.forEach(listener -> {
            try {
                listener.accept(this);
            } catch (Exception e) {
                DEFAULT_LOG.error("Error while executing input request future listener", e);
            }
        });
    }

    public static InputRequestFuture wrap(RequestFuture future) {
        var inputRequestFuture = new InputRequestFuture();
        inputRequestFuture.setAccepted(future.isAccepted());
        inputRequestFuture.setCompleted(future.isCompleted());
        return inputRequestFuture;
    }

}
