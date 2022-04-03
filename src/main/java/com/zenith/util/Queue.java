package com.zenith.util;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;

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
    private static final LinearInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    // for queue wait time estimation maths
    // shamelessly ripped from 2bored2wait
    private static final double[] QUEUE_PLACEMENT_DATA = ImmutableList.of(93, 207, 231, 257, 412, 418, 486, 506, 550, 586, 666, 758, 789, 826).stream()
            .mapToDouble(i -> i)
            .toArray();
    private static final double[] QUEUE_FACTOR_DATA = ImmutableList.of(
            0.9998618838664679f, 0.9999220416881794f, 0.9999234240704379f,
            0.9999291667668093f, 0.9999410569845172f, 0.9999168965649361f,
            0.9999440195022513f, 0.9999262577896301f, 0.9999462301738332f,
            0.999938895110192f, 0.9999219189483673f, 0.9999473463335498f,
            0.9999337457796981f, 0.9999279556964097f).stream()
            .mapToDouble(i -> i)
            .toArray();
    private static final double CONSTANT_FACTOR = 90;

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

    // probably only valid for regular queue, prio seems to move a lot faster
    // returns double representing seconds until estimated queue completion time.
    public static double getQueueWait(final Integer queueLength, final Integer queuePos) {
        double value = LINEAR_INTERPOLATOR.interpolate(QUEUE_PLACEMENT_DATA, QUEUE_FACTOR_DATA).value(queueLength);
        return Math.log((new Integer(queueLength - queuePos).doubleValue() + CONSTANT_FACTOR)
                            / (queueLength.doubleValue() + CONSTANT_FACTOR))
                /  Math.log(value);
    }


}
