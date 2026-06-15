package com.zenith.database;

import com.zenith.event.db.RedisRestartEvent;
import org.jdbi.v3.core.HandleConsumer;
import org.redisson.api.RReliableTopic;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public abstract class LiveDatabase extends LockingDatabase {

    private RReliableTopic topic;
    private final Object eventListener = new Object();
    private final AtomicBoolean redisRestarted = new AtomicBoolean(false);

    public LiveDatabase(final QueryExecutor queryExecutor, final RedisClient redisClient) {
        super(queryExecutor, redisClient);
    }

    @Override
    public void start() {
        super.start();
        EVENT_BUS.subscribe(eventListener, of(RedisRestartEvent.class, e -> redisRestarted.set(true)));
        topic = buildTopic();
    }

    @Override
    public void stop() {
        super.stop();
        EVENT_BUS.unsubscribe(eventListener);
        topic = null;
    }

    private RReliableTopic buildTopic() {
        return redisClient.getRedissonClient().getReliableTopic(getTopicKey());
    }

    public void insert(final Instant instant, final Object liveFeedRecord, final HandleConsumer<?> query) {
        insert(instant, () -> liveQueueRunnable(liveFeedRecord), query);
    }

    private String getTopicKey() {
        return getLockKey() + "Topic";
    }

    void liveQueueRunnable(Object pojo) {
        try {
            if (topic == null || redisRestarted.getAndSet(false)) {
                topic = buildTopic();
            }
            String json = OBJECT_MAPPER.writeValueAsString(pojo);
            topic.publish(json);
        } catch (final Exception e) {
            DATABASE_LOG.error("Failed to offer record to: {}", getTopicKey(), e);
        }
    }
}
