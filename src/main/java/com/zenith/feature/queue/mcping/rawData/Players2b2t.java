package com.zenith.feature.queue.mcping.rawData;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class Players2b2t {
    @SerializedName("max")
    private int max;
    @SerializedName("online")
    private int online;
    @SerializedName("sample")
    private List<Player2b2t> sample;

    public Players toPlayers() {
        Players players = new Players();
        players.setMax(this.max);
        players.setOnline(this.online);
        players.setSample(
            sample.stream()
                .map(player2b2t -> {
                    Player player = new Player();
                    player.setId(UUID.randomUUID().toString());
                    player.setName(player2b2t.getName());
                    return player;
                })
                .toList()
        );
        return players;
    }
}
