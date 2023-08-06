package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;
import com.microsoft.aad.msal4j.DeviceCode;

@EventInfo(preference = Preference.POOL)
public class MsaDeviceCodeLoginEvent {
    private final DeviceCode deviceCode;

    public MsaDeviceCodeLoginEvent(final DeviceCode deviceCode) {
        this.deviceCode = deviceCode;
    }

    public DeviceCode getDeviceCode() {
        return this.deviceCode;
    }
}
