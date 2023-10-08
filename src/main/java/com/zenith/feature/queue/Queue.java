package com.zenith.feature.queue;

import com.zenith.feature.queue.mcping.MCPing;
import com.zenith.feature.queue.mcping.PingOptions;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zenith.Shared.*;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Objects.isNull;

public class Queue {
    private static final String apiUrl = "https://2bqueue.info/queue";
    private static HttpClient httpClient;
    private static QueueStatus queueStatus;
    private static final ScheduledExecutorService refreshExecutorService = new ScheduledThreadPoolExecutor(1);
    private static final Pattern digitPattern = Pattern.compile("\\d+");
    private static final MCPing mcPing = new MCPing();
    private static final PingOptions pingOptions = new PingOptions();

    public static void start() {
        refreshExecutorService.scheduleAtFixedRate(
            Queue::updateQueueStatus,
            500L,
            Duration.of(CONFIG.server.queueStatusRefreshMinutes, MINUTES).toMillis(),
            TimeUnit.MILLISECONDS);
        pingOptions.setHostname("connect.2b2t.org");
        pingOptions.setPort(25565);
        pingOptions.setTimeout(3000);
        pingOptions.setProtocolVersion(340);
        pingOptions.setResolveDns(false);
        httpClient = HttpClient.create()
            .secure()
            .baseUrl(apiUrl)
            .headers(h -> h.add(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON));
    }

    public static QueueStatus getQueueStatus() {
        if (isNull(queueStatus)) {
            updateQueueStatus();
        }
        return queueStatus;
    }

    private static void updateQueueStatus() {
        if (!pingUpdate()) {
            if (!apiUpdate()) {
                SERVER_LOG.error("Failed updating queue status. Is the network down?");
                if (isNull(queueStatus)) {
                    queueStatus = new QueueStatus(0, 0, 0, 0L, "");
                }
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

    private static boolean pingUpdate() {
        try {
            final MCPing.ResponseDetails pingWithDetails = mcPing.getPingWithDetails(pingOptions);
            final String queueStr = pingWithDetails.standard.getPlayers().getSample().get(1).getName();
            final Matcher regularQMatcher = digitPattern.matcher(queueStr.substring(queueStr.lastIndexOf(" ")));
            final String prioQueueStr = pingWithDetails.standard.getPlayers().getSample().get(2).getName();
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
            final Integer regular = Integer.parseInt(regularQMatcher.group());
            final Integer prio = Integer.parseInt(prioQMatcher.group());
            queueStatus = new QueueStatus(prio, regular, prio + regular, 0L, "");
            return true;
        } catch (final Exception e) {
            SERVER_LOG.error("Failed updating queue with ping", e);
            return false;
        }
    }

    private static boolean apiUpdate() {
        try {
            final String response = httpClient
                    .get()
                    .responseContent()
                    .aggregate()
                    .asString()
                    .block();
            queueStatus = OBJECT_MAPPER.readValue(response, QueueStatus.class);
            return true;
        } catch (final Exception e) {
            SERVER_LOG.error("Failed updating queue status from API", e);
            return false;
        }
    }
}
