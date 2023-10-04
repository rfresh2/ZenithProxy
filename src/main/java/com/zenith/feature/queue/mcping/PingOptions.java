package com.zenith.feature.queue.mcping;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PingOptions {
    private String hostname;
    private int port;
    private int timeout;
    private int protocolVersion = -1;
    private boolean resolveDns = true;
}
