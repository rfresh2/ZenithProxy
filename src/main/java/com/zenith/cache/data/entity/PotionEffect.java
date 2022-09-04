package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class PotionEffect {
    @NonNull
    public final Effect effect;
    public int amplifier;
    public int duration;
    public boolean ambient;
    public boolean showParticles;
}
