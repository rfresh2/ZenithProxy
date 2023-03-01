import com.zenith.mcping.MCPing;
import com.zenith.mcping.PingOptions;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingTest {
    //    @Test
    public void test() {
        final MCPing mcPing = new MCPing();
        final PingOptions pingOptions = new PingOptions();
        pingOptions.setHostname("connect.2b2t.org");
        pingOptions.setPort(25565);
        pingOptions.setTimeout(3000);
        pingOptions.setProtocolVersion(340);
        final Pattern pattern = Pattern.compile("\\d+");

        try {
            final MCPing.ResponseDetails pingWithDetails = mcPing.getPingWithDetails(pingOptions);
            final String queueStr = pingWithDetails.standard.getPlayers().getSample().get(0).getName();
            final Matcher matcher = pattern.matcher(queueStr.substring(queueStr.indexOf(" ")));
            if (!matcher.find()) {
                throw new IOException("didn't find regular queue len");
            }
            final Integer regular = Integer.parseInt(matcher.group());
            if (!matcher.find()) {
                throw new IOException("didn't find regular queue len");
            }
            final Integer prio = Integer.parseInt(matcher.group());


        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

    }
}
