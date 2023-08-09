package com.zenith.event.proxy;

import com.microsoft.aad.msal4j.DeviceCode;

public class MsaDeviceCodeLoginEvent {
    private final DeviceCode deviceCode;

    public MsaDeviceCodeLoginEvent(final DeviceCode deviceCode) {
        this.deviceCode = deviceCode;
    }

    public DeviceCode getDeviceCode() {
        return this.deviceCode;
    }
}
