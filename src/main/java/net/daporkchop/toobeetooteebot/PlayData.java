package net.daporkchop.toobeetooteebot;

import java.io.Serializable;

public class PlayData implements Serializable {
    public final String UUID;
    public String name;
    public int[] playTimeByDay = new int[30];
    public int[] playTimeByHour = new int[720];
    public long lastPlayed;

    public PlayData(String UUID, String name)   {
        this.UUID = UUID;
        this.name = name;
        this.lastPlayed = System.currentTimeMillis();
        for (int i = 0; i < playTimeByDay.length; i++)  {
            playTimeByDay[i] = 0;
        }
        for (int i = 0; i < playTimeByHour.length; i++)  {
            playTimeByHour[i] = 0;
        }
    }
}
