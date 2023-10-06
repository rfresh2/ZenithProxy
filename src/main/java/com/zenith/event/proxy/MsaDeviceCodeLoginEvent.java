package com.zenith.event.proxy;

import com.microsoft.aad.msal4j.DeviceCode;

public record MsaDeviceCodeLoginEvent(DeviceCode deviceCode) { }
