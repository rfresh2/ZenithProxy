package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.event.module.TasksTickEvent;
import com.zenith.feature.tasks.Task;
import com.zenith.module.api.Module;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

@ApiStatus.Experimental
public class Tasks extends Module {
    @Getter private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    public Tasks() {
        EXECUTOR.scheduleWithFixedDelay(Tasks::postTick, 50, 50, TimeUnit.MILLISECONDS);
    }

    private static void postTick() {
        EVENT_BUS.post(TasksTickEvent.INSTANCE);
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.tasks.enabled;
    }

    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(TasksTickEvent.class, this::onTasksTick)
        );
    }

    public void addTask(Task task) {
        tasks.put(task.getId(), task);
    }

    public void removeTask(String id) {
        tasks.remove(id);
    }

    private void onTasksTick(TasksTickEvent event) {
        processTasks();
    }

    private void processTasks() {
        for (var it = tasks.entrySet().iterator(); it.hasNext(); ) {
            final var entry = it.next();
            var task = entry.getValue();
            boolean remove;
            try {
                remove = !task.tick();
            } catch (Exception e) {
                error("Error while executing scheduled task {}", task.getId(), e);
                remove = true;
            }
            if (remove)
                it.remove();
        }
    }
}
