package com.zenith.module;

import com.zenith.Proxy;

import static com.zenith.util.Constants.EVENT_BUS;

/**
 * Module system base class.
 */
public abstract class Module {
    final Proxy proxy;

    public Module(final Proxy proxy) {
        this.proxy = proxy;
        EVENT_BUS.subscribe(this);
    }
}
