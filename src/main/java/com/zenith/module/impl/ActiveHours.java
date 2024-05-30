package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.event.proxy.ActiveHoursConnectEvent;
import com.zenith.feature.queue.Queue;
import com.zenith.module.Module;
import org.jetbrains.annotations.Nullable;

import java.time.*;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.zenith.Shared.*;

public class ActiveHours extends Module {
    private @Nullable ScheduledFuture<?> activeHoursTickFuture;
    private Instant lastActiveHoursConnect = Instant.EPOCH;

    @Override
    public void subscribeEvents() {

    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.utility.actions.activeHours.enabled;
    }

    @Override
    public void onEnable() {
        activeHoursTickFuture = EXECUTOR.scheduleAtFixedRate(this::handleActiveHoursTick, 0L, 1L, TimeUnit.MINUTES);
    }

    @Override
    public void onDisable() {
        if (activeHoursTickFuture != null) {
            activeHoursTickFuture.cancel(false);
        }
    }

    private void handleActiveHoursTick() {
        var activeHoursConfig = CONFIG.client.extra.utility.actions.activeHours;
        var proxy = Proxy.getInstance();
        if (proxy.isOn2b2t() && (proxy.isPrio() && proxy.isConnected())) return;
        if (proxy.hasActivePlayer() && !activeHoursConfig.forceReconnect) return;
        if (lastActiveHoursConnect.isAfter(Instant.now().minus(Duration.ofHours(1)))) return;

        var queueLength = proxy.isOn2b2t()
            ? proxy.isPrio()
                ? Queue.getQueueStatus().prio()
                : Queue.getQueueStatus().regular()
            : 0;
        var queueWaitSeconds = activeHoursConfig.queueEtaCalc ? Queue.getQueueWait(queueLength) : 0;
        var nowPlusQueueWait = LocalDateTime.now(ZoneId.of(activeHoursConfig.timeZoneId))
            .plusSeconds(queueWaitSeconds)
            .atZone(ZoneId.of(activeHoursConfig.timeZoneId))
            .toInstant();
        var activeTimes = activeHoursConfig.activeTimes.stream()
            .flatMap(activeTime -> {
                var activeHourToday = ZonedDateTime.of(
                    LocalDate.now(ZoneId.of(activeHoursConfig.timeZoneId)),
                    LocalTime.of(activeTime.hour, activeTime.minute),
                    ZoneId.of(activeHoursConfig.timeZoneId));
                var activeHourTomorrow = activeHourToday.plusDays(1L);
                return Stream.of(activeHourToday, activeHourTomorrow);
            })
            .map(ChronoZonedDateTime::toInstant)
            .toList();
        // active hour within 10 mins range of now
        var timeRange = Duration.ofMinutes(5); // x2
        for (Instant activeTime : activeTimes) {
            if (nowPlusQueueWait.isAfter(activeTime.minus(timeRange))
                && nowPlusQueueWait.isBefore(activeTime.plus(timeRange))) {
                info("Connect triggered for registered time: {}", activeTime);
                EVENT_BUS.postAsync(new ActiveHoursConnectEvent());
                this.lastActiveHoursConnect = Instant.now();
                proxy.disconnect(SYSTEM_DISCONNECT);
                EXECUTOR.schedule(proxy::connectAndCatchExceptions, 1, TimeUnit.MINUTES);
                break;
            }
        }
    }

    public static class ActiveTime {
        public int hour;
        public int minute;

        public static ActiveTime fromString(final String arg) {
            final String[] split = arg.split(":");
            final int hour = Integer.parseInt(split[0]);
            final int minute = Integer.parseInt(split[1]);
            ActiveTime activeTime = new ActiveTime();
            activeTime.hour = hour;
            activeTime.minute = minute;
            return activeTime;
        }

        @Override
        public String toString() {
            return (hour < 10 ? "0" + hour : hour) + ":" + (minute < 10 ? "0" + minute : minute);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ActiveTime that = (ActiveTime) o;
            return hour == that.hour && minute == that.minute;
        }

        @Override
        public int hashCode() {
            return Objects.hash(hour, minute);
        }

    }
}
