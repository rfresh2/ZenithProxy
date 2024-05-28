package com.zenith.feature.entities;

import com.zenith.util.Registry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

public final class EntityRegistry {
    public static final Registry<EntityData> REGISTRY = new Registry<>(126);

    public static final EntityData ALLAY = register(new EntityData(0, "allay", 0.3499999940395355, 0.6000000238418579, EntityType.ALLAY));

    public static final EntityData AREA_EFFECT_CLOUD = register(new EntityData(1, "area_effect_cloud", 6.0, 0.5, EntityType.AREA_EFFECT_CLOUD));

    public static final EntityData ARMOR_STAND = register(new EntityData(2, "armor_stand", 0.5, 1.975000023841858, EntityType.ARMOR_STAND));

    public static final EntityData ARROW = register(new EntityData(3, "arrow", 0.5, 0.5, EntityType.ARROW));

    public static final EntityData AXOLOTL = register(new EntityData(4, "axolotl", 0.75, 0.41999998688697815, EntityType.AXOLOTL));

    public static final EntityData BAT = register(new EntityData(5, "bat", 0.5, 0.8999999761581421, EntityType.BAT));

    public static final EntityData BEE = register(new EntityData(6, "bee", 0.699999988079071, 0.6000000238418579, EntityType.BEE));

    public static final EntityData BLAZE = register(new EntityData(7, "blaze", 0.6000000238418579, 1.7999999523162842, EntityType.BLAZE));

    public static final EntityData BLOCK_DISPLAY = register(new EntityData(8, "block_display", 0.0, 0.0, EntityType.BLOCK_DISPLAY));

    public static final EntityData BOAT = register(new EntityData(9, "boat", 1.375, 0.5625, EntityType.BOAT));

    public static final EntityData BREEZE = register(new EntityData(10, "breeze", 0.6000000238418579, 1.7000000476837158, EntityType.BREEZE));

    public static final EntityData CAMEL = register(new EntityData(11, "camel", 1.7000000476837158, 2.375, EntityType.CAMEL));

    public static final EntityData CAT = register(new EntityData(12, "cat", 0.6000000238418579, 0.699999988079071, EntityType.CAT));

    public static final EntityData CAVE_SPIDER = register(new EntityData(13, "cave_spider", 0.699999988079071, 0.5, EntityType.CAVE_SPIDER));

    public static final EntityData CHEST_BOAT = register(new EntityData(14, "chest_boat", 1.375, 0.5625, EntityType.CHEST_BOAT));

    public static final EntityData CHEST_MINECART = register(new EntityData(15, "chest_minecart", 0.9800000190734863, 0.699999988079071, EntityType.CHEST_MINECART));

    public static final EntityData CHICKEN = register(new EntityData(16, "chicken", 0.4000000059604645, 0.699999988079071, EntityType.CHICKEN));

    public static final EntityData COD = register(new EntityData(17, "cod", 0.5, 0.30000001192092896, EntityType.COD));

    public static final EntityData COMMAND_BLOCK_MINECART = register(new EntityData(18, "command_block_minecart", 0.9800000190734863, 0.699999988079071, EntityType.COMMAND_BLOCK_MINECART));

    public static final EntityData COW = register(new EntityData(19, "cow", 0.8999999761581421, 1.399999976158142, EntityType.COW));

    public static final EntityData CREEPER = register(new EntityData(20, "creeper", 0.6000000238418579, 1.7000000476837158, EntityType.CREEPER));

    public static final EntityData DOLPHIN = register(new EntityData(21, "dolphin", 0.8999999761581421, 0.6000000238418579, EntityType.DOLPHIN));

    public static final EntityData DONKEY = register(new EntityData(22, "donkey", 1.396484375, 1.5, EntityType.DONKEY));

    public static final EntityData DRAGON_FIREBALL = register(new EntityData(23, "dragon_fireball", 1.0, 1.0, EntityType.DRAGON_FIREBALL));

    public static final EntityData DROWNED = register(new EntityData(24, "drowned", 0.6000000238418579, 1.9500000476837158, EntityType.DROWNED));

    public static final EntityData EGG = register(new EntityData(25, "egg", 0.25, 0.25, EntityType.EGG));

    public static final EntityData ELDER_GUARDIAN = register(new EntityData(26, "elder_guardian", 1.997499942779541, 1.997499942779541, EntityType.ELDER_GUARDIAN));

    public static final EntityData END_CRYSTAL = register(new EntityData(27, "end_crystal", 2.0, 2.0, EntityType.END_CRYSTAL));

    public static final EntityData ENDER_DRAGON = register(new EntityData(28, "ender_dragon", 16.0, 8.0, EntityType.ENDER_DRAGON));

    public static final EntityData ENDER_PEARL = register(new EntityData(29, "ender_pearl", 0.25, 0.25, EntityType.ENDER_PEARL));

    public static final EntityData ENDERMAN = register(new EntityData(30, "enderman", 0.6000000238418579, 2.9000000953674316, EntityType.ENDERMAN));

    public static final EntityData ENDERMITE = register(new EntityData(31, "endermite", 0.4000000059604645, 0.30000001192092896, EntityType.ENDERMITE));

    public static final EntityData EVOKER = register(new EntityData(32, "evoker", 0.6000000238418579, 1.9500000476837158, EntityType.EVOKER));

    public static final EntityData EVOKER_FANGS = register(new EntityData(33, "evoker_fangs", 0.5, 0.800000011920929, EntityType.EVOKER_FANGS));

    public static final EntityData EXPERIENCE_BOTTLE = register(new EntityData(34, "experience_bottle", 0.25, 0.25, EntityType.EXPERIENCE_BOTTLE));

    public static final EntityData EXPERIENCE_ORB = register(new EntityData(35, "experience_orb", 0.5, 0.5, EntityType.EXPERIENCE_ORB));

    public static final EntityData EYE_OF_ENDER = register(new EntityData(36, "eye_of_ender", 0.25, 0.25, EntityType.EYE_OF_ENDER));

    public static final EntityData FALLING_BLOCK = register(new EntityData(37, "falling_block", 0.9800000190734863, 0.9800000190734863, EntityType.FALLING_BLOCK));

    public static final EntityData FIREWORK_ROCKET = register(new EntityData(38, "firework_rocket", 0.25, 0.25, EntityType.FIREWORK_ROCKET));

    public static final EntityData FOX = register(new EntityData(39, "fox", 0.6000000238418579, 0.699999988079071, EntityType.FOX));

    public static final EntityData FROG = register(new EntityData(40, "frog", 0.5, 0.5, EntityType.FROG));

    public static final EntityData FURNACE_MINECART = register(new EntityData(41, "furnace_minecart", 0.9800000190734863, 0.699999988079071, EntityType.FURNACE_MINECART));

    public static final EntityData GHAST = register(new EntityData(42, "ghast", 4.0, 4.0, EntityType.GHAST));

    public static final EntityData GIANT = register(new EntityData(43, "giant", 3.5999999046325684, 12.0, EntityType.GIANT));

    public static final EntityData GLOW_ITEM_FRAME = register(new EntityData(44, "glow_item_frame", 0.5, 0.5, EntityType.GLOW_ITEM_FRAME));

    public static final EntityData GLOW_SQUID = register(new EntityData(45, "glow_squid", 0.800000011920929, 0.800000011920929, EntityType.GLOW_SQUID));

    public static final EntityData GOAT = register(new EntityData(46, "goat", 0.8999999761581421, 1.2999999523162842, EntityType.GOAT));

    public static final EntityData GUARDIAN = register(new EntityData(47, "guardian", 0.8500000238418579, 0.8500000238418579, EntityType.GUARDIAN));

    public static final EntityData HOGLIN = register(new EntityData(48, "hoglin", 1.396484375, 1.399999976158142, EntityType.HOGLIN));

    public static final EntityData HOPPER_MINECART = register(new EntityData(49, "hopper_minecart", 0.9800000190734863, 0.699999988079071, EntityType.HOPPER_MINECART));

    public static final EntityData HORSE = register(new EntityData(50, "horse", 1.396484375, 1.600000023841858, EntityType.HORSE));

    public static final EntityData HUSK = register(new EntityData(51, "husk", 0.6000000238418579, 1.9500000476837158, EntityType.HUSK));

    public static final EntityData ILLUSIONER = register(new EntityData(52, "illusioner", 0.6000000238418579, 1.9500000476837158, EntityType.ILLUSIONER));

    public static final EntityData INTERACTION = register(new EntityData(53, "interaction", 0.0, 0.0, EntityType.INTERACTION));

    public static final EntityData IRON_GOLEM = register(new EntityData(54, "iron_golem", 1.399999976158142, 2.700000047683716, EntityType.IRON_GOLEM));

    public static final EntityData ITEM = register(new EntityData(55, "item", 0.25, 0.25, EntityType.ITEM));

    public static final EntityData ITEM_DISPLAY = register(new EntityData(56, "item_display", 0.0, 0.0, EntityType.ITEM_DISPLAY));

    public static final EntityData ITEM_FRAME = register(new EntityData(57, "item_frame", 0.5, 0.5, EntityType.ITEM_FRAME));

    public static final EntityData FIREBALL = register(new EntityData(58, "fireball", 1.0, 1.0, EntityType.FIREBALL));

    public static final EntityData LEASH_KNOT = register(new EntityData(59, "leash_knot", 0.375, 0.5, EntityType.LEASH_KNOT));

    public static final EntityData LIGHTNING_BOLT = register(new EntityData(60, "lightning_bolt", 0.0, 0.0, EntityType.LIGHTNING_BOLT));

    public static final EntityData LLAMA = register(new EntityData(61, "llama", 0.8999999761581421, 1.8700000047683716, EntityType.LLAMA));

    public static final EntityData LLAMA_SPIT = register(new EntityData(62, "llama_spit", 0.25, 0.25, EntityType.LLAMA_SPIT));

    public static final EntityData MAGMA_CUBE = register(new EntityData(63, "magma_cube", 2.0399999618530273, 2.0399999618530273, EntityType.MAGMA_CUBE));

    public static final EntityData MARKER = register(new EntityData(64, "marker", 0.0, 0.0, EntityType.MARKER));

    public static final EntityData MINECART = register(new EntityData(65, "minecart", 0.9800000190734863, 0.699999988079071, EntityType.MINECART));

    public static final EntityData MOOSHROOM = register(new EntityData(66, "mooshroom", 0.8999999761581421, 1.399999976158142, EntityType.MOOSHROOM));

    public static final EntityData MULE = register(new EntityData(67, "mule", 1.396484375, 1.600000023841858, EntityType.MULE));

    public static final EntityData OCELOT = register(new EntityData(68, "ocelot", 0.6000000238418579, 0.699999988079071, EntityType.OCELOT));

    public static final EntityData PAINTING = register(new EntityData(69, "painting", 0.5, 0.5, EntityType.PAINTING));

    public static final EntityData PANDA = register(new EntityData(70, "panda", 1.2999999523162842, 1.25, EntityType.PANDA));

    public static final EntityData PARROT = register(new EntityData(71, "parrot", 0.5, 0.8999999761581421, EntityType.PARROT));

    public static final EntityData PHANTOM = register(new EntityData(72, "phantom", 0.8999999761581421, 0.5, EntityType.PHANTOM));

    public static final EntityData PIG = register(new EntityData(73, "pig", 0.8999999761581421, 0.8999999761581421, EntityType.PIG));

    public static final EntityData PIGLIN = register(new EntityData(74, "piglin", 0.6000000238418579, 1.9500000476837158, EntityType.PIGLIN));

    public static final EntityData PIGLIN_BRUTE = register(new EntityData(75, "piglin_brute", 0.6000000238418579, 1.9500000476837158, EntityType.PIGLIN_BRUTE));

    public static final EntityData PILLAGER = register(new EntityData(76, "pillager", 0.6000000238418579, 1.9500000476837158, EntityType.PILLAGER));

    public static final EntityData POLAR_BEAR = register(new EntityData(77, "polar_bear", 1.399999976158142, 1.399999976158142, EntityType.POLAR_BEAR));

    public static final EntityData POTION = register(new EntityData(78, "potion", 0.25, 0.25, EntityType.POTION));

    public static final EntityData PUFFERFISH = register(new EntityData(79, "pufferfish", 0.699999988079071, 0.699999988079071, EntityType.PUFFERFISH));

    public static final EntityData RABBIT = register(new EntityData(80, "rabbit", 0.4000000059604645, 0.5, EntityType.RABBIT));

    public static final EntityData RAVAGER = register(new EntityData(81, "ravager", 1.9500000476837158, 2.200000047683716, EntityType.RAVAGER));

    public static final EntityData SALMON = register(new EntityData(82, "salmon", 0.699999988079071, 0.4000000059604645, EntityType.SALMON));

    public static final EntityData SHEEP = register(new EntityData(83, "sheep", 0.8999999761581421, 1.2999999523162842, EntityType.SHEEP));

    public static final EntityData SHULKER = register(new EntityData(84, "shulker", 1.0, 1.0, EntityType.SHULKER));

    public static final EntityData SHULKER_BULLET = register(new EntityData(85, "shulker_bullet", 0.3125, 0.3125, EntityType.SHULKER_BULLET));

    public static final EntityData SILVERFISH = register(new EntityData(86, "silverfish", 0.4000000059604645, 0.30000001192092896, EntityType.SILVERFISH));

    public static final EntityData SKELETON = register(new EntityData(87, "skeleton", 0.6000000238418579, 1.9900000095367432, EntityType.SKELETON));

    public static final EntityData SKELETON_HORSE = register(new EntityData(88, "skeleton_horse", 1.396484375, 1.600000023841858, EntityType.SKELETON_HORSE));

    public static final EntityData SLIME = register(new EntityData(89, "slime", 2.0399999618530273, 2.0399999618530273, EntityType.SLIME));

    public static final EntityData SMALL_FIREBALL = register(new EntityData(90, "small_fireball", 0.3125, 0.3125, EntityType.SMALL_FIREBALL));

    public static final EntityData SNIFFER = register(new EntityData(91, "sniffer", 1.899999976158142, 1.75, EntityType.SNIFFER));

    public static final EntityData SNOW_GOLEM = register(new EntityData(92, "snow_golem", 0.699999988079071, 1.899999976158142, EntityType.SNOW_GOLEM));

    public static final EntityData SNOWBALL = register(new EntityData(93, "snowball", 0.25, 0.25, EntityType.SNOWBALL));

    public static final EntityData SPAWNER_MINECART = register(new EntityData(94, "spawner_minecart", 0.9800000190734863, 0.699999988079071, EntityType.SPAWNER_MINECART));

    public static final EntityData SPECTRAL_ARROW = register(new EntityData(95, "spectral_arrow", 0.5, 0.5, EntityType.SPECTRAL_ARROW));

    public static final EntityData SPIDER = register(new EntityData(96, "spider", 1.399999976158142, 0.8999999761581421, EntityType.SPIDER));

    public static final EntityData SQUID = register(new EntityData(97, "squid", 0.800000011920929, 0.800000011920929, EntityType.SQUID));

    public static final EntityData STRAY = register(new EntityData(98, "stray", 0.6000000238418579, 1.9900000095367432, EntityType.STRAY));

    public static final EntityData STRIDER = register(new EntityData(99, "strider", 0.8999999761581421, 1.7000000476837158, EntityType.STRIDER));

    public static final EntityData TADPOLE = register(new EntityData(100, "tadpole", 0.4000000059604645, 0.30000001192092896, EntityType.TADPOLE));

    public static final EntityData TEXT_DISPLAY = register(new EntityData(101, "text_display", 0.0, 0.0, EntityType.TEXT_DISPLAY));

    public static final EntityData TNT = register(new EntityData(102, "tnt", 0.9800000190734863, 0.9800000190734863, EntityType.TNT));

    public static final EntityData TNT_MINECART = register(new EntityData(103, "tnt_minecart", 0.9800000190734863, 0.699999988079071, EntityType.TNT_MINECART));

    public static final EntityData TRADER_LLAMA = register(new EntityData(104, "trader_llama", 0.8999999761581421, 1.8700000047683716, EntityType.TRADER_LLAMA));

    public static final EntityData TRIDENT = register(new EntityData(105, "trident", 0.5, 0.5, EntityType.TRIDENT));

    public static final EntityData TROPICAL_FISH = register(new EntityData(106, "tropical_fish", 0.5, 0.4000000059604645, EntityType.TROPICAL_FISH));

    public static final EntityData TURTLE = register(new EntityData(107, "turtle", 1.2000000476837158, 0.4000000059604645, EntityType.TURTLE));

    public static final EntityData VEX = register(new EntityData(108, "vex", 0.4000000059604645, 0.800000011920929, EntityType.VEX));

    public static final EntityData VILLAGER = register(new EntityData(109, "villager", 0.6000000238418579, 1.9500000476837158, EntityType.VILLAGER));

    public static final EntityData VINDICATOR = register(new EntityData(110, "vindicator", 0.6000000238418579, 1.9500000476837158, EntityType.VINDICATOR));

    public static final EntityData WANDERING_TRADER = register(new EntityData(111, "wandering_trader", 0.6000000238418579, 1.9500000476837158, EntityType.WANDERING_TRADER));

    public static final EntityData WARDEN = register(new EntityData(112, "warden", 0.8999999761581421, 2.9000000953674316, EntityType.WARDEN));

    public static final EntityData WIND_CHARGE = register(new EntityData(113, "wind_charge", 0.3125, 0.3125, EntityType.WIND_CHARGE));

    public static final EntityData WITCH = register(new EntityData(114, "witch", 0.6000000238418579, 1.9500000476837158, EntityType.WITCH));

    public static final EntityData WITHER = register(new EntityData(115, "wither", 0.8999999761581421, 3.5, EntityType.WITHER));

    public static final EntityData WITHER_SKELETON = register(new EntityData(116, "wither_skeleton", 0.699999988079071, 2.4000000953674316, EntityType.WITHER_SKELETON));

    public static final EntityData WITHER_SKULL = register(new EntityData(117, "wither_skull", 0.3125, 0.3125, EntityType.WITHER_SKULL));

    public static final EntityData WOLF = register(new EntityData(118, "wolf", 0.6000000238418579, 0.8500000238418579, EntityType.WOLF));

    public static final EntityData ZOGLIN = register(new EntityData(119, "zoglin", 1.396484375, 1.399999976158142, EntityType.ZOGLIN));

    public static final EntityData ZOMBIE = register(new EntityData(120, "zombie", 0.6000000238418579, 1.9500000476837158, EntityType.ZOMBIE));

    public static final EntityData ZOMBIE_HORSE = register(new EntityData(121, "zombie_horse", 1.396484375, 1.600000023841858, EntityType.ZOMBIE_HORSE));

    public static final EntityData ZOMBIE_VILLAGER = register(new EntityData(122, "zombie_villager", 0.6000000238418579, 1.9500000476837158, EntityType.ZOMBIE_VILLAGER));

    public static final EntityData ZOMBIFIED_PIGLIN = register(new EntityData(123, "zombified_piglin", 0.6000000238418579, 1.9500000476837158, EntityType.ZOMBIFIED_PIGLIN));

    public static final EntityData PLAYER = register(new EntityData(124, "player", 0.6000000238418579, 1.7999999523162842, EntityType.PLAYER));

    public static final EntityData FISHING_BOBBER = register(new EntityData(125, "fishing_bobber", 0.25, 0.25, EntityType.FISHING_BOBBER));

    private static EntityData register(EntityData value) {
        REGISTRY.put(value.id(), value);
        return value;
    }
}
