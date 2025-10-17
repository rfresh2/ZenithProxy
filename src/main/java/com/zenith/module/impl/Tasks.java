package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.event.module.TasksTickEvent;
import com.zenith.feature.tasks.Task;
import com.zenith.module.api.Module;
import lombok.Locked;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

@ApiStatus.Experimental
public class Tasks extends Module {

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

    @Locked
    public void addTask(Task task) {
        var existing = CONFIG.client.extra.tasks.tasks.get(task.getId());
        if (existing == task) return;
        if (existing != null) {
            removeTask(task.getId());
        }
        CONFIG.client.extra.tasks.tasks.put(task.getId(), task);
    }

    @Locked
    public void removeTask(String id) {
        var task = CONFIG.client.extra.tasks.tasks.remove(id);
        if (task != null) {
            try {
                task.close();
            } catch (Exception e) {
                error("Error while closing scheduled task {}", task.getId(), e);
            }
        }
    }


    @Locked
    public void clearTasks() {
        for (var it = CONFIG.client.extra.tasks.tasks.entrySet().iterator(); it.hasNext(); ) {
            var task = it.next();
            it.remove();
            try {
                task.getValue().close();
            } catch (Exception e) {
                error("Error while closing scheduled task {}", task.getKey(), e);
            }
        }
    }

    @Locked
    public List<Task> getTasks() {
        return List.copyOf(CONFIG.client.extra.tasks.tasks.values());
    }

    private void onTasksTick(TasksTickEvent event) {
        processTasks();
    }

    @Locked
    private void processTasks() {
        for (var it = CONFIG.client.extra.tasks.tasks.entrySet().iterator(); it.hasNext(); ) {
            final var entry = it.next();
            var task = entry.getValue();
            boolean remove;
            try {
                remove = !task.tick();
            } catch (Exception e) {
                error("Error while executing scheduled task {}", task.getId(), e);
                remove = true;
            }
            if (remove) {
                it.remove();
                try {
                    task.close();
                } catch (Exception e) {
                    error("Error while closing scheduled task {}", task.getId(), e);
                }
            }
        }
    }
}
