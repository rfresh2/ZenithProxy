package com.zenith.event.proxy;

import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

public record MsaDeviceCodeLoginEvent(StepMsaDeviceCode.MsaDeviceCode deviceCode) { }
