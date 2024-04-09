package com.zenith.feature.queue;

import com.zenith.feature.queue.mcping.MCPing;
import com.zenith.feature.queue.mcping.data.FinalResponse;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zenith.Shared.*;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Objects.isNull;

public class Queue {
    private static QueueStatus queueStatus;
    private static final Pattern digitPattern = Pattern.compile("\\d+");
    private static final MCPing mcPing = new MCPing();

    public static void start() {
        EXECUTOR.scheduleAtFixedRate(
            () -> Thread.ofVirtual().name("Queue Update").start(Queue::updateQueueStatus),
            500L,
            Duration.of(CONFIG.server.queueStatusRefreshMinutes, MINUTES).toMillis(),
            TimeUnit.MILLISECONDS);
    }

    public static QueueStatus getQueueStatus() {
        if (isNull(queueStatus)) {
            updateQueueStatus();
        }
        return queueStatus;
    }

    public static void updateQueueStatus() {
        if (!pingUpdate()) {
            if (!apiUpdate()) {
                SERVER_LOG.error("Failed updating queue status. Is the network down?");
                if (isNull(queueStatus)) {
                    queueStatus = new QueueStatus(0, 0, Instant.EPOCH.getEpochSecond());
                }
            }
        }
    }

    // probably only valid for regular queue, prio seems to move a lot faster
    // returns double representing seconds until estimated queue completion time.
    public static long getQueueWait(final Integer queuePos) {
        return (long) (53.8 * (Math.pow(queuePos.doubleValue(), 1.04)));
    }

    public static String getEtaStringFromSeconds(final long totalSeconds) {
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

    public static boolean pingUpdate() {
        try {
            final FinalResponse pingWithDetails = mcPing.ping("connect.2b2t.org", 25565, 3000, false);
            final String queueStr = pingWithDetails.getPlayers().getSample().get(1).getName();
            final Matcher regularQMatcher = digitPattern.matcher(queueStr.substring(queueStr.lastIndexOf(" ")));
            final String prioQueueStr = pingWithDetails.getPlayers().getSample().get(2).getName();
            final Matcher prioQMatcher = digitPattern.matcher(prioQueueStr.substring(prioQueueStr.lastIndexOf(" ")));
            if (!queueStr.contains("Queue")) {
                throw new IOException("Queue string doesn't contain Queue: " + queueStr);
            }
            if (!prioQueueStr.contains("Priority")) {
                throw new IOException("Priority queue string doesn't contain Priority: " + prioQueueStr);
            }
            if (!regularQMatcher.find()) {
                throw new IOException("didn't find regular queue len: " + queueStr);
            }
            if (!prioQMatcher.find()) {
                throw new IOException("didn't find priority queue len: " + prioQueueStr);
            }
            final int regular = Integer.parseInt(regularQMatcher.group());
            final int prio = Integer.parseInt(prioQMatcher.group());
            queueStatus = new QueueStatus(prio, regular, ZonedDateTime.now().toEpochSecond());
            return true;
        } catch (final Exception e) {
            SERVER_LOG.error("Failed updating queue with ping", e);
            return false;
        }
    }

    private static boolean apiUpdate() {
        try {
            var response = VC.getQueue().orElseThrow();
            queueStatus = new QueueStatus(response.prio(), response.regular(), response.time().toEpochSecond());
            return true;
        } catch (final Exception e) {
            SERVER_LOG.error("Failed updating queue status from API", e);
            return false;
        }
    }
}
