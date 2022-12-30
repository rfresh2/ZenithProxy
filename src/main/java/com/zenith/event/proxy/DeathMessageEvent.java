package com.zenith.event.proxy;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;

@EventInfo(preference = Preference.POOL)
public class DeathMessageEvent {
    public final String message;
    public final MCTextRoot mcTextRoot;

    public DeathMessageEvent(final String message, final MCTextRoot mcTextRoot) {
        this.message = message;
        this.mcTextRoot = mcTextRoot;
    }
}
