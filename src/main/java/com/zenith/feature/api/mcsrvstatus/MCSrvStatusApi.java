package com.zenith.feature.api.mcsrvstatus;

import com.zenith.feature.api.Api;
import com.zenith.feature.api.mcsrvstatus.model.MCSrvStatusResponse;

import java.util.Optional;

public class MCSrvStatusApi extends Api {
    public static MCSrvStatusApi INSTANCE = new MCSrvStatusApi();

    public MCSrvStatusApi() {
        super("https://api.mcsrvstat.us/3");
    }

    public Optional<MCSrvStatusResponse> getMCSrvStatus(final String address) {
        return get("/" + address, MCSrvStatusResponse.class);
    }
}
