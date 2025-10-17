package com.zenith.module.api;

import com.github.rfresh2.EventConsumer;
import com.zenith.network.codec.PacketCodecRegistries;
import com.zenith.network.codec.PacketHandlerCodec;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static com.zenith.Globals.EVENT_BUS;

/**
 * Module system base class.
 */
@Getter
public abstract class Module extends ModuleUtils {
    boolean enabled = false;
    @Nullable PacketHandlerCodec clientPacketHandlerCodec = null;
    @Nullable PacketHandlerCodec serverPacketHandlerCodec = null;

    public Module() {}

    /**
     * Whether this module should be enabled automatically on startup
     *
     * Typically, this should be read from a config boolean value
     */
    public abstract boolean enabledSetting();

    /**
     * Override this to register module events
     */
    public List<EventConsumer<?>> registerEvents() {
        return Collections.emptyList();
    }

    /**
     * Override this to register client packet handlers
     *
     * Meaning packets between zenith and the destination server
     */
    public @Nullable PacketHandlerCodec registerClientPacketHandlerCodec() {
        return null;
    }

    /**
     * Override this to register server packet handlers
     *
     * Meaning packets between zenith and players - both spectators and controlling player
     */
    public @Nullable PacketHandlerCodec registerServerPacketHandlerCodec() {
        return null;
    }

    /**
     * Override to run a custom hook when the module is enabled
     */
    public void onEnable() { }

    /**
     * Override to run a custom hook when the module is disabled
     */
    public void onDisable() { }


    /**
     * Enables the module, does not check enabledSetting() or set any config values
     */
    public synchronized void enable() {
        try {
            if (!enabled) {
                subscribeEvents();
                enabled = true;
                clientPacketHandlerCodec = registerClientPacketHandlerCodec();
                if (clientPacketHandlerCodec != null) {
                    PacketCodecRegistries.CLIENT_REGISTRY.register(clientPacketHandlerCodec);
                }
                serverPacketHandlerCodec = registerServerPacketHandlerCodec();
                if (serverPacketHandlerCodec != null) {
                    PacketCodecRegistries.SERVER_REGISTRY.register(serverPacketHandlerCodec);
                }
                onEnable();
                debug("Enabled");
            }
        } catch (Exception e) {
            error("Error enabling module", e);
            disable();
        }
    }

    /**
     * Disables the module, does not check enabledSetting() or set any config values
     */
    public synchronized void disable() {
        try {
            if (enabled) {
                enabled = false;
                unsubscribeEvents();
                if (clientPacketHandlerCodec != null) {
                    PacketCodecRegistries.CLIENT_REGISTRY.unregister(clientPacketHandlerCodec);
                }
                if (serverPacketHandlerCodec != null) {
                    PacketCodecRegistries.SERVER_REGISTRY.unregister(serverPacketHandlerCodec);
                }
                onDisable();
                debug("Disabled");
            }
        } catch (Exception e) {
            error("Error disabling module", e);
        }
    }

    /**
     * Sets the module enabled state, does not set any config values
     */
    public synchronized void setEnabled(boolean enabled) {
        if (enabled) {
            enable();
        } else {
            disable();
        }
    }

    /**
     * Sets the module enabled state from the result of enabledSetting()
     */
    public synchronized void syncEnabledFromConfig() {
        setEnabled(enabledSetting());
    }

    /**
     * Executed when the module is enabled
     *
     * Should not usually be overridden or run directly
     */
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this, registerEvents().toArray(new EventConsumer[0]));
    }

    /**
     * Executed when the module is disabled
     *
     * Should not usually be overridden or run directly
     */
    public void unsubscribeEvents() {
        EVENT_BUS.unsubscribe(this);
    }
}
