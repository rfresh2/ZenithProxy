package com.zenith.feature.replay;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.zenith.Shared.LAUNCH_CONFIG;

@Data
public class ReplayMetadata {
    private final boolean singleplayer = false;
    private String serverName;
    /**
     * Duration of the replay in milliseconds.
     */
    private int duration;
    /**
     * Unix timestamp of when the recording was started in milliseconds.
     */
    private long date;
    /**
     * Minecraft version. (E.g. 1.8)
     */
    private String mcversion;
    /**
     * File format. Defaults to 'MCPR'
     */
    private final String fileFormat = "MCPR";
    private final int fileFormatVersion = 14;
    /**
     * Minecraft protocol version. Mandatory for `fileFormatVersion >= 13`.
     */
    private int protocol;
    private final String generator = "ZenithProxy " + LAUNCH_CONFIG.version;
    /**
     * The entity id of the player manually added to this replay which represents the recording player.
     * Must be a valid entity id (e.g. must not be -1). May not be set.
     */
    private int selfId = -1;
    /**
     * Array of UUIDs of all players which can be seen in this replay.
     */
    private List<String> players = new ArrayList<>();
}
