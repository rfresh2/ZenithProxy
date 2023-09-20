package com.zenith.feature.pathing;

import com.zenith.util.math.MathHelper;
import lombok.experimental.UtilityClass;

import static com.zenith.Shared.CACHE;

@UtilityClass
public class Pathing {

    public double calculateFallDamage(final double distance) {
        // todo: check if we have feather falling + prot 4 which will increase our distance to 103 before death
        // todo: check if we're falling into a liquid
        // todo: check if we have a totem equipped
        return Math.max(0, distance - 3.5);
    }

    public Position getCurrentPlayerPos() {
        return new Position(MathHelper.round(CACHE.getPlayerCache().getX(), 5), MathHelper.round(CACHE.getPlayerCache().getY(), 5), MathHelper.round(CACHE.getPlayerCache().getZ(), 5));
    }
}
