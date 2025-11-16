package com.zenith.event.client;

import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;

public record MsaDeviceCodeLoginEvent(MsaDeviceCode deviceCode) { }
