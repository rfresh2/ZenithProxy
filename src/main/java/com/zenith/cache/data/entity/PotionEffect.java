package com.zenith.cache.data.entity;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import lombok.*;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;

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
    public boolean showIcon;
    @Nullable
    public CompoundTag factorData;
}
