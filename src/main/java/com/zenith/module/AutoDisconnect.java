package com.zenith.module;

import com.collarmc.pounce.Subscribe;
import com.zenith.Proxy;
import com.zenith.event.module.PlayerHealthChangedEvent;
import com.zenith.event.proxy.HealthAutoDisconnectEvent;
import com.zenith.util.Constants;

import static com.zenith.util.Constants.EVENT_BUS;
import static java.util.Objects.isNull;

public class AutoDisconnect extends Module {
    public AutoDisconnect(Proxy proxy) {
        super(proxy);
    }

    @Subscribe
    public void handleLowPlayerHealthEvent(final PlayerHealthChangedEvent event) {
        if (Constants.CONFIG.client.extra.utility.actions.autoDisconnect.enabled) {
            if (event.newHealth <= Constants.CONFIG.client.extra.utility.actions.autoDisconnect.health) {
                if (isNull(this.proxy.getCurrentPlayer().get())) {
                    EVENT_BUS.dispatch(new HealthAutoDisconnectEvent());
                    this.proxy.disconnect();
                }
            }
        }

    }
}
