package com.zenith.util;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import reactor.netty.http.client.HttpClient;

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
    private static final HttpClient httpClient = HttpClient.create()
            .secure()
            .baseUrl(apiUrl)
            .headers(h -> h.add(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON));
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Duration refreshPeriod = Duration.of(CONFIG.server.queueStatusRefreshMinutes, MINUTES);
    private static QueueStatus queueStatus;
    private static ScheduledExecutorService refreshExecutorService = new ScheduledThreadPoolExecutor(1);

    static {
        refreshExecutorService.scheduleAtFixedRate(
                Queue::updateQueueStatus,
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
            final String response = httpClient
                    .get()
                    .responseContent()
                    .aggregate()
                    .asString()
                    .block();
            queueStatus = mapper.readValue(response, QueueStatus.class);
        } catch (Exception e) {
            SERVER_LOG.error("Failed updating queue status", e);
            if (isNull(queueStatus)) {
                queueStatus = new QueueStatus(0, 0, 0, 0L, "");
            }
        }
    }

    // probably only valid for regular queue, prio seems to move a lot faster
    // returns double representing seconds until estimated queue completion time.
    public static double getQueueWait(final Integer queuePos) {
        return 87.0 * (Math.pow(queuePos.doubleValue(), 0.962));
    }

    public static String getEtaStringFromSeconds(final double totalSeconds) {
        final int hour = (int) (totalSeconds / 3600);
        final int minutes = (int) ((totalSeconds / 60) % 60);
        final int seconds = (int) (totalSeconds % 60);
        final String hourStr = hour >= 10 ? "" + hour : "0" + hour;
        final String minutesStr = minutes >= 10 ? "" + minutes : "0" + minutes;
        final String secondsStr = seconds >= 10 ? "" + seconds : "0" + seconds;
        return hourStr + ":" + minutesStr + ":" + secondsStr;
    }

    public static String getQueueEta(final Integer queuePos) {
        return getEtaStringFromSeconds(getQueueWait(queuePos));
    }
}
