package com.zenith.mc.entity;

import com.zenith.util.Registry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

public final class EntityRegistry {
    public static final Registry<EntityData> REGISTRY = new Registry<>(126);

    public static final EntityData ALLAY = register(new EntityData(0, "allay", 0.35f, 0.6f, EntityType.ALLAY));

    public static final EntityData AREA_EFFECT_CLOUD = register(new EntityData(1, "area_effect_cloud", 6.0f, 0.5f, EntityType.AREA_EFFECT_CLOUD));

    public static final EntityData ARMOR_STAND = register(new EntityData(2, "armor_stand", 0.5f, 1.975f, EntityType.ARMOR_STAND));

    public static final EntityData ARROW = register(new EntityData(3, "arrow", 0.5f, 0.5f, EntityType.ARROW));

    public static final EntityData AXOLOTL = register(new EntityData(4, "axolotl", 0.75f, 0.42f, EntityType.AXOLOTL));

    public static final EntityData BAT = register(new EntityData(5, "bat", 0.5f, 0.9f, EntityType.BAT));

    public static final EntityData BEE = register(new EntityData(6, "bee", 0.7f, 0.6f, EntityType.BEE));

    public static final EntityData BLAZE = register(new EntityData(7, "blaze", 0.6f, 1.8f, EntityType.BLAZE));

    public static final EntityData BLOCK_DISPLAY = register(new EntityData(8, "block_display", 0.0f, 0.0f, EntityType.BLOCK_DISPLAY));

    public static final EntityData BOAT = register(new EntityData(9, "boat", 1.375f, 0.5625f, EntityType.BOAT));

    public static final EntityData BREEZE = register(new EntityData(10, "breeze", 0.6f, 1.7f, EntityType.BREEZE));

    public static final EntityData CAMEL = register(new EntityData(11, "camel", 1.7f, 2.375f, EntityType.CAMEL));

    public static final EntityData CAT = register(new EntityData(12, "cat", 0.6f, 0.7f, EntityType.CAT));

    public static final EntityData CAVE_SPIDER = register(new EntityData(13, "cave_spider", 0.7f, 0.5f, EntityType.CAVE_SPIDER));

    public static final EntityData CHEST_BOAT = register(new EntityData(14, "chest_boat", 1.375f, 0.5625f, EntityType.CHEST_BOAT));

    public static final EntityData CHEST_MINECART = register(new EntityData(15, "chest_minecart", 0.98f, 0.7f, EntityType.CHEST_MINECART));

    public static final EntityData CHICKEN = register(new EntityData(16, "chicken", 0.4f, 0.7f, EntityType.CHICKEN));

    public static final EntityData COD = register(new EntityData(17, "cod", 0.5f, 0.3f, EntityType.COD));

    public static final EntityData COMMAND_BLOCK_MINECART = register(new EntityData(18, "command_block_minecart", 0.98f, 0.7f, EntityType.COMMAND_BLOCK_MINECART));

    public static final EntityData COW = register(new EntityData(19, "cow", 0.9f, 1.4f, EntityType.COW));

    public static final EntityData CREEPER = register(new EntityData(20, "creeper", 0.6f, 1.7f, EntityType.CREEPER));

    public static final EntityData DOLPHIN = register(new EntityData(21, "dolphin", 0.9f, 0.6f, EntityType.DOLPHIN));

    public static final EntityData DONKEY = register(new EntityData(22, "donkey", 1.3964844f, 1.5f, EntityType.DONKEY));

    public static final EntityData DRAGON_FIREBALL = register(new EntityData(23, "dragon_fireball", 1.0f, 1.0f, EntityType.DRAGON_FIREBALL));

    public static final EntityData DROWNED = register(new EntityData(24, "drowned", 0.6f, 1.95f, EntityType.DROWNED));

    public static final EntityData EGG = register(new EntityData(25, "egg", 0.25f, 0.25f, EntityType.EGG));

    public static final EntityData ELDER_GUARDIAN = register(new EntityData(26, "elder_guardian", 1.9975f, 1.9975f, EntityType.ELDER_GUARDIAN));

    public static final EntityData END_CRYSTAL = register(new EntityData(27, "end_crystal", 2.0f, 2.0f, EntityType.END_CRYSTAL));

    public static final EntityData ENDER_DRAGON = register(new EntityData(28, "ender_dragon", 16.0f, 8.0f, EntityType.ENDER_DRAGON));

    public static final EntityData ENDER_PEARL = register(new EntityData(29, "ender_pearl", 0.25f, 0.25f, EntityType.ENDER_PEARL));

    public static final EntityData ENDERMAN = register(new EntityData(30, "enderman", 0.6f, 2.9f, EntityType.ENDERMAN));

    public static final EntityData ENDERMITE = register(new EntityData(31, "endermite", 0.4f, 0.3f, EntityType.ENDERMITE));

    public static final EntityData EVOKER = register(new EntityData(32, "evoker", 0.6f, 1.95f, EntityType.EVOKER));

    public static final EntityData EVOKER_FANGS = register(new EntityData(33, "evoker_fangs", 0.5f, 0.8f, EntityType.EVOKER_FANGS));

    public static final EntityData EXPERIENCE_BOTTLE = register(new EntityData(34, "experience_bottle", 0.25f, 0.25f, EntityType.EXPERIENCE_BOTTLE));

    public static final EntityData EXPERIENCE_ORB = register(new EntityData(35, "experience_orb", 0.5f, 0.5f, EntityType.EXPERIENCE_ORB));

    public static final EntityData EYE_OF_ENDER = register(new EntityData(36, "eye_of_ender", 0.25f, 0.25f, EntityType.EYE_OF_ENDER));

    public static final EntityData FALLING_BLOCK = register(new EntityData(37, "falling_block", 0.98f, 0.98f, EntityType.FALLING_BLOCK));

    public static final EntityData FIREWORK_ROCKET = register(new EntityData(38, "firework_rocket", 0.25f, 0.25f, EntityType.FIREWORK_ROCKET));

    public static final EntityData FOX = register(new EntityData(39, "fox", 0.6f, 0.7f, EntityType.FOX));

    public static final EntityData FROG = register(new EntityData(40, "frog", 0.5f, 0.5f, EntityType.FROG));

    public static final EntityData FURNACE_MINECART = register(new EntityData(41, "furnace_minecart", 0.98f, 0.7f, EntityType.FURNACE_MINECART));

    public static final EntityData GHAST = register(new EntityData(42, "ghast", 4.0f, 4.0f, EntityType.GHAST));

    public static final EntityData GIANT = register(new EntityData(43, "giant", 3.6f, 12.0f, EntityType.GIANT));

    public static final EntityData GLOW_ITEM_FRAME = register(new EntityData(44, "glow_item_frame", 0.5f, 0.5f, EntityType.GLOW_ITEM_FRAME));

    public static final EntityData GLOW_SQUID = register(new EntityData(45, "glow_squid", 0.8f, 0.8f, EntityType.GLOW_SQUID));

    public static final EntityData GOAT = register(new EntityData(46, "goat", 0.9f, 1.3f, EntityType.GOAT));

    public static final EntityData GUARDIAN = register(new EntityData(47, "guardian", 0.85f, 0.85f, EntityType.GUARDIAN));

    public static final EntityData HOGLIN = register(new EntityData(48, "hoglin", 1.3964844f, 1.4f, EntityType.HOGLIN));

    public static final EntityData HOPPER_MINECART = register(new EntityData(49, "hopper_minecart", 0.98f, 0.7f, EntityType.HOPPER_MINECART));

    public static final EntityData HORSE = register(new EntityData(50, "horse", 1.3964844f, 1.6f, EntityType.HORSE));

    public static final EntityData HUSK = register(new EntityData(51, "husk", 0.6f, 1.95f, EntityType.HUSK));

    public static final EntityData ILLUSIONER = register(new EntityData(52, "illusioner", 0.6f, 1.95f, EntityType.ILLUSIONER));

    public static final EntityData INTERACTION = register(new EntityData(53, "interaction", 0.0f, 0.0f, EntityType.INTERACTION));

    public static final EntityData IRON_GOLEM = register(new EntityData(54, "iron_golem", 1.4f, 2.7f, EntityType.IRON_GOLEM));

    public static final EntityData ITEM = register(new EntityData(55, "item", 0.25f, 0.25f, EntityType.ITEM));

    public static final EntityData ITEM_DISPLAY = register(new EntityData(56, "item_display", 0.0f, 0.0f, EntityType.ITEM_DISPLAY));

    public static final EntityData ITEM_FRAME = register(new EntityData(57, "item_frame", 0.5f, 0.5f, EntityType.ITEM_FRAME));

    public static final EntityData FIREBALL = register(new EntityData(58, "fireball", 1.0f, 1.0f, EntityType.FIREBALL));

    public static final EntityData LEASH_KNOT = register(new EntityData(59, "leash_knot", 0.375f, 0.5f, EntityType.LEASH_KNOT));

    public static final EntityData LIGHTNING_BOLT = register(new EntityData(60, "lightning_bolt", 0.0f, 0.0f, EntityType.LIGHTNING_BOLT));

    public static final EntityData LLAMA = register(new EntityData(61, "llama", 0.9f, 1.87f, EntityType.LLAMA));

    public static final EntityData LLAMA_SPIT = register(new EntityData(62, "llama_spit", 0.25f, 0.25f, EntityType.LLAMA_SPIT));

    public static final EntityData MAGMA_CUBE = register(new EntityData(63, "magma_cube", 2.04f, 2.04f, EntityType.MAGMA_CUBE));

    public static final EntityData MARKER = register(new EntityData(64, "marker", 0.0f, 0.0f, EntityType.MARKER));

    public static final EntityData MINECART = register(new EntityData(65, "minecart", 0.98f, 0.7f, EntityType.MINECART));

    public static final EntityData MOOSHROOM = register(new EntityData(66, "mooshroom", 0.9f, 1.4f, EntityType.MOOSHROOM));

    public static final EntityData MULE = register(new EntityData(67, "mule", 1.3964844f, 1.6f, EntityType.MULE));

    public static final EntityData OCELOT = register(new EntityData(68, "ocelot", 0.6f, 0.7f, EntityType.OCELOT));

    public static final EntityData PAINTING = register(new EntityData(69, "painting", 0.5f, 0.5f, EntityType.PAINTING));

    public static final EntityData PANDA = register(new EntityData(70, "panda", 1.3f, 1.25f, EntityType.PANDA));

    public static final EntityData PARROT = register(new EntityData(71, "parrot", 0.5f, 0.9f, EntityType.PARROT));

    public static final EntityData PHANTOM = register(new EntityData(72, "phantom", 0.9f, 0.5f, EntityType.PHANTOM));

    public static final EntityData PIG = register(new EntityData(73, "pig", 0.9f, 0.9f, EntityType.PIG));

    public static final EntityData PIGLIN = register(new EntityData(74, "piglin", 0.6f, 1.95f, EntityType.PIGLIN));

    public static final EntityData PIGLIN_BRUTE = register(new EntityData(75, "piglin_brute", 0.6f, 1.95f, EntityType.PIGLIN_BRUTE));

    public static final EntityData PILLAGER = register(new EntityData(76, "pillager", 0.6f, 1.95f, EntityType.PILLAGER));

    public static final EntityData POLAR_BEAR = register(new EntityData(77, "polar_bear", 1.4f, 1.4f, EntityType.POLAR_BEAR));

    public static final EntityData POTION = register(new EntityData(78, "potion", 0.25f, 0.25f, EntityType.POTION));

    public static final EntityData PUFFERFISH = register(new EntityData(79, "pufferfish", 0.7f, 0.7f, EntityType.PUFFERFISH));

    public static final EntityData RABBIT = register(new EntityData(80, "rabbit", 0.4f, 0.5f, EntityType.RABBIT));

    public static final EntityData RAVAGER = register(new EntityData(81, "ravager", 1.95f, 2.2f, EntityType.RAVAGER));

    public static final EntityData SALMON = register(new EntityData(82, "salmon", 0.7f, 0.4f, EntityType.SALMON));

    public static final EntityData SHEEP = register(new EntityData(83, "sheep", 0.9f, 1.3f, EntityType.SHEEP));

    public static final EntityData SHULKER = register(new EntityData(84, "shulker", 1.0f, 1.0f, EntityType.SHULKER));

    public static final EntityData SHULKER_BULLET = register(new EntityData(85, "shulker_bullet", 0.3125f, 0.3125f, EntityType.SHULKER_BULLET));

    public static final EntityData SILVERFISH = register(new EntityData(86, "silverfish", 0.4f, 0.3f, EntityType.SILVERFISH));

    public static final EntityData SKELETON = register(new EntityData(87, "skeleton", 0.6f, 1.99f, EntityType.SKELETON));

    public static final EntityData SKELETON_HORSE = register(new EntityData(88, "skeleton_horse", 1.3964844f, 1.6f, EntityType.SKELETON_HORSE));

    public static final EntityData SLIME = register(new EntityData(89, "slime", 2.04f, 2.04f, EntityType.SLIME));

    public static final EntityData SMALL_FIREBALL = register(new EntityData(90, "small_fireball", 0.3125f, 0.3125f, EntityType.SMALL_FIREBALL));

    public static final EntityData SNIFFER = register(new EntityData(91, "sniffer", 1.9f, 1.75f, EntityType.SNIFFER));

    public static final EntityData SNOW_GOLEM = register(new EntityData(92, "snow_golem", 0.7f, 1.9f, EntityType.SNOW_GOLEM));

    public static final EntityData SNOWBALL = register(new EntityData(93, "snowball", 0.25f, 0.25f, EntityType.SNOWBALL));

    public static final EntityData SPAWNER_MINECART = register(new EntityData(94, "spawner_minecart", 0.98f, 0.7f, EntityType.SPAWNER_MINECART));

    public static final EntityData SPECTRAL_ARROW = register(new EntityData(95, "spectral_arrow", 0.5f, 0.5f, EntityType.SPECTRAL_ARROW));

    public static final EntityData SPIDER = register(new EntityData(96, "spider", 1.4f, 0.9f, EntityType.SPIDER));

    public static final EntityData SQUID = register(new EntityData(97, "squid", 0.8f, 0.8f, EntityType.SQUID));

    public static final EntityData STRAY = register(new EntityData(98, "stray", 0.6f, 1.99f, EntityType.STRAY));

    public static final EntityData STRIDER = register(new EntityData(99, "strider", 0.9f, 1.7f, EntityType.STRIDER));

    public static final EntityData TADPOLE = register(new EntityData(100, "tadpole", 0.4f, 0.3f, EntityType.TADPOLE));

    public static final EntityData TEXT_DISPLAY = register(new EntityData(101, "text_display", 0.0f, 0.0f, EntityType.TEXT_DISPLAY));

    public static final EntityData TNT = register(new EntityData(102, "tnt", 0.98f, 0.98f, EntityType.TNT));

    public static final EntityData TNT_MINECART = register(new EntityData(103, "tnt_minecart", 0.98f, 0.7f, EntityType.TNT_MINECART));

    public static final EntityData TRADER_LLAMA = register(new EntityData(104, "trader_llama", 0.9f, 1.87f, EntityType.TRADER_LLAMA));

    public static final EntityData TRIDENT = register(new EntityData(105, "trident", 0.5f, 0.5f, EntityType.TRIDENT));

    public static final EntityData TROPICAL_FISH = register(new EntityData(106, "tropical_fish", 0.5f, 0.4f, EntityType.TROPICAL_FISH));

    public static final EntityData TURTLE = register(new EntityData(107, "turtle", 1.2f, 0.4f, EntityType.TURTLE));

    public static final EntityData VEX = register(new EntityData(108, "vex", 0.4f, 0.8f, EntityType.VEX));

    public static final EntityData VILLAGER = register(new EntityData(109, "villager", 0.6f, 1.95f, EntityType.VILLAGER));

    public static final EntityData VINDICATOR = register(new EntityData(110, "vindicator", 0.6f, 1.95f, EntityType.VINDICATOR));

    public static final EntityData WANDERING_TRADER = register(new EntityData(111, "wandering_trader", 0.6f, 1.95f, EntityType.WANDERING_TRADER));

    public static final EntityData WARDEN = register(new EntityData(112, "warden", 0.9f, 2.9f, EntityType.WARDEN));

    public static final EntityData WIND_CHARGE = register(new EntityData(113, "wind_charge", 0.3125f, 0.3125f, EntityType.WIND_CHARGE));

    public static final EntityData WITCH = register(new EntityData(114, "witch", 0.6f, 1.95f, EntityType.WITCH));

    public static final EntityData WITHER = register(new EntityData(115, "wither", 0.9f, 3.5f, EntityType.WITHER));

    public static final EntityData WITHER_SKELETON = register(new EntityData(116, "wither_skeleton", 0.7f, 2.4f, EntityType.WITHER_SKELETON));

    public static final EntityData WITHER_SKULL = register(new EntityData(117, "wither_skull", 0.3125f, 0.3125f, EntityType.WITHER_SKULL));

    public static final EntityData WOLF = register(new EntityData(118, "wolf", 0.6f, 0.85f, EntityType.WOLF));

    public static final EntityData ZOGLIN = register(new EntityData(119, "zoglin", 1.3964844f, 1.4f, EntityType.ZOGLIN));

    public static final EntityData ZOMBIE = register(new EntityData(120, "zombie", 0.6f, 1.95f, EntityType.ZOMBIE));

    public static final EntityData ZOMBIE_HORSE = register(new EntityData(121, "zombie_horse", 1.3964844f, 1.6f, EntityType.ZOMBIE_HORSE));

    public static final EntityData ZOMBIE_VILLAGER = register(new EntityData(122, "zombie_villager", 0.6f, 1.95f, EntityType.ZOMBIE_VILLAGER));

    public static final EntityData ZOMBIFIED_PIGLIN = register(new EntityData(123, "zombified_piglin", 0.6f, 1.95f, EntityType.ZOMBIFIED_PIGLIN));

    public static final EntityData PLAYER = register(new EntityData(124, "player", 0.6f, 1.8f, EntityType.PLAYER));

    public static final EntityData FISHING_BOBBER = register(new EntityData(125, "fishing_bobber", 0.25f, 0.25f, EntityType.FISHING_BOBBER));

    private static EntityData register(EntityData value) {
        REGISTRY.put(value.id(), value);
        return value;
    }
}
