package com.zenith.feature.api.mcstatus;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.mcstatus.model.MCStatusResponse;

import java.util.Optional;

public class MCStatusApi extends Api {
    public static MCStatusApi INSTANCE = new MCStatusApi();

    public MCStatusApi() {
        super("https://api.mcstatus.io/v2/status/java");
    }

    public Optional<MCStatusResponse> getMCServerStatus(final String address) {
        return get("/" + address, MCStatusResponse.class);
    }
}
