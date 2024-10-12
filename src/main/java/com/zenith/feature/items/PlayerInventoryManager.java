package com.zenith.feature.items;

import com.github.rfresh2.EventConsumer;
import com.zenith.Proxy;
import com.zenith.event.module.ClientBotTick;
import com.zenith.util.Timer;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;

import java.util.Collections;
import java.util.List;

import static com.zenith.Shared.*;

public class PlayerInventoryManager {
    private static final InventoryActionRequest DEFAULT_ACTION_REQUEST = new InventoryActionRequest(null, Collections.emptyList(), Integer.MIN_VALUE);
    private final Timer tickTimer = Timer.createTickTimer();
    private final int actionDelayTicks = 5;
    private InventoryActionRequest currentActionRequest = DEFAULT_ACTION_REQUEST;

    public PlayerInventoryManager() {
        EVENT_BUS.subscribe(
            this,
            // after modules, before player simulation
            EventConsumer.of(ClientBotTick.class, -5000, this::handleTick),
            EventConsumer.of(ClientBotTick.Starting.class, this::handleBotTickStarting)
        );
    }

    private void handleBotTickStarting(ClientBotTick.Starting event) {
        var openContainerId = CACHE.getPlayerCache().getInventoryCache().getOpenContainerId();
        if (openContainerId == 0) return;
        Proxy.getInstance().getClient().sendAsync(new ServerboundContainerClosePacket(openContainerId));
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

    public synchronized void handleTick(final ClientBotTick event) {
        if (currentActionRequest == DEFAULT_ACTION_REQUEST) return;
        if (CONFIG.debug.ncpStrictInventory) {
            if (CACHE.getPlayerCache().getInventoryCache().getMouseStack() != null) {
                PATHING.stop(Integer.MAX_VALUE);
            }
        }
        if (tickTimer.tick(actionDelayTicks)) {
            var nextAction = currentActionRequest.nextAction();
            if (nextAction != null) {
                var packet = nextAction.toPacket();
                if (packet != null) {
                    Proxy.getInstance().getClient().sendAsync(packet);
                    if (CONFIG.debug.ncpStrictInventory) {
                        if (packet.getCarriedItem() == null)
                            Proxy.getInstance().getClient().sendAsync(new ServerboundContainerClosePacket(0));
                        else PATHING.stop(Integer.MAX_VALUE);
                    }
                }
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
