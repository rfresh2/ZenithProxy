package com.zenith.feature.items;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
public class InventoryActionRequest {
    private final Object owner;
    private final List<ContainerClickAction> actions;
    private final int priority;
    private int actionExecIndex = 0;

    public boolean isCompleted() {
        return actionExecIndex >= actions.size();
    }

    public boolean isExecuting() {
        return actionExecIndex > 0 && !isCompleted();
    }

    public @Nullable ContainerClickAction nextAction() {
        var index = actionExecIndex++;
        if (index >= actions.size()) return null;
        return actions.get(index);
    }
}
