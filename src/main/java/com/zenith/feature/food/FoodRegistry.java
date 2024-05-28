package com.zenith.feature.food;

import com.zenith.util.Registry;

public final class FoodRegistry {
    public static final Registry<FoodData> REGISTRY = new Registry<>(40);

    public static final FoodData APPLE = register(new FoodData(796, "apple", 64, 4.0, 2.4000000953674316, true));

    public static final FoodData MUSHROOM_STEW = register(new FoodData(846, "mushroom_stew", 1, 6.0, 7.200000286102295, true));

    public static final FoodData BREAD = register(new FoodData(852, "bread", 64, 5.0, 6.0, true));

    public static final FoodData PORKCHOP = register(new FoodData(878, "porkchop", 64, 3.0, 1.8000000715255737, true));

    public static final FoodData COOKED_PORKCHOP = register(new FoodData(879, "cooked_porkchop", 64, 8.0, 12.800000190734863, true));

    public static final FoodData GOLDEN_APPLE = register(new FoodData(881, "golden_apple", 64, 4.0, 9.600000381469727, true));

    public static final FoodData ENCHANTED_GOLDEN_APPLE = register(new FoodData(882, "enchanted_golden_apple", 64, 4.0, 9.600000381469727, true));

    public static final FoodData COD = register(new FoodData(932, "cod", 64, 2.0, 0.4000000059604645, true));

    public static final FoodData SALMON = register(new FoodData(933, "salmon", 64, 2.0, 0.4000000059604645, true));

    public static final FoodData TROPICAL_FISH = register(new FoodData(934, "tropical_fish", 64, 1.0, 0.20000000298023224, true));

    public static final FoodData PUFFERFISH = register(new FoodData(935, "pufferfish", 64, 1.0, 0.20000000298023224, false));

    public static final FoodData COOKED_COD = register(new FoodData(936, "cooked_cod", 64, 5.0, 6.0, true));

    public static final FoodData COOKED_SALMON = register(new FoodData(937, "cooked_salmon", 64, 6.0, 9.600000381469727, true));

    public static final FoodData COOKIE = register(new FoodData(977, "cookie", 64, 2.0, 0.4000000059604645, true));

    public static final FoodData MELON_SLICE = register(new FoodData(981, "melon_slice", 64, 2.0, 1.2000000476837158, true));

    public static final FoodData DRIED_KELP = register(new FoodData(982, "dried_kelp", 64, 1.0, 0.6000000238418579, true));

    public static final FoodData BEEF = register(new FoodData(985, "beef", 64, 3.0, 1.8000000715255737, true));

    public static final FoodData COOKED_BEEF = register(new FoodData(986, "cooked_beef", 64, 8.0, 12.800000190734863, true));

    public static final FoodData CHICKEN = register(new FoodData(987, "chicken", 64, 2.0, 1.2000000476837158, false));

    public static final FoodData COOKED_CHICKEN = register(new FoodData(988, "cooked_chicken", 64, 6.0, 7.200000286102295, true));

    public static final FoodData ROTTEN_FLESH = register(new FoodData(989, "rotten_flesh", 64, 4.0, 0.800000011920929, false));

    public static final FoodData SPIDER_EYE = register(new FoodData(997, "spider_eye", 64, 2.0, 3.200000047683716, false));

    public static final FoodData CARROT = register(new FoodData(1090, "carrot", 64, 3.0, 3.6000001430511475, true));

    public static final FoodData POTATO = register(new FoodData(1091, "potato", 64, 1.0, 0.6000000238418579, true));

    public static final FoodData BAKED_POTATO = register(new FoodData(1092, "baked_potato", 64, 5.0, 6.0, true));

    public static final FoodData POISONOUS_POTATO = register(new FoodData(1093, "poisonous_potato", 64, 2.0, 1.2000000476837158, false));

    public static final FoodData GOLDEN_CARROT = register(new FoodData(1095, "golden_carrot", 64, 6.0, 14.40000057220459, true));

    public static final FoodData PUMPKIN_PIE = register(new FoodData(1104, "pumpkin_pie", 64, 8.0, 4.800000190734863, true));

    public static final FoodData RABBIT = register(new FoodData(1111, "rabbit", 64, 3.0, 1.8000000715255737, true));

    public static final FoodData COOKED_RABBIT = register(new FoodData(1112, "cooked_rabbit", 64, 5.0, 6.0, true));

    public static final FoodData RABBIT_STEW = register(new FoodData(1113, "rabbit_stew", 1, 10.0, 12.0, true));

    public static final FoodData MUTTON = register(new FoodData(1124, "mutton", 64, 2.0, 1.2000000476837158, true));

    public static final FoodData COOKED_MUTTON = register(new FoodData(1125, "cooked_mutton", 64, 6.0, 9.600000381469727, true));

    public static final FoodData CHORUS_FRUIT = register(new FoodData(1143, "chorus_fruit", 64, 4.0, 2.4000000953674316, false));

    public static final FoodData BEETROOT = register(new FoodData(1147, "beetroot", 64, 1.0, 1.2000000476837158, true));

    public static final FoodData BEETROOT_SOUP = register(new FoodData(1149, "beetroot_soup", 1, 6.0, 7.200000286102295, true));

    public static final FoodData SUSPICIOUS_STEW = register(new FoodData(1183, "suspicious_stew", 1, 6.0, 7.200000286102295, true));

    public static final FoodData SWEET_BERRIES = register(new FoodData(1204, "sweet_berries", 64, 2.0, 0.4000000059604645, true));

    public static final FoodData GLOW_BERRIES = register(new FoodData(1205, "glow_berries", 64, 2.0, 0.4000000059604645, true));

    public static final FoodData HONEY_BOTTLE = register(new FoodData(1212, "honey_bottle", 16, 6.0, 1.2000000476837158, true));

    private static FoodData register(FoodData value) {
        REGISTRY.put(value.id(), value);
        return value;
    }
}
