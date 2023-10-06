package com.zenith.event.proxy;

import java.time.Duration;

public record QueueCompleteEvent(Duration queueDuration) { }
