package com.zenith.feature.items;

import com.github.rfresh2.EventConsumer;
import com.github.steveice10.mc.protocol.data.game.inventory.ClickItemAction;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.zenith.Proxy;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.util.Timer;

import java.util.Collections;
import java.util.List;

import static com.zenith.Shared.EVENT_BUS;

public class PlayerInventoryManager {
    private static final InventoryActionRequest DEFAULT_ACTION_REQUEST = new InventoryActionRequest(null, Collections.emptyList(), Integer.MIN_VALUE);
    private final Timer tickTimer = Timer.newTickTimer();
    private final int actionDelayTicks = 2;
    private InventoryActionRequest currentActionRequest = DEFAULT_ACTION_REQUEST;

    public PlayerInventoryManager() {
        EVENT_BUS.subscribe(
            this,
            // after modules, before player simulation
            EventConsumer.of(ClientTickEvent.class, -5000, this::handleTick)
        );
    }

    public synchronized boolean isOwner(Object owner) {
        return owner == currentActionRequest.getOwner();
    }

    public synchronized boolean isCompleted() {
        return currentActionRequest.isCompleted();
    }

    public synchronized boolean isExecuting() {
        return currentActionRequest.isExecuting();
    }

    public synchronized void invActionReq(final Object owner, final List<ContainerClickAction> actions, int priority) {
        if (priority <= currentActionRequest.getPriority()) return;
        if (isExecuting()) return;
        currentActionRequest = new InventoryActionRequest(owner, actions, priority);
    }

    public synchronized void invActionReq(final Object owner, final ContainerClickAction action, int priority) {
        if (priority <= currentActionRequest.getPriority()) return;
        if (isExecuting()) return;
        currentActionRequest = new InventoryActionRequest(owner, List.of(action), priority);
    }

    public synchronized void handleTick(final ClientTickEvent event) {
        if (currentActionRequest == DEFAULT_ACTION_REQUEST) return;
        if (tickTimer.tick(actionDelayTicks)) {
            var nextAction = currentActionRequest.nextAction();
            if (nextAction != null) {
                var packet = nextAction.toPacket();
                if (packet != null) Proxy.getInstance().getClient().sendAsync(packet);
            }
            if (currentActionRequest.isCompleted()) currentActionRequest = DEFAULT_ACTION_REQUEST;
        }
    }

    public List<ContainerClickAction> swapSlots(int fromSlot, int toSlot) {
        return List.of(
            new ContainerClickAction(fromSlot, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK),
            new ContainerClickAction(toSlot, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK),
            new ContainerClickAction(fromSlot, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK)
        );
    }

}
