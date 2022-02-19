package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.event.module.PlayerHealthChangedEvent;
import com.zenith.util.Constants;

public class AutoDisconnect extends Module {
    public AutoDisconnect(Proxy proxy) {
        super(proxy);
    }

    @Subscribe
    public void handleLowPlayerHealthEvent(final PlayerHealthChangedEvent event) {
        if (Constants.CONFIG.client.extra.utility.actions.autoDisconnect.enabled) {
            if (event.newHealth <= Constants.CONFIG.client.extra.utility.actions.autoDisconnect.health) {
                // todo: send discord message?
                this.proxy.disconnect();
            }
        }

    }
}
