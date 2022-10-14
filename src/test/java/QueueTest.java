import com.zenith.pathing.BlockDataManager;
import com.zenith.pathing.World;
import com.zenith.util.Queue;
import org.junit.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertTrue;

public class QueueTest {

    @Test
    public void queueEtaTest() {
        Duration queueEstimate = Duration.ofSeconds((long) Queue.getQueueWait(350, 350));
        System.out.println(queueEstimate.toString());
        assertTrue(queueEstimate.compareTo(Duration.of(140L, ChronoUnit.MINUTES)) > 0);
        assertTrue(queueEstimate.compareTo(Duration.of(4L, ChronoUnit.HOURS)) < 0);
    }

    @Test
    public void underscoreTest() {
        String message = "rfresh_ whispers: don't _fuck_ with me";
        // String s = Pattern.compile("[_]").matcher(message).replaceAll("\\\\_");
        String s = message.replaceAll("_", "\\\\_");
        System.out.println(s);
    }

    @Test
    public void worldTest() {
        final World world = new World(new BlockDataManager());


    }

}
