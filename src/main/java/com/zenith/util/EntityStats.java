package com.zenith.util;
import java.util.HashMap;
import java.util.Map;

public class EntityStats {
    private Map<String, EntityData> dataMap;

    public EntityStats() {
        dataMap = new HashMap<>();
        loadData();
    }

    public void loadData() {
        dataMap.put("allay", new EntityData(0.36,0.6,0.35));
        dataMap.put("area_effect_cloud", new EntityData(0.425,0.5,6.0));
        dataMap.put("armor_stand", new EntityData(1.7775,1.975,0.5));
        dataMap.put("arrow", new EntityData(0.13,0.5,0.5));
        dataMap.put("axolotl", new EntityData(0.2751,0.42,0.75));
        dataMap.put("bat", new EntityData(0.45,0.9,0.5));
        dataMap.put("bee", new EntityData(0.3,0.6,0.7));
        dataMap.put("blaze", new EntityData(1.53,1.8,0.6));
        dataMap.put("block_display", new EntityData(0.0,0.0,0.0));
        dataMap.put("boat", new EntityData(0.5625,0.5625,1.375));
        dataMap.put("camel", new EntityData(2.275,2.375,1.7));
        dataMap.put("cat", new EntityData(0.35,0.7,0.6));
        dataMap.put("cave_spider", new EntityData(0.45,0.5,0.7));
        dataMap.put("chest_boat", new EntityData(0.5625,0.5625,1.375));
        dataMap.put("chest_minecart", new EntityData(0.595,0.7,0.98));
        dataMap.put("chicken", new EntityData(0.644,0.7,0.4));
        dataMap.put("cod", new EntityData(0.19500001,0.3,0.5));
        dataMap.put("command_block_minecart", new EntityData(0.595,0.7,0.98));
        dataMap.put("cow", new EntityData(1.3,1.4,0.9));
        dataMap.put("creeper", new EntityData(1.445,1.7,0.6));
        dataMap.put("dolphin", new EntityData(0.3,0.6,0.9));
        dataMap.put("donkey", new EntityData(1.425,1.5,1.3964844));
        dataMap.put("dragon_fireball", new EntityData(0.85,1.0,1.0));
        dataMap.put("drowned", new EntityData(1.74,1.95,0.6));
        dataMap.put("egg", new EntityData(0.2125,0.25,0.25));
        dataMap.put("elder_guardian", new EntityData(0.99875,1.9975,1.9975));
        dataMap.put("end_crystal", new EntityData(1.7,2.0,2.0));
        dataMap.put("ender_dragon", new EntityData(6.8,8.0,16.0));
        dataMap.put("ender_pearl", new EntityData(0.2125,0.25,0.25));
        dataMap.put("enderman", new EntityData(2.55,2.9,0.6));
        dataMap.put("endermite", new EntityData(0.13,0.3,0.4));
        dataMap.put("evoker", new EntityData(1.6575,1.95,0.6));
        dataMap.put("evoker_fangs", new EntityData(0.68,0.8,0.5));
        dataMap.put("experience_bottle", new EntityData(0.2125,0.25,0.25));
        dataMap.put("experience_orb", new EntityData(0.425,0.5,0.5));
        dataMap.put("eye_of_ender", new EntityData(0.2125,0.25,0.25));
        dataMap.put("falling_block", new EntityData(0.83300006,0.98,0.98));
        dataMap.put("firework_rocket", new EntityData(0.2125,0.25,0.25));
        dataMap.put("fox", new EntityData(0.4,0.7,0.6));
        dataMap.put("frog", new EntityData(0.425,0.5,0.5));
        dataMap.put("furnace_minecart", new EntityData(0.595,0.7,0.98));
        dataMap.put("ghast", new EntityData(2.6,4.0,4.0));
        dataMap.put("giant", new EntityData(10.440001,12.0,3.6));
        dataMap.put("glow_item_frame", new EntityData(0.0,0.5,0.5));
        dataMap.put("glow_squid", new EntityData(0.4,0.8,0.8));
        dataMap.put("goat", new EntityData(1.105,1.3,0.9));
        dataMap.put("guardian", new EntityData(0.425,0.85,0.85));
        dataMap.put("hoglin", new EntityData(1.19,1.4,1.3964844));
        dataMap.put("hopper_minecart", new EntityData(0.595,0.7,0.98));
        dataMap.put("horse", new EntityData(1.52,1.6,1.3964844));
        dataMap.put("husk", new EntityData(1.74,1.95,0.6));
        dataMap.put("illusioner", new EntityData(1.6575,1.95,0.6));
        dataMap.put("interaction", new EntityData(0.0,0.0,0.0));
        dataMap.put("iron_golem", new EntityData(2.295,2.7,1.4));
        dataMap.put("item", new EntityData(0.2125,0.25,0.25));
        dataMap.put("item_display", new EntityData(0.0,0.0,0.0));
        dataMap.put("item_frame", new EntityData(0.0,0.5,0.5));
        dataMap.put("fireball", new EntityData(0.85,1.0,1.0));
        dataMap.put("leash_knot", new EntityData(0.0625,0.5,0.375));
        dataMap.put("lightning_bolt", new EntityData(0.0,0.0,0.0));
        dataMap.put("llama", new EntityData(1.7765,1.87,0.9));
        dataMap.put("llama_spit", new EntityData(0.2125,0.25,0.25));
        dataMap.put("magma_cube", new EntityData(0.32512498,0.52019995,0.52019995));
        dataMap.put("marker", new EntityData(0.0,0.0,0.0));
        dataMap.put("minecart", new EntityData(0.595,0.7,0.98));
        dataMap.put("mooshroom", new EntityData(1.3,1.4,0.9));
        dataMap.put("mule", new EntityData(1.52,1.6,1.3964844));
        dataMap.put("ocelot", new EntityData(0.595,0.7,0.6));
        dataMap.put("painting", new EntityData(0.425,0.5,0.5));
        dataMap.put("panda", new EntityData(1.0625,1.25,1.3));
        dataMap.put("parrot", new EntityData(0.54,0.9,0.5));
        dataMap.put("phantom", new EntityData(0.175,0.5,0.9));
        dataMap.put("pig", new EntityData(0.765,0.9,0.9));
        dataMap.put("piglin", new EntityData(1.79,1.95,0.6));
        dataMap.put("piglin_brute", new EntityData(1.79,1.95,0.6));
        dataMap.put("pillager", new EntityData(1.6575,1.95,0.6));
        dataMap.put("polar_bear", new EntityData(1.19,1.4,1.4));
        dataMap.put("potion", new EntityData(0.2125,0.25,0.25));
        dataMap.put("pufferfish", new EntityData(0.22749999,0.35,0.35));
        dataMap.put("rabbit", new EntityData(0.425,0.5,0.4));
        dataMap.put("ravager", new EntityData(1.8700001,2.2,1.95));
        dataMap.put("salmon", new EntityData(0.26,0.4,0.7));
        dataMap.put("sheep", new EntityData(1.2349999,1.3,0.9));
        dataMap.put("shulker", new EntityData(0.5,1.0,1.0));
        dataMap.put("shulker_bullet", new EntityData(0.265625,0.3125,0.3125));
        dataMap.put("silverfish", new EntityData(0.13,0.3,0.4));
        dataMap.put("skeleton", new EntityData(1.74,1.99,0.6));
        dataMap.put("skeleton_horse", new EntityData(1.52,1.6,1.3964844));
        dataMap.put("slime", new EntityData(0.32512498,0.52019995,0.52019995));
        dataMap.put("small_fireball", new EntityData(0.265625,0.3125,0.3125));
        dataMap.put("sniffer", new EntityData(1.0500001,1.75,1.9));
        dataMap.put("snow_golem", new EntityData(1.7,1.9,0.7));
        dataMap.put("snowball", new EntityData(0.2125,0.25,0.25));
        dataMap.put("spawner_minecart", new EntityData(0.595,0.7,0.98));
        dataMap.put("spectral_arrow", new EntityData(0.13,0.5,0.5));
        dataMap.put("spider", new EntityData(0.65,0.9,1.4));
        dataMap.put("squid", new EntityData(0.4,0.8,0.8));
        dataMap.put("stray", new EntityData(1.74,1.99,0.6));
        dataMap.put("strider", new EntityData(1.445,1.7,0.9));
        dataMap.put("tadpole", new EntityData(0.19500001,0.3,0.4));
        dataMap.put("text_display", new EntityData(0.0,0.0,0.0));
        dataMap.put("tnt", new EntityData(0.15,0.98,0.98));
        dataMap.put("tnt_minecart", new EntityData(0.595,0.7,0.98));
        dataMap.put("trader_llama", new EntityData(1.7765,1.87,0.9));
        dataMap.put("trident", new EntityData(0.13,0.5,0.5));
        dataMap.put("tropical_fish", new EntityData(0.26,0.4,0.5));
        dataMap.put("turtle", new EntityData(0.34,0.4,1.2));
        dataMap.put("vex", new EntityData(0.51875,0.8,0.4));
        dataMap.put("villager", new EntityData(1.62,1.95,0.6));
        dataMap.put("vindicator", new EntityData(1.6575,1.95,0.6));
        dataMap.put("wandering_trader", new EntityData(1.62,1.95,0.6));
        dataMap.put("warden", new EntityData(2.4650002,2.9,0.9));
        dataMap.put("witch", new EntityData(1.62,1.95,0.6));
        dataMap.put("wither", new EntityData(2.9750001,3.5,0.9));
        dataMap.put("wither_skeleton", new EntityData(2.1,2.4,0.7));
        dataMap.put("wither_skull", new EntityData(0.265625,0.3125,0.3125));
        dataMap.put("wolf", new EntityData(0.68,0.85,0.6));
        dataMap.put("zoglin", new EntityData(1.19,1.4,1.3964844));
        dataMap.put("zombie", new EntityData(1.74,1.95,0.6));
        dataMap.put("zombie_horse", new EntityData(1.52,1.6,1.3964844));
        dataMap.put("zombie_villager", new EntityData(1.74,1.95,0.6));
        dataMap.put("zombified_piglin", new EntityData(1.79,1.95,0.6));
        dataMap.put("fishing_bobber", new EntityData(0.2125,0.25,0.25));
    }

    public EntityData getEntityData(String entityName) {
        return dataMap.get(entityName);
    }

    public static class EntityData {
        private double eyeHeight;
        private double totalHeight;
        private double totalWidth;

        public EntityData(double eyeHeight, double totalHeight, double totalWidth) {
            this.eyeHeight = eyeHeight;
            this.totalHeight = totalHeight;
            this.totalWidth = totalWidth;
        }

        public double getEyeHeight() {
            return eyeHeight;
        }

        public double getTotalHeight() {
            return totalHeight;
        }

        public double getTotalWidth() {
            return totalWidth;
        }
    }
}