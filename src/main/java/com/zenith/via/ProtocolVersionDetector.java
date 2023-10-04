package com.zenith.via;

import com.zenith.feature.queue.mcping.MCPing;
import com.zenith.feature.queue.mcping.PingOptions;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProtocolVersionDetector {
    private static final MCPing mcPing = new MCPing();
    public int getProtocolVersion(final String hostname, final int port) {
        try {
            final PingOptions pingOptions = new PingOptions();
            pingOptions.setHostname(hostname);
            pingOptions.setPort(port);
            pingOptions.setTimeout(3000);
            pingOptions.setProtocolVersion(MCPing.PROTOCOL_VERSION_DISCOVERY);
            pingOptions.setResolveDns(true);
            final MCPing.ResponseDetails pingWithDetails = mcPing.getPingWithDetails(pingOptions);
            return pingWithDetails.standard.getVersion().getProtocol();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
