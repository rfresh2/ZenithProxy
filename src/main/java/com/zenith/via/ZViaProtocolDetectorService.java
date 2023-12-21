package com.zenith.via;

import com.google.common.collect.Maps;
import com.viaversion.viaversion.platform.AbstractProtocolDetectorService;

import java.util.Map;

import static com.zenith.Shared.CONFIG;

// todo: might not need this
public class ZViaProtocolDetectorService extends AbstractProtocolDetectorService {
    @Override
    protected Map<String, Integer> configuredServers() {
        final Map<String, Integer> map = Maps.newHashMap();
        map.put(CONFIG.client.server.address, CONFIG.client.server.port);
        return map;
    }

    @Override
    protected int lowestSupportedProtocolVersion() {
        return 0;
    }

    @Override
    public void probeAllServers() {
        // todo: uhhhh don't think this is needed
    }
}
