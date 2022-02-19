package com.zenith.util;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.zenith.util.Constants.CONFIG;
import static com.zenith.util.Constants.SERVER_LOG;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Objects.isNull;

public class Queue {
    private static final String apiUrl = "https://2bqueue.info/queue";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Duration refreshPeriod = Duration.of(CONFIG.server.queueStatusRefreshMinutes, MINUTES);
    private static QueueStatus queueStatus;
    private static ScheduledExecutorService refreshExecutorService = new ScheduledThreadPoolExecutor(1);

    static {
        refreshExecutorService.scheduleAtFixedRate(
                () -> updateQueueStatus(),
                0,
                refreshPeriod.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    public static QueueStatus getQueueStatus() {
        if (isNull(queueStatus)) {
            updateQueueStatus();
        }
        return queueStatus;
    }

    private static void updateQueueStatus() {
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
