package com.zenith.util;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ForkJoinPool;

import static com.zenith.util.Constants.SERVER_LOG;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Objects.isNull;

public class Queue {
    private static final String apiUrl = "https://2bqueue.info/queue";
    private static final ObjectMapper mapper = new ObjectMapper();

    private static Instant lastRefreshTime = Instant.EPOCH;
    // todo: add config option with minimum
    private static final Duration refreshPeriod = Duration.of(5L, MINUTES);
    private static QueueStatus queueStatus;

    public static QueueStatus getQueueStatus() {
        if (isNull(queueStatus)) {
            updateQueueStatus();
        } else if (lastRefreshTime.isBefore(Instant.now().minus(refreshPeriod))) {
            ForkJoinPool.commonPool().execute(() -> updateQueueStatus());
        }
        return queueStatus;
    }

    private static void updateQueueStatus() {
        lastRefreshTime = Instant.now();
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestProperty("accept", "application/json");
            InputStream responseStream = connection.getInputStream();
            queueStatus = mapper.readValue(responseStream, QueueStatus.class);
        } catch (Exception e) {
            SERVER_LOG.error("Failed updating queue status", e);
            if (isNull(queueStatus)) {
                queueStatus = new QueueStatus(0, 0, 0, 0L, "");
            }
        }
    }


}
