package com.zenith.cache.data.entity;

import com.viaversion.nbt.mini.MNBT;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

import javax.annotation.Nullable;

@Data
@AllArgsConstructor
@Accessors(chain = true)
public class PotionEffect {
    @NonNull
    public final Effect effect;
    public int amplifier;
    public int duration;
    public boolean ambient;
    public boolean showParticles;
    public boolean showIcon;
    @Nullable
    public MNBT factorData;
}
