package com.zenith.feature.queue.mcping;

import com.zenith.feature.queue.mcping.data.MCResponse;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingTest {
//    @Test
    public void test() throws IOException {
        final Pattern digitPattern = Pattern.compile("\\d+");
        final MCResponse pingWithDetails = MCPing.INSTANCE.ping("2b2t.org", 25565, 3000, true);
        final String queueStr = pingWithDetails.players().sample().get(1).name();
        final Matcher regularQMatcher = digitPattern.matcher(queueStr.substring(queueStr.lastIndexOf(" ")));
        final String prioQueueStr = pingWithDetails.players().sample().get(2).name();
        final Matcher prioQMatcher = digitPattern.matcher(prioQueueStr.substring(prioQueueStr.lastIndexOf(" ")));
        if (!queueStr.contains("Queue")) {
            throw new IOException("Queue string doesn't contain Queue: " + queueStr);
        }
        if (!regularQMatcher.find()) {
            throw new IOException("didn't find regular queue len: " + queueStr);
        }
        if (!prioQMatcher.find()) {
            throw new IOException("didn't find priority queue len: " + prioQueueStr);
        }
        if (!prioQueueStr.contains("Priority")) {
            throw new IOException("Priority queue string doesn't contain Priority: " + prioQueueStr);
        }
        final int regular = Integer.parseInt(regularQMatcher.group());
        final int prio = Integer.parseInt(prioQMatcher.group());
        System.out.println("Regular: " + regular + ", Priority: " + prio);
    }

//    @Test
    public void protocolVersionDetectorTest() throws IOException {
        var version = MCPing.INSTANCE.getProtocolVersion("2b2t.org", 25565, 3000, true);
        Assertions.assertEquals(765, version);
    }
}
