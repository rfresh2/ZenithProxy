package com.zenith.feature.tasks;

import com.zenith.event.client.ClientConnectEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static com.zenith.Globals.EVENT_BUS;
import static com.zenith.Globals.GSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskSerializerTest {

    @Test
    public void testCommandAction() {
        Action action = new CommandAction("say hello");
        var str = GSON.toJson(action, Action.class);
        var result = GSON.fromJson(str, Action.class);
        assertEquals(action, result);
    }

    @Test
    public void testDurationContinuation() {
        Continuation continuation = new DurationContinuation(1000);
        var str = GSON.toJson(continuation, Continuation.class);
        var result = GSON.fromJson(str, Continuation.class);
        assertEquals(continuation, result);
    }

    @Test
    public void testEventCondition() {
        Condition condition = new EventCondition(ClientConnectEvent.class);
        var str = GSON.toJson(condition, Condition.class);
        var result = GSON.fromJson(str, Condition.class);
        assertTrue(EVENT_BUS.isSubscribed(result));
        var subbedEvents = EVENT_BUS.subscribedEvents(result);
        Assertions.assertEquals(1, subbedEvents.size());
        Assertions.assertEquals(ClientConnectEvent.class, subbedEvents.toArray()[0]);
        assertEquals(condition, result);
    }

    @Test
    public void testForeverContinuation() {
        Continuation continuation = new ForeverContinuation();
        var str = GSON.toJson(continuation, Continuation.class);
        var result = GSON.fromJson(str, Continuation.class);
        assertEquals(continuation, result);
    }

    @Test
    public void testIntervalCondition() {
        Condition condition = new IntervalCondition(Instant.now(), java.time.Duration.ofSeconds(10));
        var str = GSON.toJson(condition, Condition.class);
        var result = GSON.fromJson(str, Condition.class);
        assertEquals(condition, result);
    }

    @Test
    public void testNContinuation() {
        Continuation continuation = new NContinuation(5, 0);
        var str = GSON.toJson(continuation, Continuation.class);
        var result = GSON.fromJson(str, Continuation.class);
        assertEquals(continuation, result);
    }

    @Test
    public void testOnceContinuation() {
        Continuation continuation = new OnceContinuation();
        var str = GSON.toJson(continuation, Continuation.class);
        var result = GSON.fromJson(str, Continuation.class);
        assertEquals(continuation, result);
    }

    @Test
    public void testTask() {
        Task task = new Task(
            "task1",
            new CommandAction("say hello"),
            new IntervalCondition(Instant.now(), Duration.ofMillis(500L)),
            new NContinuation(5)
        );
        var str = GSON.toJson(task, Task.class);
        var result = GSON.fromJson(str, Task.class);
        assertEquals(task, result);
    }
}
