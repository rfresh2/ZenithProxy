package com.zenith.feature.world.blockdata;

import com.zenith.util.Registry;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

public final class BlockRegistry {
    public static final Registry<Block> REGISTRY = new Registry<>(1060);

    public static final Block AIR = register(new Block(0, "air", false, 0, 0, 0, null));

    public static final Block STONE = register(new Block(1, "stone", true, 1, 1, 11, null));

    public static final Block GRANITE = register(new Block(2, "granite", true, 2, 2, 10, null));

    public static final Block POLISHED_GRANITE = register(new Block(3, "polished_granite", true, 3, 3, 10, null));

    public static final Block DIORITE = register(new Block(4, "diorite", true, 4, 4, 14, null));

    public static final Block POLISHED_DIORITE = register(new Block(5, "polished_diorite", true, 5, 5, 14, null));

    public static final Block ANDESITE = register(new Block(6, "andesite", true, 6, 6, 11, null));

    public static final Block POLISHED_ANDESITE = register(new Block(7, "polished_andesite", true, 7, 7, 11, null));

    public static final Block GRASS_BLOCK = register(new Block(8, "grass_block", true, 8, 9, 1, null));

    public static final Block DIRT = register(new Block(9, "dirt", true, 10, 10, 10, null));

    public static final Block COARSE_DIRT = register(new Block(10, "coarse_dirt", true, 11, 11, 10, null));

    public static final Block PODZOL = register(new Block(11, "podzol", true, 12, 13, 34, null));

    public static final Block COBBLESTONE = register(new Block(12, "cobblestone", true, 14, 14, 11, null));

    public static final Block OAK_PLANKS = register(new Block(13, "oak_planks", true, 15, 15, 13, null));

    public static final Block SPRUCE_PLANKS = register(new Block(14, "spruce_planks", true, 16, 16, 34, null));

    public static final Block BIRCH_PLANKS = register(new Block(15, "birch_planks", true, 17, 17, 2, null));

    public static final Block JUNGLE_PLANKS = register(new Block(16, "jungle_planks", true, 18, 18, 10, null));

    public static final Block ACACIA_PLANKS = register(new Block(17, "acacia_planks", true, 19, 19, 15, null));

    public static final Block CHERRY_PLANKS = register(new Block(18, "cherry_planks", true, 20, 20, 36, null));

    public static final Block DARK_OAK_PLANKS = register(new Block(19, "dark_oak_planks", true, 21, 21, 26, null));

    public static final Block MANGROVE_PLANKS = register(new Block(20, "mangrove_planks", true, 22, 22, 28, null));

    public static final Block BAMBOO_PLANKS = register(new Block(21, "bamboo_planks", true, 23, 23, 18, null));

    public static final Block BAMBOO_MOSAIC = register(new Block(22, "bamboo_mosaic", true, 24, 24, 18, null));

    public static final Block OAK_SAPLING = register(new Block(23, "oak_sapling", false, 25, 26, 7, null));

    public static final Block SPRUCE_SAPLING = register(new Block(24, "spruce_sapling", false, 27, 28, 7, null));

    public static final Block BIRCH_SAPLING = register(new Block(25, "birch_sapling", false, 29, 30, 7, null));

    public static final Block JUNGLE_SAPLING = register(new Block(26, "jungle_sapling", false, 31, 32, 7, null));

    public static final Block ACACIA_SAPLING = register(new Block(27, "acacia_sapling", false, 33, 34, 7, null));

    public static final Block CHERRY_SAPLING = register(new Block(28, "cherry_sapling", false, 35, 36, 20, null));

    public static final Block DARK_OAK_SAPLING = register(new Block(29, "dark_oak_sapling", false, 37, 38, 7, null));

    public static final Block MANGROVE_PROPAGULE = register(new Block(30, "mangrove_propagule", false, 39, 78, 7, null));

    public static final Block BEDROCK = register(new Block(31, "bedrock", true, 79, 79, 11, null));

    public static final Block WATER = register(new Block(32, "water", false, 80, 95, 12, null));

    public static final Block LAVA = register(new Block(33, "lava", false, 96, 111, 4, null));

    public static final Block SAND = register(new Block(34, "sand", true, 112, 112, 2, null));

    public static final Block SUSPICIOUS_SAND = register(new Block(35, "suspicious_sand", true, 113, 116, 2, BlockEntityType.BRUSHABLE_BLOCK));

    public static final Block RED_SAND = register(new Block(36, "red_sand", true, 117, 117, 15, null));

    public static final Block GRAVEL = register(new Block(37, "gravel", true, 118, 118, 11, null));

    public static final Block SUSPICIOUS_GRAVEL = register(new Block(38, "suspicious_gravel", true, 119, 122, 11, BlockEntityType.BRUSHABLE_BLOCK));

    public static final Block GOLD_ORE = register(new Block(39, "gold_ore", true, 123, 123, 11, null));

    public static final Block DEEPSLATE_GOLD_ORE = register(new Block(40, "deepslate_gold_ore", true, 124, 124, 59, null));

    public static final Block IRON_ORE = register(new Block(41, "iron_ore", true, 125, 125, 11, null));

    public static final Block DEEPSLATE_IRON_ORE = register(new Block(42, "deepslate_iron_ore", true, 126, 126, 59, null));

    public static final Block COAL_ORE = register(new Block(43, "coal_ore", true, 127, 127, 11, null));

    public static final Block DEEPSLATE_COAL_ORE = register(new Block(44, "deepslate_coal_ore", true, 128, 128, 59, null));

    public static final Block NETHER_GOLD_ORE = register(new Block(45, "nether_gold_ore", true, 129, 129, 35, null));

    public static final Block OAK_LOG = register(new Block(46, "oak_log", true, 130, 132, 13, null));

    public static final Block SPRUCE_LOG = register(new Block(47, "spruce_log", true, 133, 135, 34, null));

    public static final Block BIRCH_LOG = register(new Block(48, "birch_log", true, 136, 138, 2, null));

    public static final Block JUNGLE_LOG = register(new Block(49, "jungle_log", true, 139, 141, 10, null));

    public static final Block ACACIA_LOG = register(new Block(50, "acacia_log", true, 142, 144, 15, null));

    public static final Block CHERRY_LOG = register(new Block(51, "cherry_log", true, 145, 147, 36, null));

    public static final Block DARK_OAK_LOG = register(new Block(52, "dark_oak_log", true, 148, 150, 26, null));

    public static final Block MANGROVE_LOG = register(new Block(53, "mangrove_log", true, 151, 153, 28, null));

    public static final Block MANGROVE_ROOTS = register(new Block(54, "mangrove_roots", true, 154, 155, 34, null));

    public static final Block MUDDY_MANGROVE_ROOTS = register(new Block(55, "muddy_mangrove_roots", true, 156, 158, 34, null));

    public static final Block BAMBOO_BLOCK = register(new Block(56, "bamboo_block", true, 159, 161, 18, null));

    public static final Block STRIPPED_SPRUCE_LOG = register(new Block(57, "stripped_spruce_log", true, 162, 164, 34, null));

    public static final Block STRIPPED_BIRCH_LOG = register(new Block(58, "stripped_birch_log", true, 165, 167, 2, null));

    public static final Block STRIPPED_JUNGLE_LOG = register(new Block(59, "stripped_jungle_log", true, 168, 170, 10, null));

    public static final Block STRIPPED_ACACIA_LOG = register(new Block(60, "stripped_acacia_log", true, 171, 173, 15, null));

    public static final Block STRIPPED_CHERRY_LOG = register(new Block(61, "stripped_cherry_log", true, 174, 176, 36, null));

    public static final Block STRIPPED_DARK_OAK_LOG = register(new Block(62, "stripped_dark_oak_log", true, 177, 179, 26, null));

    public static final Block STRIPPED_OAK_LOG = register(new Block(63, "stripped_oak_log", true, 180, 182, 13, null));

    public static final Block STRIPPED_MANGROVE_LOG = register(new Block(64, "stripped_mangrove_log", true, 183, 185, 28, null));

    public static final Block STRIPPED_BAMBOO_BLOCK = register(new Block(65, "stripped_bamboo_block", true, 186, 188, 18, null));

    public static final Block OAK_WOOD = register(new Block(66, "oak_wood", true, 189, 191, 13, null));

    public static final Block SPRUCE_WOOD = register(new Block(67, "spruce_wood", true, 192, 194, 34, null));

    public static final Block BIRCH_WOOD = register(new Block(68, "birch_wood", true, 195, 197, 2, null));

    public static final Block JUNGLE_WOOD = register(new Block(69, "jungle_wood", true, 198, 200, 10, null));

    public static final Block ACACIA_WOOD = register(new Block(70, "acacia_wood", true, 201, 203, 21, null));

    public static final Block CHERRY_WOOD = register(new Block(71, "cherry_wood", true, 204, 206, 43, null));

    public static final Block DARK_OAK_WOOD = register(new Block(72, "dark_oak_wood", true, 207, 209, 26, null));

    public static final Block MANGROVE_WOOD = register(new Block(73, "mangrove_wood", true, 210, 212, 28, null));

    public static final Block STRIPPED_OAK_WOOD = register(new Block(74, "stripped_oak_wood", true, 213, 215, 13, null));

    public static final Block STRIPPED_SPRUCE_WOOD = register(new Block(75, "stripped_spruce_wood", true, 216, 218, 34, null));

    public static final Block STRIPPED_BIRCH_WOOD = register(new Block(76, "stripped_birch_wood", true, 219, 221, 2, null));

    public static final Block STRIPPED_JUNGLE_WOOD = register(new Block(77, "stripped_jungle_wood", true, 222, 224, 10, null));

    public static final Block STRIPPED_ACACIA_WOOD = register(new Block(78, "stripped_acacia_wood", true, 225, 227, 15, null));

    public static final Block STRIPPED_CHERRY_WOOD = register(new Block(79, "stripped_cherry_wood", true, 228, 230, 42, null));

    public static final Block STRIPPED_DARK_OAK_WOOD = register(new Block(80, "stripped_dark_oak_wood", true, 231, 233, 26, null));

    public static final Block STRIPPED_MANGROVE_WOOD = register(new Block(81, "stripped_mangrove_wood", true, 234, 236, 28, null));

    public static final Block OAK_LEAVES = register(new Block(82, "oak_leaves", true, 237, 264, 7, null));

    public static final Block SPRUCE_LEAVES = register(new Block(83, "spruce_leaves", true, 265, 292, 7, null));

    public static final Block BIRCH_LEAVES = register(new Block(84, "birch_leaves", true, 293, 320, 7, null));

    public static final Block JUNGLE_LEAVES = register(new Block(85, "jungle_leaves", true, 321, 348, 7, null));

    public static final Block ACACIA_LEAVES = register(new Block(86, "acacia_leaves", true, 349, 376, 7, null));

    public static final Block CHERRY_LEAVES = register(new Block(87, "cherry_leaves", true, 377, 404, 20, null));

    public static final Block DARK_OAK_LEAVES = register(new Block(88, "dark_oak_leaves", true, 405, 432, 7, null));

    public static final Block MANGROVE_LEAVES = register(new Block(89, "mangrove_leaves", true, 433, 460, 7, null));

    public static final Block AZALEA_LEAVES = register(new Block(90, "azalea_leaves", true, 461, 488, 7, null));

    public static final Block FLOWERING_AZALEA_LEAVES = register(new Block(91, "flowering_azalea_leaves", true, 489, 516, 7, null));

    public static final Block SPONGE = register(new Block(92, "sponge", true, 517, 517, 18, null));

    public static final Block WET_SPONGE = register(new Block(93, "wet_sponge", true, 518, 518, 18, null));

    public static final Block GLASS = register(new Block(94, "glass", true, 519, 519, 0, null));

    public static final Block LAPIS_ORE = register(new Block(95, "lapis_ore", true, 520, 520, 11, null));

    public static final Block DEEPSLATE_LAPIS_ORE = register(new Block(96, "deepslate_lapis_ore", true, 521, 521, 59, null));

    public static final Block LAPIS_BLOCK = register(new Block(97, "lapis_block", true, 522, 522, 32, null));

    public static final Block DISPENSER = register(new Block(98, "dispenser", true, 523, 534, 11, BlockEntityType.DISPENSER));

    public static final Block SANDSTONE = register(new Block(99, "sandstone", true, 535, 535, 2, null));

    public static final Block CHISELED_SANDSTONE = register(new Block(100, "chiseled_sandstone", true, 536, 536, 2, null));

    public static final Block CUT_SANDSTONE = register(new Block(101, "cut_sandstone", true, 537, 537, 2, null));

    public static final Block NOTE_BLOCK = register(new Block(102, "note_block", true, 538, 1687, 13, null));

    public static final Block WHITE_BED = register(new Block(103, "white_bed", true, 1688, 1703, 8, BlockEntityType.BED));

    public static final Block ORANGE_BED = register(new Block(104, "orange_bed", true, 1704, 1719, 15, BlockEntityType.BED));

    public static final Block MAGENTA_BED = register(new Block(105, "magenta_bed", true, 1720, 1735, 16, BlockEntityType.BED));

    public static final Block LIGHT_BLUE_BED = register(new Block(106, "light_blue_bed", true, 1736, 1751, 17, BlockEntityType.BED));

    public static final Block YELLOW_BED = register(new Block(107, "yellow_bed", true, 1752, 1767, 18, BlockEntityType.BED));

    public static final Block LIME_BED = register(new Block(108, "lime_bed", true, 1768, 1783, 19, BlockEntityType.BED));

    public static final Block PINK_BED = register(new Block(109, "pink_bed", true, 1784, 1799, 20, BlockEntityType.BED));

    public static final Block GRAY_BED = register(new Block(110, "gray_bed", true, 1800, 1815, 21, BlockEntityType.BED));

    public static final Block LIGHT_GRAY_BED = register(new Block(111, "light_gray_bed", true, 1816, 1831, 22, BlockEntityType.BED));

    public static final Block CYAN_BED = register(new Block(112, "cyan_bed", true, 1832, 1847, 23, BlockEntityType.BED));

    public static final Block PURPLE_BED = register(new Block(113, "purple_bed", true, 1848, 1863, 24, BlockEntityType.BED));

    public static final Block BLUE_BED = register(new Block(114, "blue_bed", true, 1864, 1879, 25, BlockEntityType.BED));

    public static final Block BROWN_BED = register(new Block(115, "brown_bed", true, 1880, 1895, 26, BlockEntityType.BED));

    public static final Block GREEN_BED = register(new Block(116, "green_bed", true, 1896, 1911, 27, BlockEntityType.BED));

    public static final Block RED_BED = register(new Block(117, "red_bed", true, 1912, 1927, 28, BlockEntityType.BED));

    public static final Block BLACK_BED = register(new Block(118, "black_bed", true, 1928, 1943, 29, BlockEntityType.BED));

    public static final Block POWERED_RAIL = register(new Block(119, "powered_rail", false, 1944, 1967, 0, null));

    public static final Block DETECTOR_RAIL = register(new Block(120, "detector_rail", false, 1968, 1991, 0, null));

    public static final Block STICKY_PISTON = register(new Block(121, "sticky_piston", true, 1992, 2003, 11, null));

    public static final Block COBWEB = register(new Block(122, "cobweb", false, 2004, 2004, 3, null));

    public static final Block SHORT_GRASS = register(new Block(123, "short_grass", false, 2005, 2005, 7, null));

    public static final Block FERN = register(new Block(124, "fern", false, 2006, 2006, 7, null));

    public static final Block DEAD_BUSH = register(new Block(125, "dead_bush", false, 2007, 2007, 13, null));

    public static final Block SEAGRASS = register(new Block(126, "seagrass", false, 2008, 2008, 12, null));

    public static final Block TALL_SEAGRASS = register(new Block(127, "tall_seagrass", false, 2009, 2010, 12, null));

    public static final Block PISTON = register(new Block(128, "piston", true, 2011, 2022, 11, null));

    public static final Block PISTON_HEAD = register(new Block(129, "piston_head", true, 2023, 2046, 11, null));

    public static final Block WHITE_WOOL = register(new Block(130, "white_wool", true, 2047, 2047, 8, null));

    public static final Block ORANGE_WOOL = register(new Block(131, "orange_wool", true, 2048, 2048, 15, null));

    public static final Block MAGENTA_WOOL = register(new Block(132, "magenta_wool", true, 2049, 2049, 16, null));

    public static final Block LIGHT_BLUE_WOOL = register(new Block(133, "light_blue_wool", true, 2050, 2050, 17, null));

    public static final Block YELLOW_WOOL = register(new Block(134, "yellow_wool", true, 2051, 2051, 18, null));

    public static final Block LIME_WOOL = register(new Block(135, "lime_wool", true, 2052, 2052, 19, null));

    public static final Block PINK_WOOL = register(new Block(136, "pink_wool", true, 2053, 2053, 20, null));

    public static final Block GRAY_WOOL = register(new Block(137, "gray_wool", true, 2054, 2054, 21, null));

    public static final Block LIGHT_GRAY_WOOL = register(new Block(138, "light_gray_wool", true, 2055, 2055, 22, null));

    public static final Block CYAN_WOOL = register(new Block(139, "cyan_wool", true, 2056, 2056, 23, null));

    public static final Block PURPLE_WOOL = register(new Block(140, "purple_wool", true, 2057, 2057, 24, null));

    public static final Block BLUE_WOOL = register(new Block(141, "blue_wool", true, 2058, 2058, 25, null));

    public static final Block BROWN_WOOL = register(new Block(142, "brown_wool", true, 2059, 2059, 26, null));

    public static final Block GREEN_WOOL = register(new Block(143, "green_wool", true, 2060, 2060, 27, null));

    public static final Block RED_WOOL = register(new Block(144, "red_wool", true, 2061, 2061, 28, null));

    public static final Block BLACK_WOOL = register(new Block(145, "black_wool", true, 2062, 2062, 29, null));

    public static final Block MOVING_PISTON = register(new Block(146, "moving_piston", false, 2063, 2074, 11, BlockEntityType.PISTON));

    public static final Block DANDELION = register(new Block(147, "dandelion", false, 2075, 2075, 7, null));

    public static final Block TORCHFLOWER = register(new Block(148, "torchflower", false, 2076, 2076, 7, null));

    public static final Block POPPY = register(new Block(149, "poppy", false, 2077, 2077, 7, null));

    public static final Block BLUE_ORCHID = register(new Block(150, "blue_orchid", false, 2078, 2078, 7, null));

    public static final Block ALLIUM = register(new Block(151, "allium", false, 2079, 2079, 7, null));

    public static final Block AZURE_BLUET = register(new Block(152, "azure_bluet", false, 2080, 2080, 7, null));

    public static final Block RED_TULIP = register(new Block(153, "red_tulip", false, 2081, 2081, 7, null));

    public static final Block ORANGE_TULIP = register(new Block(154, "orange_tulip", false, 2082, 2082, 7, null));

    public static final Block WHITE_TULIP = register(new Block(155, "white_tulip", false, 2083, 2083, 7, null));

    public static final Block PINK_TULIP = register(new Block(156, "pink_tulip", false, 2084, 2084, 7, null));

    public static final Block OXEYE_DAISY = register(new Block(157, "oxeye_daisy", false, 2085, 2085, 7, null));

    public static final Block CORNFLOWER = register(new Block(158, "cornflower", false, 2086, 2086, 7, null));

    public static final Block WITHER_ROSE = register(new Block(159, "wither_rose", false, 2087, 2087, 7, null));

    public static final Block LILY_OF_THE_VALLEY = register(new Block(160, "lily_of_the_valley", false, 2088, 2088, 7, null));

    public static final Block BROWN_MUSHROOM = register(new Block(161, "brown_mushroom", false, 2089, 2089, 26, null));

    public static final Block RED_MUSHROOM = register(new Block(162, "red_mushroom", false, 2090, 2090, 28, null));

    public static final Block GOLD_BLOCK = register(new Block(163, "gold_block", true, 2091, 2091, 30, null));

    public static final Block IRON_BLOCK = register(new Block(164, "iron_block", true, 2092, 2092, 6, null));

    public static final Block BRICKS = register(new Block(165, "bricks", true, 2093, 2093, 28, null));

    public static final Block TNT = register(new Block(166, "tnt", true, 2094, 2095, 4, null));

    public static final Block BOOKSHELF = register(new Block(167, "bookshelf", true, 2096, 2096, 13, null));

    public static final Block CHISELED_BOOKSHELF = register(new Block(168, "chiseled_bookshelf", true, 2097, 2352, 13, BlockEntityType.CHISELED_BOOKSHELF));

    public static final Block MOSSY_COBBLESTONE = register(new Block(169, "mossy_cobblestone", true, 2353, 2353, 11, null));

    public static final Block OBSIDIAN = register(new Block(170, "obsidian", true, 2354, 2354, 29, null));

    public static final Block TORCH = register(new Block(171, "torch", false, 2355, 2355, 0, null));

    public static final Block WALL_TORCH = register(new Block(172, "wall_torch", false, 2356, 2359, 0, null));

    public static final Block FIRE = register(new Block(173, "fire", false, 2360, 2871, 4, null));

    public static final Block SOUL_FIRE = register(new Block(174, "soul_fire", false, 2872, 2872, 17, null));

    public static final Block SPAWNER = register(new Block(175, "spawner", true, 2873, 2873, 11, BlockEntityType.MOB_SPAWNER));

    public static final Block OAK_STAIRS = register(new Block(176, "oak_stairs", true, 2874, 2953, 13, null));

    public static final Block CHEST = register(new Block(177, "chest", true, 2954, 2977, 13, BlockEntityType.CHEST));

    public static final Block REDSTONE_WIRE = register(new Block(178, "redstone_wire", false, 2978, 4273, 0, null));

    public static final Block DIAMOND_ORE = register(new Block(179, "diamond_ore", true, 4274, 4274, 11, null));

    public static final Block DEEPSLATE_DIAMOND_ORE = register(new Block(180, "deepslate_diamond_ore", true, 4275, 4275, 59, null));

    public static final Block DIAMOND_BLOCK = register(new Block(181, "diamond_block", true, 4276, 4276, 31, null));

    public static final Block CRAFTING_TABLE = register(new Block(182, "crafting_table", true, 4277, 4277, 13, null));

    public static final Block WHEAT = register(new Block(183, "wheat", false, 4278, 4285, 7, null));

    public static final Block FARMLAND = register(new Block(184, "farmland", true, 4286, 4293, 10, null));

    public static final Block FURNACE = register(new Block(185, "furnace", true, 4294, 4301, 11, BlockEntityType.FURNACE));

    public static final Block OAK_SIGN = register(new Block(186, "oak_sign", false, 4302, 4333, 13, BlockEntityType.SIGN));

    public static final Block SPRUCE_SIGN = register(new Block(187, "spruce_sign", false, 4334, 4365, 34, BlockEntityType.SIGN));

    public static final Block BIRCH_SIGN = register(new Block(188, "birch_sign", false, 4366, 4397, 2, BlockEntityType.SIGN));

    public static final Block ACACIA_SIGN = register(new Block(189, "acacia_sign", false, 4398, 4429, 15, BlockEntityType.SIGN));

    public static final Block CHERRY_SIGN = register(new Block(190, "cherry_sign", false, 4430, 4461, 36, BlockEntityType.SIGN));

    public static final Block JUNGLE_SIGN = register(new Block(191, "jungle_sign", false, 4462, 4493, 10, BlockEntityType.SIGN));

    public static final Block DARK_OAK_SIGN = register(new Block(192, "dark_oak_sign", false, 4494, 4525, 26, BlockEntityType.SIGN));

    public static final Block MANGROVE_SIGN = register(new Block(193, "mangrove_sign", false, 4526, 4557, 28, BlockEntityType.SIGN));

    public static final Block BAMBOO_SIGN = register(new Block(194, "bamboo_sign", false, 4558, 4589, 18, BlockEntityType.SIGN));

    public static final Block OAK_DOOR = register(new Block(195, "oak_door", true, 4590, 4653, 13, null));

    public static final Block LADDER = register(new Block(196, "ladder", true, 4654, 4661, 0, null));

    public static final Block RAIL = register(new Block(197, "rail", false, 4662, 4681, 0, null));

    public static final Block COBBLESTONE_STAIRS = register(new Block(198, "cobblestone_stairs", true, 4682, 4761, 11, null));

    public static final Block OAK_WALL_SIGN = register(new Block(199, "oak_wall_sign", false, 4762, 4769, 13, BlockEntityType.SIGN));

    public static final Block SPRUCE_WALL_SIGN = register(new Block(200, "spruce_wall_sign", false, 4770, 4777, 34, BlockEntityType.SIGN));

    public static final Block BIRCH_WALL_SIGN = register(new Block(201, "birch_wall_sign", false, 4778, 4785, 2, BlockEntityType.SIGN));

    public static final Block ACACIA_WALL_SIGN = register(new Block(202, "acacia_wall_sign", false, 4786, 4793, 15, BlockEntityType.SIGN));

    public static final Block CHERRY_WALL_SIGN = register(new Block(203, "cherry_wall_sign", false, 4794, 4801, 36, BlockEntityType.SIGN));

    public static final Block JUNGLE_WALL_SIGN = register(new Block(204, "jungle_wall_sign", false, 4802, 4809, 10, BlockEntityType.SIGN));

    public static final Block DARK_OAK_WALL_SIGN = register(new Block(205, "dark_oak_wall_sign", false, 4810, 4817, 26, BlockEntityType.SIGN));

    public static final Block MANGROVE_WALL_SIGN = register(new Block(206, "mangrove_wall_sign", false, 4818, 4825, 28, BlockEntityType.SIGN));

    public static final Block BAMBOO_WALL_SIGN = register(new Block(207, "bamboo_wall_sign", false, 4826, 4833, 18, BlockEntityType.SIGN));

    public static final Block OAK_HANGING_SIGN = register(new Block(208, "oak_hanging_sign", false, 4834, 4897, 13, BlockEntityType.HANGING_SIGN));

    public static final Block SPRUCE_HANGING_SIGN = register(new Block(209, "spruce_hanging_sign", false, 4898, 4961, 34, BlockEntityType.HANGING_SIGN));

    public static final Block BIRCH_HANGING_SIGN = register(new Block(210, "birch_hanging_sign", false, 4962, 5025, 2, BlockEntityType.HANGING_SIGN));

    public static final Block ACACIA_HANGING_SIGN = register(new Block(211, "acacia_hanging_sign", false, 5026, 5089, 15, BlockEntityType.HANGING_SIGN));

    public static final Block CHERRY_HANGING_SIGN = register(new Block(212, "cherry_hanging_sign", false, 5090, 5153, 42, BlockEntityType.HANGING_SIGN));

    public static final Block JUNGLE_HANGING_SIGN = register(new Block(213, "jungle_hanging_sign", false, 5154, 5217, 10, BlockEntityType.HANGING_SIGN));

    public static final Block DARK_OAK_HANGING_SIGN = register(new Block(214, "dark_oak_hanging_sign", false, 5218, 5281, 26, BlockEntityType.HANGING_SIGN));

    public static final Block CRIMSON_HANGING_SIGN = register(new Block(215, "crimson_hanging_sign", false, 5282, 5345, 53, BlockEntityType.HANGING_SIGN));

    public static final Block WARPED_HANGING_SIGN = register(new Block(216, "warped_hanging_sign", false, 5346, 5409, 56, BlockEntityType.HANGING_SIGN));

    public static final Block MANGROVE_HANGING_SIGN = register(new Block(217, "mangrove_hanging_sign", false, 5410, 5473, 28, BlockEntityType.HANGING_SIGN));

    public static final Block BAMBOO_HANGING_SIGN = register(new Block(218, "bamboo_hanging_sign", false, 5474, 5537, 18, BlockEntityType.HANGING_SIGN));

    public static final Block OAK_WALL_HANGING_SIGN = register(new Block(219, "oak_wall_hanging_sign", true, 5538, 5545, 13, BlockEntityType.HANGING_SIGN));

    public static final Block SPRUCE_WALL_HANGING_SIGN = register(new Block(220, "spruce_wall_hanging_sign", true, 5546, 5553, 13, BlockEntityType.HANGING_SIGN));

    public static final Block BIRCH_WALL_HANGING_SIGN = register(new Block(221, "birch_wall_hanging_sign", true, 5554, 5561, 2, BlockEntityType.HANGING_SIGN));

    public static final Block ACACIA_WALL_HANGING_SIGN = register(new Block(222, "acacia_wall_hanging_sign", true, 5562, 5569, 15, BlockEntityType.HANGING_SIGN));

    public static final Block CHERRY_WALL_HANGING_SIGN = register(new Block(223, "cherry_wall_hanging_sign", true, 5570, 5577, 42, BlockEntityType.HANGING_SIGN));

    public static final Block JUNGLE_WALL_HANGING_SIGN = register(new Block(224, "jungle_wall_hanging_sign", true, 5578, 5585, 10, BlockEntityType.HANGING_SIGN));

    public static final Block DARK_OAK_WALL_HANGING_SIGN = register(new Block(225, "dark_oak_wall_hanging_sign", true, 5586, 5593, 26, BlockEntityType.HANGING_SIGN));

    public static final Block MANGROVE_WALL_HANGING_SIGN = register(new Block(226, "mangrove_wall_hanging_sign", true, 5594, 5601, 28, BlockEntityType.HANGING_SIGN));

    public static final Block CRIMSON_WALL_HANGING_SIGN = register(new Block(227, "crimson_wall_hanging_sign", true, 5602, 5609, 53, BlockEntityType.HANGING_SIGN));

    public static final Block WARPED_WALL_HANGING_SIGN = register(new Block(228, "warped_wall_hanging_sign", true, 5610, 5617, 56, BlockEntityType.HANGING_SIGN));

    public static final Block BAMBOO_WALL_HANGING_SIGN = register(new Block(229, "bamboo_wall_hanging_sign", true, 5618, 5625, 18, BlockEntityType.HANGING_SIGN));

    public static final Block LEVER = register(new Block(230, "lever", false, 5626, 5649, 0, null));

    public static final Block STONE_PRESSURE_PLATE = register(new Block(231, "stone_pressure_plate", false, 5650, 5651, 11, null));

    public static final Block IRON_DOOR = register(new Block(232, "iron_door", true, 5652, 5715, 6, null));

    public static final Block OAK_PRESSURE_PLATE = register(new Block(233, "oak_pressure_plate", false, 5716, 5717, 13, null));

    public static final Block SPRUCE_PRESSURE_PLATE = register(new Block(234, "spruce_pressure_plate", false, 5718, 5719, 34, null));

    public static final Block BIRCH_PRESSURE_PLATE = register(new Block(235, "birch_pressure_plate", false, 5720, 5721, 2, null));

    public static final Block JUNGLE_PRESSURE_PLATE = register(new Block(236, "jungle_pressure_plate", false, 5722, 5723, 10, null));

    public static final Block ACACIA_PRESSURE_PLATE = register(new Block(237, "acacia_pressure_plate", false, 5724, 5725, 15, null));

    public static final Block CHERRY_PRESSURE_PLATE = register(new Block(238, "cherry_pressure_plate", false, 5726, 5727, 36, null));

    public static final Block DARK_OAK_PRESSURE_PLATE = register(new Block(239, "dark_oak_pressure_plate", false, 5728, 5729, 26, null));

    public static final Block MANGROVE_PRESSURE_PLATE = register(new Block(240, "mangrove_pressure_plate", false, 5730, 5731, 28, null));

    public static final Block BAMBOO_PRESSURE_PLATE = register(new Block(241, "bamboo_pressure_plate", false, 5732, 5733, 18, null));

    public static final Block REDSTONE_ORE = register(new Block(242, "redstone_ore", true, 5734, 5735, 11, null));

    public static final Block DEEPSLATE_REDSTONE_ORE = register(new Block(243, "deepslate_redstone_ore", true, 5736, 5737, 59, null));

    public static final Block REDSTONE_TORCH = register(new Block(244, "redstone_torch", false, 5738, 5739, 0, null));

    public static final Block REDSTONE_WALL_TORCH = register(new Block(245, "redstone_wall_torch", false, 5740, 5747, 0, null));

    public static final Block STONE_BUTTON = register(new Block(246, "stone_button", false, 5748, 5771, 0, null));

    public static final Block SNOW = register(new Block(247, "snow", false, 5772, 5779, 8, null));

    public static final Block ICE = register(new Block(248, "ice", true, 5780, 5780, 5, null));

    public static final Block SNOW_BLOCK = register(new Block(249, "snow_block", true, 5781, 5781, 8, null));

    public static final Block CACTUS = register(new Block(250, "cactus", true, 5782, 5797, 7, null));

    public static final Block CLAY = register(new Block(251, "clay", true, 5798, 5798, 9, null));

    public static final Block SUGAR_CANE = register(new Block(252, "sugar_cane", false, 5799, 5814, 7, null));

    public static final Block JUKEBOX = register(new Block(253, "jukebox", true, 5815, 5816, 10, BlockEntityType.JUKEBOX));

    public static final Block OAK_FENCE = register(new Block(254, "oak_fence", true, 5817, 5848, 13, null));

    public static final Block NETHERRACK = register(new Block(255, "netherrack", true, 5849, 5849, 35, null));

    public static final Block SOUL_SAND = register(new Block(256, "soul_sand", true, 5850, 5850, 26, null));

    public static final Block SOUL_SOIL = register(new Block(257, "soul_soil", true, 5851, 5851, 26, null));

    public static final Block BASALT = register(new Block(258, "basalt", true, 5852, 5854, 29, null));

    public static final Block POLISHED_BASALT = register(new Block(259, "polished_basalt", true, 5855, 5857, 29, null));

    public static final Block SOUL_TORCH = register(new Block(260, "soul_torch", false, 5858, 5858, 0, null));

    public static final Block SOUL_WALL_TORCH = register(new Block(261, "soul_wall_torch", false, 5859, 5862, 0, null));

    public static final Block GLOWSTONE = register(new Block(262, "glowstone", true, 5863, 5863, 2, null));

    public static final Block NETHER_PORTAL = register(new Block(263, "nether_portal", false, 5864, 5865, 0, null));

    public static final Block CARVED_PUMPKIN = register(new Block(264, "carved_pumpkin", true, 5866, 5869, 15, null));

    public static final Block JACK_O_LANTERN = register(new Block(265, "jack_o_lantern", true, 5870, 5873, 15, null));

    public static final Block CAKE = register(new Block(266, "cake", true, 5874, 5880, 0, null));

    public static final Block REPEATER = register(new Block(267, "repeater", true, 5881, 5944, 0, null));

    public static final Block WHITE_STAINED_GLASS = register(new Block(268, "white_stained_glass", true, 5945, 5945, 8, null));

    public static final Block ORANGE_STAINED_GLASS = register(new Block(269, "orange_stained_glass", true, 5946, 5946, 15, null));

    public static final Block MAGENTA_STAINED_GLASS = register(new Block(270, "magenta_stained_glass", true, 5947, 5947, 16, null));

    public static final Block LIGHT_BLUE_STAINED_GLASS = register(new Block(271, "light_blue_stained_glass", true, 5948, 5948, 17, null));

    public static final Block YELLOW_STAINED_GLASS = register(new Block(272, "yellow_stained_glass", true, 5949, 5949, 18, null));

    public static final Block LIME_STAINED_GLASS = register(new Block(273, "lime_stained_glass", true, 5950, 5950, 19, null));

    public static final Block PINK_STAINED_GLASS = register(new Block(274, "pink_stained_glass", true, 5951, 5951, 20, null));

    public static final Block GRAY_STAINED_GLASS = register(new Block(275, "gray_stained_glass", true, 5952, 5952, 21, null));

    public static final Block LIGHT_GRAY_STAINED_GLASS = register(new Block(276, "light_gray_stained_glass", true, 5953, 5953, 22, null));

    public static final Block CYAN_STAINED_GLASS = register(new Block(277, "cyan_stained_glass", true, 5954, 5954, 23, null));

    public static final Block PURPLE_STAINED_GLASS = register(new Block(278, "purple_stained_glass", true, 5955, 5955, 24, null));

    public static final Block BLUE_STAINED_GLASS = register(new Block(279, "blue_stained_glass", true, 5956, 5956, 25, null));

    public static final Block BROWN_STAINED_GLASS = register(new Block(280, "brown_stained_glass", true, 5957, 5957, 26, null));

    public static final Block GREEN_STAINED_GLASS = register(new Block(281, "green_stained_glass", true, 5958, 5958, 27, null));

    public static final Block RED_STAINED_GLASS = register(new Block(282, "red_stained_glass", true, 5959, 5959, 28, null));

    public static final Block BLACK_STAINED_GLASS = register(new Block(283, "black_stained_glass", true, 5960, 5960, 29, null));

    public static final Block OAK_TRAPDOOR = register(new Block(284, "oak_trapdoor", true, 5961, 6024, 13, null));

    public static final Block SPRUCE_TRAPDOOR = register(new Block(285, "spruce_trapdoor", true, 6025, 6088, 34, null));

    public static final Block BIRCH_TRAPDOOR = register(new Block(286, "birch_trapdoor", true, 6089, 6152, 2, null));

    public static final Block JUNGLE_TRAPDOOR = register(new Block(287, "jungle_trapdoor", true, 6153, 6216, 10, null));

    public static final Block ACACIA_TRAPDOOR = register(new Block(288, "acacia_trapdoor", true, 6217, 6280, 15, null));

    public static final Block CHERRY_TRAPDOOR = register(new Block(289, "cherry_trapdoor", true, 6281, 6344, 36, null));

    public static final Block DARK_OAK_TRAPDOOR = register(new Block(290, "dark_oak_trapdoor", true, 6345, 6408, 26, null));

    public static final Block MANGROVE_TRAPDOOR = register(new Block(291, "mangrove_trapdoor", true, 6409, 6472, 28, null));

    public static final Block BAMBOO_TRAPDOOR = register(new Block(292, "bamboo_trapdoor", true, 6473, 6536, 18, null));

    public static final Block STONE_BRICKS = register(new Block(293, "stone_bricks", true, 6537, 6537, 11, null));

    public static final Block MOSSY_STONE_BRICKS = register(new Block(294, "mossy_stone_bricks", true, 6538, 6538, 11, null));

    public static final Block CRACKED_STONE_BRICKS = register(new Block(295, "cracked_stone_bricks", true, 6539, 6539, 11, null));

    public static final Block CHISELED_STONE_BRICKS = register(new Block(296, "chiseled_stone_bricks", true, 6540, 6540, 11, null));

    public static final Block PACKED_MUD = register(new Block(297, "packed_mud", true, 6541, 6541, 10, null));

    public static final Block MUD_BRICKS = register(new Block(298, "mud_bricks", true, 6542, 6542, 44, null));

    public static final Block INFESTED_STONE = register(new Block(299, "infested_stone", true, 6543, 6543, 9, null));

    public static final Block INFESTED_COBBLESTONE = register(new Block(300, "infested_cobblestone", true, 6544, 6544, 9, null));

    public static final Block INFESTED_STONE_BRICKS = register(new Block(301, "infested_stone_bricks", true, 6545, 6545, 9, null));

    public static final Block INFESTED_MOSSY_STONE_BRICKS = register(new Block(302, "infested_mossy_stone_bricks", true, 6546, 6546, 9, null));

    public static final Block INFESTED_CRACKED_STONE_BRICKS = register(new Block(303, "infested_cracked_stone_bricks", true, 6547, 6547, 9, null));

    public static final Block INFESTED_CHISELED_STONE_BRICKS = register(new Block(304, "infested_chiseled_stone_bricks", true, 6548, 6548, 9, null));

    public static final Block BROWN_MUSHROOM_BLOCK = register(new Block(305, "brown_mushroom_block", true, 6549, 6612, 10, null));

    public static final Block RED_MUSHROOM_BLOCK = register(new Block(306, "red_mushroom_block", true, 6613, 6676, 28, null));

    public static final Block MUSHROOM_STEM = register(new Block(307, "mushroom_stem", true, 6677, 6740, 3, null));

    public static final Block IRON_BARS = register(new Block(308, "iron_bars", true, 6741, 6772, 0, null));

    public static final Block CHAIN = register(new Block(309, "chain", true, 6773, 6778, 0, null));

    public static final Block GLASS_PANE = register(new Block(310, "glass_pane", true, 6779, 6810, 0, null));

    public static final Block PUMPKIN = register(new Block(311, "pumpkin", true, 6811, 6811, 15, null));

    public static final Block MELON = register(new Block(312, "melon", true, 6812, 6812, 19, null));

    public static final Block ATTACHED_PUMPKIN_STEM = register(new Block(313, "attached_pumpkin_stem", false, 6813, 6816, 7, null));

    public static final Block ATTACHED_MELON_STEM = register(new Block(314, "attached_melon_stem", false, 6817, 6820, 7, null));

    public static final Block PUMPKIN_STEM = register(new Block(315, "pumpkin_stem", false, 6821, 6828, 7, null));

    public static final Block MELON_STEM = register(new Block(316, "melon_stem", false, 6829, 6836, 7, null));

    public static final Block VINE = register(new Block(317, "vine", false, 6837, 6868, 7, null));

    public static final Block GLOW_LICHEN = register(new Block(318, "glow_lichen", false, 6869, 6996, 61, null));

    public static final Block OAK_FENCE_GATE = register(new Block(319, "oak_fence_gate", true, 6997, 7028, 13, null));

    public static final Block BRICK_STAIRS = register(new Block(320, "brick_stairs", true, 7029, 7108, 28, null));

    public static final Block STONE_BRICK_STAIRS = register(new Block(321, "stone_brick_stairs", true, 7109, 7188, 11, null));

    public static final Block MUD_BRICK_STAIRS = register(new Block(322, "mud_brick_stairs", true, 7189, 7268, 44, null));

    public static final Block MYCELIUM = register(new Block(323, "mycelium", true, 7269, 7270, 24, null));

    public static final Block LILY_PAD = register(new Block(324, "lily_pad", true, 7271, 7271, 7, null));

    public static final Block NETHER_BRICKS = register(new Block(325, "nether_bricks", true, 7272, 7272, 35, null));

    public static final Block NETHER_BRICK_FENCE = register(new Block(326, "nether_brick_fence", true, 7273, 7304, 35, null));

    public static final Block NETHER_BRICK_STAIRS = register(new Block(327, "nether_brick_stairs", true, 7305, 7384, 35, null));

    public static final Block NETHER_WART = register(new Block(328, "nether_wart", false, 7385, 7388, 28, null));

    public static final Block ENCHANTING_TABLE = register(new Block(329, "enchanting_table", true, 7389, 7389, 28, BlockEntityType.ENCHANTING_TABLE));

    public static final Block BREWING_STAND = register(new Block(330, "brewing_stand", true, 7390, 7397, 6, BlockEntityType.BREWING_STAND));

    public static final Block CAULDRON = register(new Block(331, "cauldron", true, 7398, 7398, 11, null));

    public static final Block WATER_CAULDRON = register(new Block(332, "water_cauldron", true, 7399, 7401, 11, null));

    public static final Block LAVA_CAULDRON = register(new Block(333, "lava_cauldron", true, 7402, 7402, 11, null));

    public static final Block POWDER_SNOW_CAULDRON = register(new Block(334, "powder_snow_cauldron", true, 7403, 7405, 11, null));

    public static final Block END_PORTAL = register(new Block(335, "end_portal", false, 7406, 7406, 29, BlockEntityType.END_PORTAL));

    public static final Block END_PORTAL_FRAME = register(new Block(336, "end_portal_frame", true, 7407, 7414, 27, null));

    public static final Block END_STONE = register(new Block(337, "end_stone", true, 7415, 7415, 2, null));

    public static final Block DRAGON_EGG = register(new Block(338, "dragon_egg", true, 7416, 7416, 29, null));

    public static final Block REDSTONE_LAMP = register(new Block(339, "redstone_lamp", true, 7417, 7418, 0, null));

    public static final Block COCOA = register(new Block(340, "cocoa", true, 7419, 7430, 7, null));

    public static final Block SANDSTONE_STAIRS = register(new Block(341, "sandstone_stairs", true, 7431, 7510, 2, null));

    public static final Block EMERALD_ORE = register(new Block(342, "emerald_ore", true, 7511, 7511, 11, null));

    public static final Block DEEPSLATE_EMERALD_ORE = register(new Block(343, "deepslate_emerald_ore", true, 7512, 7512, 59, null));

    public static final Block ENDER_CHEST = register(new Block(344, "ender_chest", true, 7513, 7520, 11, BlockEntityType.ENDER_CHEST));

    public static final Block TRIPWIRE_HOOK = register(new Block(345, "tripwire_hook", false, 7521, 7536, 0, null));

    public static final Block TRIPWIRE = register(new Block(346, "tripwire", false, 7537, 7664, 0, null));

    public static final Block EMERALD_BLOCK = register(new Block(347, "emerald_block", true, 7665, 7665, 33, null));

    public static final Block SPRUCE_STAIRS = register(new Block(348, "spruce_stairs", true, 7666, 7745, 34, null));

    public static final Block BIRCH_STAIRS = register(new Block(349, "birch_stairs", true, 7746, 7825, 2, null));

    public static final Block JUNGLE_STAIRS = register(new Block(350, "jungle_stairs", true, 7826, 7905, 10, null));

    public static final Block COMMAND_BLOCK = register(new Block(351, "command_block", true, 7906, 7917, 26, BlockEntityType.COMMAND_BLOCK));

    public static final Block BEACON = register(new Block(352, "beacon", true, 7918, 7918, 31, BlockEntityType.BEACON));

    public static final Block COBBLESTONE_WALL = register(new Block(353, "cobblestone_wall", true, 7919, 8242, 11, null));

    public static final Block MOSSY_COBBLESTONE_WALL = register(new Block(354, "mossy_cobblestone_wall", true, 8243, 8566, 11, null));

    public static final Block FLOWER_POT = register(new Block(355, "flower_pot", true, 8567, 8567, 0, null));

    public static final Block POTTED_TORCHFLOWER = register(new Block(356, "potted_torchflower", true, 8568, 8568, 0, null));

    public static final Block POTTED_OAK_SAPLING = register(new Block(357, "potted_oak_sapling", true, 8569, 8569, 0, null));

    public static final Block POTTED_SPRUCE_SAPLING = register(new Block(358, "potted_spruce_sapling", true, 8570, 8570, 0, null));

    public static final Block POTTED_BIRCH_SAPLING = register(new Block(359, "potted_birch_sapling", true, 8571, 8571, 0, null));

    public static final Block POTTED_JUNGLE_SAPLING = register(new Block(360, "potted_jungle_sapling", true, 8572, 8572, 0, null));

    public static final Block POTTED_ACACIA_SAPLING = register(new Block(361, "potted_acacia_sapling", true, 8573, 8573, 0, null));

    public static final Block POTTED_CHERRY_SAPLING = register(new Block(362, "potted_cherry_sapling", true, 8574, 8574, 0, null));

    public static final Block POTTED_DARK_OAK_SAPLING = register(new Block(363, "potted_dark_oak_sapling", true, 8575, 8575, 0, null));

    public static final Block POTTED_MANGROVE_PROPAGULE = register(new Block(364, "potted_mangrove_propagule", true, 8576, 8576, 0, null));

    public static final Block POTTED_FERN = register(new Block(365, "potted_fern", true, 8577, 8577, 0, null));

    public static final Block POTTED_DANDELION = register(new Block(366, "potted_dandelion", true, 8578, 8578, 0, null));

    public static final Block POTTED_POPPY = register(new Block(367, "potted_poppy", true, 8579, 8579, 0, null));

    public static final Block POTTED_BLUE_ORCHID = register(new Block(368, "potted_blue_orchid", true, 8580, 8580, 0, null));

    public static final Block POTTED_ALLIUM = register(new Block(369, "potted_allium", true, 8581, 8581, 0, null));

    public static final Block POTTED_AZURE_BLUET = register(new Block(370, "potted_azure_bluet", true, 8582, 8582, 0, null));

    public static final Block POTTED_RED_TULIP = register(new Block(371, "potted_red_tulip", true, 8583, 8583, 0, null));

    public static final Block POTTED_ORANGE_TULIP = register(new Block(372, "potted_orange_tulip", true, 8584, 8584, 0, null));

    public static final Block POTTED_WHITE_TULIP = register(new Block(373, "potted_white_tulip", true, 8585, 8585, 0, null));

    public static final Block POTTED_PINK_TULIP = register(new Block(374, "potted_pink_tulip", true, 8586, 8586, 0, null));

    public static final Block POTTED_OXEYE_DAISY = register(new Block(375, "potted_oxeye_daisy", true, 8587, 8587, 0, null));

    public static final Block POTTED_CORNFLOWER = register(new Block(376, "potted_cornflower", true, 8588, 8588, 0, null));

    public static final Block POTTED_LILY_OF_THE_VALLEY = register(new Block(377, "potted_lily_of_the_valley", true, 8589, 8589, 0, null));

    public static final Block POTTED_WITHER_ROSE = register(new Block(378, "potted_wither_rose", true, 8590, 8590, 0, null));

    public static final Block POTTED_RED_MUSHROOM = register(new Block(379, "potted_red_mushroom", true, 8591, 8591, 0, null));

    public static final Block POTTED_BROWN_MUSHROOM = register(new Block(380, "potted_brown_mushroom", true, 8592, 8592, 0, null));

    public static final Block POTTED_DEAD_BUSH = register(new Block(381, "potted_dead_bush", true, 8593, 8593, 0, null));

    public static final Block POTTED_CACTUS = register(new Block(382, "potted_cactus", true, 8594, 8594, 0, null));

    public static final Block CARROTS = register(new Block(383, "carrots", false, 8595, 8602, 7, null));

    public static final Block POTATOES = register(new Block(384, "potatoes", false, 8603, 8610, 7, null));

    public static final Block OAK_BUTTON = register(new Block(385, "oak_button", false, 8611, 8634, 0, null));

    public static final Block SPRUCE_BUTTON = register(new Block(386, "spruce_button", false, 8635, 8658, 0, null));

    public static final Block BIRCH_BUTTON = register(new Block(387, "birch_button", false, 8659, 8682, 0, null));

    public static final Block JUNGLE_BUTTON = register(new Block(388, "jungle_button", false, 8683, 8706, 0, null));

    public static final Block ACACIA_BUTTON = register(new Block(389, "acacia_button", false, 8707, 8730, 0, null));

    public static final Block CHERRY_BUTTON = register(new Block(390, "cherry_button", false, 8731, 8754, 0, null));

    public static final Block DARK_OAK_BUTTON = register(new Block(391, "dark_oak_button", false, 8755, 8778, 0, null));

    public static final Block MANGROVE_BUTTON = register(new Block(392, "mangrove_button", false, 8779, 8802, 0, null));

    public static final Block BAMBOO_BUTTON = register(new Block(393, "bamboo_button", false, 8803, 8826, 0, null));

    public static final Block SKELETON_SKULL = register(new Block(394, "skeleton_skull", true, 8827, 8858, 0, BlockEntityType.SKULL));

    public static final Block SKELETON_WALL_SKULL = register(new Block(395, "skeleton_wall_skull", true, 8859, 8866, 0, BlockEntityType.SKULL));

    public static final Block WITHER_SKELETON_SKULL = register(new Block(396, "wither_skeleton_skull", true, 8867, 8898, 0, BlockEntityType.SKULL));

    public static final Block WITHER_SKELETON_WALL_SKULL = register(new Block(397, "wither_skeleton_wall_skull", true, 8899, 8906, 0, BlockEntityType.SKULL));

    public static final Block ZOMBIE_HEAD = register(new Block(398, "zombie_head", true, 8907, 8938, 0, BlockEntityType.SKULL));

    public static final Block ZOMBIE_WALL_HEAD = register(new Block(399, "zombie_wall_head", true, 8939, 8946, 0, BlockEntityType.SKULL));

    public static final Block PLAYER_HEAD = register(new Block(400, "player_head", true, 8947, 8978, 0, BlockEntityType.SKULL));

    public static final Block PLAYER_WALL_HEAD = register(new Block(401, "player_wall_head", true, 8979, 8986, 0, BlockEntityType.SKULL));

    public static final Block CREEPER_HEAD = register(new Block(402, "creeper_head", true, 8987, 9018, 0, BlockEntityType.SKULL));

    public static final Block CREEPER_WALL_HEAD = register(new Block(403, "creeper_wall_head", true, 9019, 9026, 0, BlockEntityType.SKULL));

    public static final Block DRAGON_HEAD = register(new Block(404, "dragon_head", true, 9027, 9058, 0, BlockEntityType.SKULL));

    public static final Block DRAGON_WALL_HEAD = register(new Block(405, "dragon_wall_head", true, 9059, 9066, 0, BlockEntityType.SKULL));

    public static final Block PIGLIN_HEAD = register(new Block(406, "piglin_head", true, 9067, 9098, 0, BlockEntityType.SKULL));

    public static final Block PIGLIN_WALL_HEAD = register(new Block(407, "piglin_wall_head", true, 9099, 9106, 0, BlockEntityType.SKULL));

    public static final Block ANVIL = register(new Block(408, "anvil", true, 9107, 9110, 6, null));

    public static final Block CHIPPED_ANVIL = register(new Block(409, "chipped_anvil", true, 9111, 9114, 6, null));

    public static final Block DAMAGED_ANVIL = register(new Block(410, "damaged_anvil", true, 9115, 9118, 6, null));

    public static final Block TRAPPED_CHEST = register(new Block(411, "trapped_chest", true, 9119, 9142, 13, BlockEntityType.TRAPPED_CHEST));

    public static final Block LIGHT_WEIGHTED_PRESSURE_PLATE = register(new Block(412, "light_weighted_pressure_plate", false, 9143, 9158, 30, null));

    public static final Block HEAVY_WEIGHTED_PRESSURE_PLATE = register(new Block(413, "heavy_weighted_pressure_plate", false, 9159, 9174, 6, null));

    public static final Block COMPARATOR = register(new Block(414, "comparator", true, 9175, 9190, 0, BlockEntityType.COMPARATOR));

    public static final Block DAYLIGHT_DETECTOR = register(new Block(415, "daylight_detector", true, 9191, 9222, 13, BlockEntityType.DAYLIGHT_DETECTOR));

    public static final Block REDSTONE_BLOCK = register(new Block(416, "redstone_block", true, 9223, 9223, 4, null));

    public static final Block NETHER_QUARTZ_ORE = register(new Block(417, "nether_quartz_ore", true, 9224, 9224, 35, null));

    public static final Block HOPPER = register(new Block(418, "hopper", true, 9225, 9234, 11, BlockEntityType.HOPPER));

    public static final Block QUARTZ_BLOCK = register(new Block(419, "quartz_block", true, 9235, 9235, 14, null));

    public static final Block CHISELED_QUARTZ_BLOCK = register(new Block(420, "chiseled_quartz_block", true, 9236, 9236, 14, null));

    public static final Block QUARTZ_PILLAR = register(new Block(421, "quartz_pillar", true, 9237, 9239, 14, null));

    public static final Block QUARTZ_STAIRS = register(new Block(422, "quartz_stairs", true, 9240, 9319, 14, null));

    public static final Block ACTIVATOR_RAIL = register(new Block(423, "activator_rail", false, 9320, 9343, 0, null));

    public static final Block DROPPER = register(new Block(424, "dropper", true, 9344, 9355, 11, BlockEntityType.DROPPER));

    public static final Block WHITE_TERRACOTTA = register(new Block(425, "white_terracotta", true, 9356, 9356, 36, null));

    public static final Block ORANGE_TERRACOTTA = register(new Block(426, "orange_terracotta", true, 9357, 9357, 37, null));

    public static final Block MAGENTA_TERRACOTTA = register(new Block(427, "magenta_terracotta", true, 9358, 9358, 38, null));

    public static final Block LIGHT_BLUE_TERRACOTTA = register(new Block(428, "light_blue_terracotta", true, 9359, 9359, 39, null));

    public static final Block YELLOW_TERRACOTTA = register(new Block(429, "yellow_terracotta", true, 9360, 9360, 40, null));

    public static final Block LIME_TERRACOTTA = register(new Block(430, "lime_terracotta", true, 9361, 9361, 41, null));

    public static final Block PINK_TERRACOTTA = register(new Block(431, "pink_terracotta", true, 9362, 9362, 42, null));

    public static final Block GRAY_TERRACOTTA = register(new Block(432, "gray_terracotta", true, 9363, 9363, 43, null));

    public static final Block LIGHT_GRAY_TERRACOTTA = register(new Block(433, "light_gray_terracotta", true, 9364, 9364, 44, null));

    public static final Block CYAN_TERRACOTTA = register(new Block(434, "cyan_terracotta", true, 9365, 9365, 45, null));

    public static final Block PURPLE_TERRACOTTA = register(new Block(435, "purple_terracotta", true, 9366, 9366, 46, null));

    public static final Block BLUE_TERRACOTTA = register(new Block(436, "blue_terracotta", true, 9367, 9367, 47, null));

    public static final Block BROWN_TERRACOTTA = register(new Block(437, "brown_terracotta", true, 9368, 9368, 48, null));

    public static final Block GREEN_TERRACOTTA = register(new Block(438, "green_terracotta", true, 9369, 9369, 49, null));

    public static final Block RED_TERRACOTTA = register(new Block(439, "red_terracotta", true, 9370, 9370, 50, null));

    public static final Block BLACK_TERRACOTTA = register(new Block(440, "black_terracotta", true, 9371, 9371, 51, null));

    public static final Block WHITE_STAINED_GLASS_PANE = register(new Block(441, "white_stained_glass_pane", true, 9372, 9403, 0, null));

    public static final Block ORANGE_STAINED_GLASS_PANE = register(new Block(442, "orange_stained_glass_pane", true, 9404, 9435, 0, null));

    public static final Block MAGENTA_STAINED_GLASS_PANE = register(new Block(443, "magenta_stained_glass_pane", true, 9436, 9467, 0, null));

    public static final Block LIGHT_BLUE_STAINED_GLASS_PANE = register(new Block(444, "light_blue_stained_glass_pane", true, 9468, 9499, 0, null));

    public static final Block YELLOW_STAINED_GLASS_PANE = register(new Block(445, "yellow_stained_glass_pane", true, 9500, 9531, 0, null));

    public static final Block LIME_STAINED_GLASS_PANE = register(new Block(446, "lime_stained_glass_pane", true, 9532, 9563, 0, null));

    public static final Block PINK_STAINED_GLASS_PANE = register(new Block(447, "pink_stained_glass_pane", true, 9564, 9595, 0, null));

    public static final Block GRAY_STAINED_GLASS_PANE = register(new Block(448, "gray_stained_glass_pane", true, 9596, 9627, 0, null));

    public static final Block LIGHT_GRAY_STAINED_GLASS_PANE = register(new Block(449, "light_gray_stained_glass_pane", true, 9628, 9659, 0, null));

    public static final Block CYAN_STAINED_GLASS_PANE = register(new Block(450, "cyan_stained_glass_pane", true, 9660, 9691, 0, null));

    public static final Block PURPLE_STAINED_GLASS_PANE = register(new Block(451, "purple_stained_glass_pane", true, 9692, 9723, 0, null));

    public static final Block BLUE_STAINED_GLASS_PANE = register(new Block(452, "blue_stained_glass_pane", true, 9724, 9755, 0, null));

    public static final Block BROWN_STAINED_GLASS_PANE = register(new Block(453, "brown_stained_glass_pane", true, 9756, 9787, 0, null));

    public static final Block GREEN_STAINED_GLASS_PANE = register(new Block(454, "green_stained_glass_pane", true, 9788, 9819, 0, null));

    public static final Block RED_STAINED_GLASS_PANE = register(new Block(455, "red_stained_glass_pane", true, 9820, 9851, 0, null));

    public static final Block BLACK_STAINED_GLASS_PANE = register(new Block(456, "black_stained_glass_pane", true, 9852, 9883, 0, null));

    public static final Block ACACIA_STAIRS = register(new Block(457, "acacia_stairs", true, 9884, 9963, 15, null));

    public static final Block CHERRY_STAIRS = register(new Block(458, "cherry_stairs", true, 9964, 10043, 36, null));

    public static final Block DARK_OAK_STAIRS = register(new Block(459, "dark_oak_stairs", true, 10044, 10123, 26, null));

    public static final Block MANGROVE_STAIRS = register(new Block(460, "mangrove_stairs", true, 10124, 10203, 28, null));

    public static final Block BAMBOO_STAIRS = register(new Block(461, "bamboo_stairs", true, 10204, 10283, 18, null));

    public static final Block BAMBOO_MOSAIC_STAIRS = register(new Block(462, "bamboo_mosaic_stairs", true, 10284, 10363, 18, null));

    public static final Block SLIME_BLOCK = register(new Block(463, "slime_block", true, 10364, 10364, 1, null));

    public static final Block BARRIER = register(new Block(464, "barrier", true, 10365, 10366, 0, null));

    public static final Block LIGHT = register(new Block(465, "light", false, 10367, 10398, 0, null));

    public static final Block IRON_TRAPDOOR = register(new Block(466, "iron_trapdoor", true, 10399, 10462, 6, null));

    public static final Block PRISMARINE = register(new Block(467, "prismarine", true, 10463, 10463, 23, null));

    public static final Block PRISMARINE_BRICKS = register(new Block(468, "prismarine_bricks", true, 10464, 10464, 31, null));

    public static final Block DARK_PRISMARINE = register(new Block(469, "dark_prismarine", true, 10465, 10465, 31, null));

    public static final Block PRISMARINE_STAIRS = register(new Block(470, "prismarine_stairs", true, 10466, 10545, 23, null));

    public static final Block PRISMARINE_BRICK_STAIRS = register(new Block(471, "prismarine_brick_stairs", true, 10546, 10625, 31, null));

    public static final Block DARK_PRISMARINE_STAIRS = register(new Block(472, "dark_prismarine_stairs", true, 10626, 10705, 31, null));

    public static final Block PRISMARINE_SLAB = register(new Block(473, "prismarine_slab", true, 10706, 10711, 23, null));

    public static final Block PRISMARINE_BRICK_SLAB = register(new Block(474, "prismarine_brick_slab", true, 10712, 10717, 31, null));

    public static final Block DARK_PRISMARINE_SLAB = register(new Block(475, "dark_prismarine_slab", true, 10718, 10723, 31, null));

    public static final Block SEA_LANTERN = register(new Block(476, "sea_lantern", true, 10724, 10724, 14, null));

    public static final Block HAY_BLOCK = register(new Block(477, "hay_block", true, 10725, 10727, 18, null));

    public static final Block WHITE_CARPET = register(new Block(478, "white_carpet", true, 10728, 10728, 8, null));

    public static final Block ORANGE_CARPET = register(new Block(479, "orange_carpet", true, 10729, 10729, 15, null));

    public static final Block MAGENTA_CARPET = register(new Block(480, "magenta_carpet", true, 10730, 10730, 16, null));

    public static final Block LIGHT_BLUE_CARPET = register(new Block(481, "light_blue_carpet", true, 10731, 10731, 17, null));

    public static final Block YELLOW_CARPET = register(new Block(482, "yellow_carpet", true, 10732, 10732, 18, null));

    public static final Block LIME_CARPET = register(new Block(483, "lime_carpet", true, 10733, 10733, 19, null));

    public static final Block PINK_CARPET = register(new Block(484, "pink_carpet", true, 10734, 10734, 20, null));

    public static final Block GRAY_CARPET = register(new Block(485, "gray_carpet", true, 10735, 10735, 21, null));

    public static final Block LIGHT_GRAY_CARPET = register(new Block(486, "light_gray_carpet", true, 10736, 10736, 22, null));

    public static final Block CYAN_CARPET = register(new Block(487, "cyan_carpet", true, 10737, 10737, 23, null));

    public static final Block PURPLE_CARPET = register(new Block(488, "purple_carpet", true, 10738, 10738, 24, null));

    public static final Block BLUE_CARPET = register(new Block(489, "blue_carpet", true, 10739, 10739, 25, null));

    public static final Block BROWN_CARPET = register(new Block(490, "brown_carpet", true, 10740, 10740, 26, null));

    public static final Block GREEN_CARPET = register(new Block(491, "green_carpet", true, 10741, 10741, 27, null));

    public static final Block RED_CARPET = register(new Block(492, "red_carpet", true, 10742, 10742, 28, null));

    public static final Block BLACK_CARPET = register(new Block(493, "black_carpet", true, 10743, 10743, 29, null));

    public static final Block TERRACOTTA = register(new Block(494, "terracotta", true, 10744, 10744, 15, null));

    public static final Block COAL_BLOCK = register(new Block(495, "coal_block", true, 10745, 10745, 29, null));

    public static final Block PACKED_ICE = register(new Block(496, "packed_ice", true, 10746, 10746, 5, null));

    public static final Block SUNFLOWER = register(new Block(497, "sunflower", false, 10747, 10748, 7, null));

    public static final Block LILAC = register(new Block(498, "lilac", false, 10749, 10750, 7, null));

    public static final Block ROSE_BUSH = register(new Block(499, "rose_bush", false, 10751, 10752, 7, null));

    public static final Block PEONY = register(new Block(500, "peony", false, 10753, 10754, 7, null));

    public static final Block TALL_GRASS = register(new Block(501, "tall_grass", false, 10755, 10756, 7, null));

    public static final Block LARGE_FERN = register(new Block(502, "large_fern", false, 10757, 10758, 7, null));

    public static final Block WHITE_BANNER = register(new Block(503, "white_banner", false, 10759, 10774, 13, BlockEntityType.BANNER));

    public static final Block ORANGE_BANNER = register(new Block(504, "orange_banner", false, 10775, 10790, 13, BlockEntityType.BANNER));

    public static final Block MAGENTA_BANNER = register(new Block(505, "magenta_banner", false, 10791, 10806, 13, BlockEntityType.BANNER));

    public static final Block LIGHT_BLUE_BANNER = register(new Block(506, "light_blue_banner", false, 10807, 10822, 13, BlockEntityType.BANNER));

    public static final Block YELLOW_BANNER = register(new Block(507, "yellow_banner", false, 10823, 10838, 13, BlockEntityType.BANNER));

    public static final Block LIME_BANNER = register(new Block(508, "lime_banner", false, 10839, 10854, 13, BlockEntityType.BANNER));

    public static final Block PINK_BANNER = register(new Block(509, "pink_banner", false, 10855, 10870, 13, BlockEntityType.BANNER));

    public static final Block GRAY_BANNER = register(new Block(510, "gray_banner", false, 10871, 10886, 13, BlockEntityType.BANNER));

    public static final Block LIGHT_GRAY_BANNER = register(new Block(511, "light_gray_banner", false, 10887, 10902, 13, BlockEntityType.BANNER));

    public static final Block CYAN_BANNER = register(new Block(512, "cyan_banner", false, 10903, 10918, 13, BlockEntityType.BANNER));

    public static final Block PURPLE_BANNER = register(new Block(513, "purple_banner", false, 10919, 10934, 13, BlockEntityType.BANNER));

    public static final Block BLUE_BANNER = register(new Block(514, "blue_banner", false, 10935, 10950, 13, BlockEntityType.BANNER));

    public static final Block BROWN_BANNER = register(new Block(515, "brown_banner", false, 10951, 10966, 13, BlockEntityType.BANNER));

    public static final Block GREEN_BANNER = register(new Block(516, "green_banner", false, 10967, 10982, 13, BlockEntityType.BANNER));

    public static final Block RED_BANNER = register(new Block(517, "red_banner", false, 10983, 10998, 13, BlockEntityType.BANNER));

    public static final Block BLACK_BANNER = register(new Block(518, "black_banner", false, 10999, 11014, 13, BlockEntityType.BANNER));

    public static final Block WHITE_WALL_BANNER = register(new Block(519, "white_wall_banner", false, 11015, 11018, 13, BlockEntityType.BANNER));

    public static final Block ORANGE_WALL_BANNER = register(new Block(520, "orange_wall_banner", false, 11019, 11022, 13, BlockEntityType.BANNER));

    public static final Block MAGENTA_WALL_BANNER = register(new Block(521, "magenta_wall_banner", false, 11023, 11026, 13, BlockEntityType.BANNER));

    public static final Block LIGHT_BLUE_WALL_BANNER = register(new Block(522, "light_blue_wall_banner", false, 11027, 11030, 13, BlockEntityType.BANNER));

    public static final Block YELLOW_WALL_BANNER = register(new Block(523, "yellow_wall_banner", false, 11031, 11034, 13, BlockEntityType.BANNER));

    public static final Block LIME_WALL_BANNER = register(new Block(524, "lime_wall_banner", false, 11035, 11038, 13, BlockEntityType.BANNER));

    public static final Block PINK_WALL_BANNER = register(new Block(525, "pink_wall_banner", false, 11039, 11042, 13, BlockEntityType.BANNER));

    public static final Block GRAY_WALL_BANNER = register(new Block(526, "gray_wall_banner", false, 11043, 11046, 13, BlockEntityType.BANNER));

    public static final Block LIGHT_GRAY_WALL_BANNER = register(new Block(527, "light_gray_wall_banner", false, 11047, 11050, 13, BlockEntityType.BANNER));

    public static final Block CYAN_WALL_BANNER = register(new Block(528, "cyan_wall_banner", false, 11051, 11054, 13, BlockEntityType.BANNER));

    public static final Block PURPLE_WALL_BANNER = register(new Block(529, "purple_wall_banner", false, 11055, 11058, 13, BlockEntityType.BANNER));

    public static final Block BLUE_WALL_BANNER = register(new Block(530, "blue_wall_banner", false, 11059, 11062, 13, BlockEntityType.BANNER));

    public static final Block BROWN_WALL_BANNER = register(new Block(531, "brown_wall_banner", false, 11063, 11066, 13, BlockEntityType.BANNER));

    public static final Block GREEN_WALL_BANNER = register(new Block(532, "green_wall_banner", false, 11067, 11070, 13, BlockEntityType.BANNER));

    public static final Block RED_WALL_BANNER = register(new Block(533, "red_wall_banner", false, 11071, 11074, 13, BlockEntityType.BANNER));

    public static final Block BLACK_WALL_BANNER = register(new Block(534, "black_wall_banner", false, 11075, 11078, 13, BlockEntityType.BANNER));

    public static final Block RED_SANDSTONE = register(new Block(535, "red_sandstone", true, 11079, 11079, 15, null));

    public static final Block CHISELED_RED_SANDSTONE = register(new Block(536, "chiseled_red_sandstone", true, 11080, 11080, 15, null));

    public static final Block CUT_RED_SANDSTONE = register(new Block(537, "cut_red_sandstone", true, 11081, 11081, 15, null));

    public static final Block RED_SANDSTONE_STAIRS = register(new Block(538, "red_sandstone_stairs", true, 11082, 11161, 15, null));

    public static final Block OAK_SLAB = register(new Block(539, "oak_slab", true, 11162, 11167, 13, null));

    public static final Block SPRUCE_SLAB = register(new Block(540, "spruce_slab", true, 11168, 11173, 34, null));

    public static final Block BIRCH_SLAB = register(new Block(541, "birch_slab", true, 11174, 11179, 2, null));

    public static final Block JUNGLE_SLAB = register(new Block(542, "jungle_slab", true, 11180, 11185, 10, null));

    public static final Block ACACIA_SLAB = register(new Block(543, "acacia_slab", true, 11186, 11191, 15, null));

    public static final Block CHERRY_SLAB = register(new Block(544, "cherry_slab", true, 11192, 11197, 36, null));

    public static final Block DARK_OAK_SLAB = register(new Block(545, "dark_oak_slab", true, 11198, 11203, 26, null));

    public static final Block MANGROVE_SLAB = register(new Block(546, "mangrove_slab", true, 11204, 11209, 28, null));

    public static final Block BAMBOO_SLAB = register(new Block(547, "bamboo_slab", true, 11210, 11215, 18, null));

    public static final Block BAMBOO_MOSAIC_SLAB = register(new Block(548, "bamboo_mosaic_slab", true, 11216, 11221, 18, null));

    public static final Block STONE_SLAB = register(new Block(549, "stone_slab", true, 11222, 11227, 11, null));

    public static final Block SMOOTH_STONE_SLAB = register(new Block(550, "smooth_stone_slab", true, 11228, 11233, 11, null));

    public static final Block SANDSTONE_SLAB = register(new Block(551, "sandstone_slab", true, 11234, 11239, 2, null));

    public static final Block CUT_SANDSTONE_SLAB = register(new Block(552, "cut_sandstone_slab", true, 11240, 11245, 2, null));

    public static final Block PETRIFIED_OAK_SLAB = register(new Block(553, "petrified_oak_slab", true, 11246, 11251, 13, null));

    public static final Block COBBLESTONE_SLAB = register(new Block(554, "cobblestone_slab", true, 11252, 11257, 11, null));

    public static final Block BRICK_SLAB = register(new Block(555, "brick_slab", true, 11258, 11263, 28, null));

    public static final Block STONE_BRICK_SLAB = register(new Block(556, "stone_brick_slab", true, 11264, 11269, 11, null));

    public static final Block MUD_BRICK_SLAB = register(new Block(557, "mud_brick_slab", true, 11270, 11275, 44, null));

    public static final Block NETHER_BRICK_SLAB = register(new Block(558, "nether_brick_slab", true, 11276, 11281, 35, null));

    public static final Block QUARTZ_SLAB = register(new Block(559, "quartz_slab", true, 11282, 11287, 14, null));

    public static final Block RED_SANDSTONE_SLAB = register(new Block(560, "red_sandstone_slab", true, 11288, 11293, 15, null));

    public static final Block CUT_RED_SANDSTONE_SLAB = register(new Block(561, "cut_red_sandstone_slab", true, 11294, 11299, 15, null));

    public static final Block PURPUR_SLAB = register(new Block(562, "purpur_slab", true, 11300, 11305, 16, null));

    public static final Block SMOOTH_STONE = register(new Block(563, "smooth_stone", true, 11306, 11306, 11, null));

    public static final Block SMOOTH_SANDSTONE = register(new Block(564, "smooth_sandstone", true, 11307, 11307, 2, null));

    public static final Block SMOOTH_QUARTZ = register(new Block(565, "smooth_quartz", true, 11308, 11308, 14, null));

    public static final Block SMOOTH_RED_SANDSTONE = register(new Block(566, "smooth_red_sandstone", true, 11309, 11309, 15, null));

    public static final Block SPRUCE_FENCE_GATE = register(new Block(567, "spruce_fence_gate", true, 11310, 11341, 34, null));

    public static final Block BIRCH_FENCE_GATE = register(new Block(568, "birch_fence_gate", true, 11342, 11373, 2, null));

    public static final Block JUNGLE_FENCE_GATE = register(new Block(569, "jungle_fence_gate", true, 11374, 11405, 10, null));

    public static final Block ACACIA_FENCE_GATE = register(new Block(570, "acacia_fence_gate", true, 11406, 11437, 15, null));

    public static final Block CHERRY_FENCE_GATE = register(new Block(571, "cherry_fence_gate", true, 11438, 11469, 36, null));

    public static final Block DARK_OAK_FENCE_GATE = register(new Block(572, "dark_oak_fence_gate", true, 11470, 11501, 26, null));

    public static final Block MANGROVE_FENCE_GATE = register(new Block(573, "mangrove_fence_gate", true, 11502, 11533, 28, null));

    public static final Block BAMBOO_FENCE_GATE = register(new Block(574, "bamboo_fence_gate", true, 11534, 11565, 18, null));

    public static final Block SPRUCE_FENCE = register(new Block(575, "spruce_fence", true, 11566, 11597, 34, null));

    public static final Block BIRCH_FENCE = register(new Block(576, "birch_fence", true, 11598, 11629, 2, null));

    public static final Block JUNGLE_FENCE = register(new Block(577, "jungle_fence", true, 11630, 11661, 10, null));

    public static final Block ACACIA_FENCE = register(new Block(578, "acacia_fence", true, 11662, 11693, 15, null));

    public static final Block CHERRY_FENCE = register(new Block(579, "cherry_fence", true, 11694, 11725, 36, null));

    public static final Block DARK_OAK_FENCE = register(new Block(580, "dark_oak_fence", true, 11726, 11757, 26, null));

    public static final Block MANGROVE_FENCE = register(new Block(581, "mangrove_fence", true, 11758, 11789, 28, null));

    public static final Block BAMBOO_FENCE = register(new Block(582, "bamboo_fence", true, 11790, 11821, 18, null));

    public static final Block SPRUCE_DOOR = register(new Block(583, "spruce_door", true, 11822, 11885, 34, null));

    public static final Block BIRCH_DOOR = register(new Block(584, "birch_door", true, 11886, 11949, 2, null));

    public static final Block JUNGLE_DOOR = register(new Block(585, "jungle_door", true, 11950, 12013, 10, null));

    public static final Block ACACIA_DOOR = register(new Block(586, "acacia_door", true, 12014, 12077, 15, null));

    public static final Block CHERRY_DOOR = register(new Block(587, "cherry_door", true, 12078, 12141, 36, null));

    public static final Block DARK_OAK_DOOR = register(new Block(588, "dark_oak_door", true, 12142, 12205, 26, null));

    public static final Block MANGROVE_DOOR = register(new Block(589, "mangrove_door", true, 12206, 12269, 28, null));

    public static final Block BAMBOO_DOOR = register(new Block(590, "bamboo_door", true, 12270, 12333, 18, null));

    public static final Block END_ROD = register(new Block(591, "end_rod", true, 12334, 12339, 0, null));

    public static final Block CHORUS_PLANT = register(new Block(592, "chorus_plant", true, 12340, 12403, 24, null));

    public static final Block CHORUS_FLOWER = register(new Block(593, "chorus_flower", true, 12404, 12409, 24, null));

    public static final Block PURPUR_BLOCK = register(new Block(594, "purpur_block", true, 12410, 12410, 16, null));

    public static final Block PURPUR_PILLAR = register(new Block(595, "purpur_pillar", true, 12411, 12413, 16, null));

    public static final Block PURPUR_STAIRS = register(new Block(596, "purpur_stairs", true, 12414, 12493, 16, null));

    public static final Block END_STONE_BRICKS = register(new Block(597, "end_stone_bricks", true, 12494, 12494, 2, null));

    public static final Block TORCHFLOWER_CROP = register(new Block(598, "torchflower_crop", false, 12495, 12496, 7, null));

    public static final Block PITCHER_CROP = register(new Block(599, "pitcher_crop", true, 12497, 12506, 7, null));

    public static final Block PITCHER_PLANT = register(new Block(600, "pitcher_plant", false, 12507, 12508, 7, null));

    public static final Block BEETROOTS = register(new Block(601, "beetroots", false, 12509, 12512, 7, null));

    public static final Block DIRT_PATH = register(new Block(602, "dirt_path", true, 12513, 12513, 10, null));

    public static final Block END_GATEWAY = register(new Block(603, "end_gateway", false, 12514, 12514, 29, BlockEntityType.END_GATEWAY));

    public static final Block REPEATING_COMMAND_BLOCK = register(new Block(604, "repeating_command_block", true, 12515, 12526, 24, BlockEntityType.COMMAND_BLOCK));

    public static final Block CHAIN_COMMAND_BLOCK = register(new Block(605, "chain_command_block", true, 12527, 12538, 27, BlockEntityType.COMMAND_BLOCK));

    public static final Block FROSTED_ICE = register(new Block(606, "frosted_ice", true, 12539, 12542, 5, null));

    public static final Block MAGMA_BLOCK = register(new Block(607, "magma_block", true, 12543, 12543, 35, null));

    public static final Block NETHER_WART_BLOCK = register(new Block(608, "nether_wart_block", true, 12544, 12544, 28, null));

    public static final Block RED_NETHER_BRICKS = register(new Block(609, "red_nether_bricks", true, 12545, 12545, 35, null));

    public static final Block BONE_BLOCK = register(new Block(610, "bone_block", true, 12546, 12548, 2, null));

    public static final Block STRUCTURE_VOID = register(new Block(611, "structure_void", false, 12549, 12549, 0, null));

    public static final Block OBSERVER = register(new Block(612, "observer", true, 12550, 12561, 11, null));

    public static final Block SHULKER_BOX = register(new Block(613, "shulker_box", true, 12562, 12567, 24, BlockEntityType.SHULKER_BOX));

    public static final Block WHITE_SHULKER_BOX = register(new Block(614, "white_shulker_box", true, 12568, 12573, 8, BlockEntityType.SHULKER_BOX));

    public static final Block ORANGE_SHULKER_BOX = register(new Block(615, "orange_shulker_box", true, 12574, 12579, 15, BlockEntityType.SHULKER_BOX));

    public static final Block MAGENTA_SHULKER_BOX = register(new Block(616, "magenta_shulker_box", true, 12580, 12585, 16, BlockEntityType.SHULKER_BOX));

    public static final Block LIGHT_BLUE_SHULKER_BOX = register(new Block(617, "light_blue_shulker_box", true, 12586, 12591, 17, BlockEntityType.SHULKER_BOX));

    public static final Block YELLOW_SHULKER_BOX = register(new Block(618, "yellow_shulker_box", true, 12592, 12597, 18, BlockEntityType.SHULKER_BOX));

    public static final Block LIME_SHULKER_BOX = register(new Block(619, "lime_shulker_box", true, 12598, 12603, 19, BlockEntityType.SHULKER_BOX));

    public static final Block PINK_SHULKER_BOX = register(new Block(620, "pink_shulker_box", true, 12604, 12609, 20, BlockEntityType.SHULKER_BOX));

    public static final Block GRAY_SHULKER_BOX = register(new Block(621, "gray_shulker_box", true, 12610, 12615, 21, BlockEntityType.SHULKER_BOX));

    public static final Block LIGHT_GRAY_SHULKER_BOX = register(new Block(622, "light_gray_shulker_box", true, 12616, 12621, 22, BlockEntityType.SHULKER_BOX));

    public static final Block CYAN_SHULKER_BOX = register(new Block(623, "cyan_shulker_box", true, 12622, 12627, 23, BlockEntityType.SHULKER_BOX));

    public static final Block PURPLE_SHULKER_BOX = register(new Block(624, "purple_shulker_box", true, 12628, 12633, 46, BlockEntityType.SHULKER_BOX));

    public static final Block BLUE_SHULKER_BOX = register(new Block(625, "blue_shulker_box", true, 12634, 12639, 25, BlockEntityType.SHULKER_BOX));

    public static final Block BROWN_SHULKER_BOX = register(new Block(626, "brown_shulker_box", true, 12640, 12645, 26, BlockEntityType.SHULKER_BOX));

    public static final Block GREEN_SHULKER_BOX = register(new Block(627, "green_shulker_box", true, 12646, 12651, 27, BlockEntityType.SHULKER_BOX));

    public static final Block RED_SHULKER_BOX = register(new Block(628, "red_shulker_box", true, 12652, 12657, 28, BlockEntityType.SHULKER_BOX));

    public static final Block BLACK_SHULKER_BOX = register(new Block(629, "black_shulker_box", true, 12658, 12663, 29, BlockEntityType.SHULKER_BOX));

    public static final Block WHITE_GLAZED_TERRACOTTA = register(new Block(630, "white_glazed_terracotta", true, 12664, 12667, 8, null));

    public static final Block ORANGE_GLAZED_TERRACOTTA = register(new Block(631, "orange_glazed_terracotta", true, 12668, 12671, 15, null));

    public static final Block MAGENTA_GLAZED_TERRACOTTA = register(new Block(632, "magenta_glazed_terracotta", true, 12672, 12675, 16, null));

    public static final Block LIGHT_BLUE_GLAZED_TERRACOTTA = register(new Block(633, "light_blue_glazed_terracotta", true, 12676, 12679, 17, null));

    public static final Block YELLOW_GLAZED_TERRACOTTA = register(new Block(634, "yellow_glazed_terracotta", true, 12680, 12683, 18, null));

    public static final Block LIME_GLAZED_TERRACOTTA = register(new Block(635, "lime_glazed_terracotta", true, 12684, 12687, 19, null));

    public static final Block PINK_GLAZED_TERRACOTTA = register(new Block(636, "pink_glazed_terracotta", true, 12688, 12691, 20, null));

    public static final Block GRAY_GLAZED_TERRACOTTA = register(new Block(637, "gray_glazed_terracotta", true, 12692, 12695, 21, null));

    public static final Block LIGHT_GRAY_GLAZED_TERRACOTTA = register(new Block(638, "light_gray_glazed_terracotta", true, 12696, 12699, 22, null));

    public static final Block CYAN_GLAZED_TERRACOTTA = register(new Block(639, "cyan_glazed_terracotta", true, 12700, 12703, 23, null));

    public static final Block PURPLE_GLAZED_TERRACOTTA = register(new Block(640, "purple_glazed_terracotta", true, 12704, 12707, 24, null));

    public static final Block BLUE_GLAZED_TERRACOTTA = register(new Block(641, "blue_glazed_terracotta", true, 12708, 12711, 25, null));

    public static final Block BROWN_GLAZED_TERRACOTTA = register(new Block(642, "brown_glazed_terracotta", true, 12712, 12715, 26, null));

    public static final Block GREEN_GLAZED_TERRACOTTA = register(new Block(643, "green_glazed_terracotta", true, 12716, 12719, 27, null));

    public static final Block RED_GLAZED_TERRACOTTA = register(new Block(644, "red_glazed_terracotta", true, 12720, 12723, 28, null));

    public static final Block BLACK_GLAZED_TERRACOTTA = register(new Block(645, "black_glazed_terracotta", true, 12724, 12727, 29, null));

    public static final Block WHITE_CONCRETE = register(new Block(646, "white_concrete", true, 12728, 12728, 8, null));

    public static final Block ORANGE_CONCRETE = register(new Block(647, "orange_concrete", true, 12729, 12729, 15, null));

    public static final Block MAGENTA_CONCRETE = register(new Block(648, "magenta_concrete", true, 12730, 12730, 16, null));

    public static final Block LIGHT_BLUE_CONCRETE = register(new Block(649, "light_blue_concrete", true, 12731, 12731, 17, null));

    public static final Block YELLOW_CONCRETE = register(new Block(650, "yellow_concrete", true, 12732, 12732, 18, null));

    public static final Block LIME_CONCRETE = register(new Block(651, "lime_concrete", true, 12733, 12733, 19, null));

    public static final Block PINK_CONCRETE = register(new Block(652, "pink_concrete", true, 12734, 12734, 20, null));

    public static final Block GRAY_CONCRETE = register(new Block(653, "gray_concrete", true, 12735, 12735, 21, null));

    public static final Block LIGHT_GRAY_CONCRETE = register(new Block(654, "light_gray_concrete", true, 12736, 12736, 22, null));

    public static final Block CYAN_CONCRETE = register(new Block(655, "cyan_concrete", true, 12737, 12737, 23, null));

    public static final Block PURPLE_CONCRETE = register(new Block(656, "purple_concrete", true, 12738, 12738, 24, null));

    public static final Block BLUE_CONCRETE = register(new Block(657, "blue_concrete", true, 12739, 12739, 25, null));

    public static final Block BROWN_CONCRETE = register(new Block(658, "brown_concrete", true, 12740, 12740, 26, null));

    public static final Block GREEN_CONCRETE = register(new Block(659, "green_concrete", true, 12741, 12741, 27, null));

    public static final Block RED_CONCRETE = register(new Block(660, "red_concrete", true, 12742, 12742, 28, null));

    public static final Block BLACK_CONCRETE = register(new Block(661, "black_concrete", true, 12743, 12743, 29, null));

    public static final Block WHITE_CONCRETE_POWDER = register(new Block(662, "white_concrete_powder", true, 12744, 12744, 8, null));

    public static final Block ORANGE_CONCRETE_POWDER = register(new Block(663, "orange_concrete_powder", true, 12745, 12745, 15, null));

    public static final Block MAGENTA_CONCRETE_POWDER = register(new Block(664, "magenta_concrete_powder", true, 12746, 12746, 16, null));

    public static final Block LIGHT_BLUE_CONCRETE_POWDER = register(new Block(665, "light_blue_concrete_powder", true, 12747, 12747, 17, null));

    public static final Block YELLOW_CONCRETE_POWDER = register(new Block(666, "yellow_concrete_powder", true, 12748, 12748, 18, null));

    public static final Block LIME_CONCRETE_POWDER = register(new Block(667, "lime_concrete_powder", true, 12749, 12749, 19, null));

    public static final Block PINK_CONCRETE_POWDER = register(new Block(668, "pink_concrete_powder", true, 12750, 12750, 20, null));

    public static final Block GRAY_CONCRETE_POWDER = register(new Block(669, "gray_concrete_powder", true, 12751, 12751, 21, null));

    public static final Block LIGHT_GRAY_CONCRETE_POWDER = register(new Block(670, "light_gray_concrete_powder", true, 12752, 12752, 22, null));

    public static final Block CYAN_CONCRETE_POWDER = register(new Block(671, "cyan_concrete_powder", true, 12753, 12753, 23, null));

    public static final Block PURPLE_CONCRETE_POWDER = register(new Block(672, "purple_concrete_powder", true, 12754, 12754, 24, null));

    public static final Block BLUE_CONCRETE_POWDER = register(new Block(673, "blue_concrete_powder", true, 12755, 12755, 25, null));

    public static final Block BROWN_CONCRETE_POWDER = register(new Block(674, "brown_concrete_powder", true, 12756, 12756, 26, null));

    public static final Block GREEN_CONCRETE_POWDER = register(new Block(675, "green_concrete_powder", true, 12757, 12757, 27, null));

    public static final Block RED_CONCRETE_POWDER = register(new Block(676, "red_concrete_powder", true, 12758, 12758, 28, null));

    public static final Block BLACK_CONCRETE_POWDER = register(new Block(677, "black_concrete_powder", true, 12759, 12759, 29, null));

    public static final Block KELP = register(new Block(678, "kelp", false, 12760, 12785, 12, null));

    public static final Block KELP_PLANT = register(new Block(679, "kelp_plant", false, 12786, 12786, 12, null));

    public static final Block DRIED_KELP_BLOCK = register(new Block(680, "dried_kelp_block", true, 12787, 12787, 27, null));

    public static final Block TURTLE_EGG = register(new Block(681, "turtle_egg", true, 12788, 12799, 2, null));

    public static final Block SNIFFER_EGG = register(new Block(682, "sniffer_egg", true, 12800, 12802, 28, null));

    public static final Block DEAD_TUBE_CORAL_BLOCK = register(new Block(683, "dead_tube_coral_block", true, 12803, 12803, 21, null));

    public static final Block DEAD_BRAIN_CORAL_BLOCK = register(new Block(684, "dead_brain_coral_block", true, 12804, 12804, 21, null));

    public static final Block DEAD_BUBBLE_CORAL_BLOCK = register(new Block(685, "dead_bubble_coral_block", true, 12805, 12805, 21, null));

    public static final Block DEAD_FIRE_CORAL_BLOCK = register(new Block(686, "dead_fire_coral_block", true, 12806, 12806, 21, null));

    public static final Block DEAD_HORN_CORAL_BLOCK = register(new Block(687, "dead_horn_coral_block", true, 12807, 12807, 21, null));

    public static final Block TUBE_CORAL_BLOCK = register(new Block(688, "tube_coral_block", true, 12808, 12808, 25, null));

    public static final Block BRAIN_CORAL_BLOCK = register(new Block(689, "brain_coral_block", true, 12809, 12809, 20, null));

    public static final Block BUBBLE_CORAL_BLOCK = register(new Block(690, "bubble_coral_block", true, 12810, 12810, 24, null));

    public static final Block FIRE_CORAL_BLOCK = register(new Block(691, "fire_coral_block", true, 12811, 12811, 28, null));

    public static final Block HORN_CORAL_BLOCK = register(new Block(692, "horn_coral_block", true, 12812, 12812, 18, null));

    public static final Block DEAD_TUBE_CORAL = register(new Block(693, "dead_tube_coral", false, 12813, 12814, 21, null));

    public static final Block DEAD_BRAIN_CORAL = register(new Block(694, "dead_brain_coral", false, 12815, 12816, 21, null));

    public static final Block DEAD_BUBBLE_CORAL = register(new Block(695, "dead_bubble_coral", false, 12817, 12818, 21, null));

    public static final Block DEAD_FIRE_CORAL = register(new Block(696, "dead_fire_coral", false, 12819, 12820, 21, null));

    public static final Block DEAD_HORN_CORAL = register(new Block(697, "dead_horn_coral", false, 12821, 12822, 21, null));

    public static final Block TUBE_CORAL = register(new Block(698, "tube_coral", false, 12823, 12824, 25, null));

    public static final Block BRAIN_CORAL = register(new Block(699, "brain_coral", false, 12825, 12826, 20, null));

    public static final Block BUBBLE_CORAL = register(new Block(700, "bubble_coral", false, 12827, 12828, 24, null));

    public static final Block FIRE_CORAL = register(new Block(701, "fire_coral", false, 12829, 12830, 28, null));

    public static final Block HORN_CORAL = register(new Block(702, "horn_coral", false, 12831, 12832, 18, null));

    public static final Block DEAD_TUBE_CORAL_FAN = register(new Block(703, "dead_tube_coral_fan", false, 12833, 12834, 21, null));

    public static final Block DEAD_BRAIN_CORAL_FAN = register(new Block(704, "dead_brain_coral_fan", false, 12835, 12836, 21, null));

    public static final Block DEAD_BUBBLE_CORAL_FAN = register(new Block(705, "dead_bubble_coral_fan", false, 12837, 12838, 21, null));

    public static final Block DEAD_FIRE_CORAL_FAN = register(new Block(706, "dead_fire_coral_fan", false, 12839, 12840, 21, null));

    public static final Block DEAD_HORN_CORAL_FAN = register(new Block(707, "dead_horn_coral_fan", false, 12841, 12842, 21, null));

    public static final Block TUBE_CORAL_FAN = register(new Block(708, "tube_coral_fan", false, 12843, 12844, 25, null));

    public static final Block BRAIN_CORAL_FAN = register(new Block(709, "brain_coral_fan", false, 12845, 12846, 20, null));

    public static final Block BUBBLE_CORAL_FAN = register(new Block(710, "bubble_coral_fan", false, 12847, 12848, 24, null));

    public static final Block FIRE_CORAL_FAN = register(new Block(711, "fire_coral_fan", false, 12849, 12850, 28, null));

    public static final Block HORN_CORAL_FAN = register(new Block(712, "horn_coral_fan", false, 12851, 12852, 18, null));

    public static final Block DEAD_TUBE_CORAL_WALL_FAN = register(new Block(713, "dead_tube_coral_wall_fan", false, 12853, 12860, 21, null));

    public static final Block DEAD_BRAIN_CORAL_WALL_FAN = register(new Block(714, "dead_brain_coral_wall_fan", false, 12861, 12868, 21, null));

    public static final Block DEAD_BUBBLE_CORAL_WALL_FAN = register(new Block(715, "dead_bubble_coral_wall_fan", false, 12869, 12876, 21, null));

    public static final Block DEAD_FIRE_CORAL_WALL_FAN = register(new Block(716, "dead_fire_coral_wall_fan", false, 12877, 12884, 21, null));

    public static final Block DEAD_HORN_CORAL_WALL_FAN = register(new Block(717, "dead_horn_coral_wall_fan", false, 12885, 12892, 21, null));

    public static final Block TUBE_CORAL_WALL_FAN = register(new Block(718, "tube_coral_wall_fan", false, 12893, 12900, 25, null));

    public static final Block BRAIN_CORAL_WALL_FAN = register(new Block(719, "brain_coral_wall_fan", false, 12901, 12908, 20, null));

    public static final Block BUBBLE_CORAL_WALL_FAN = register(new Block(720, "bubble_coral_wall_fan", false, 12909, 12916, 24, null));

    public static final Block FIRE_CORAL_WALL_FAN = register(new Block(721, "fire_coral_wall_fan", false, 12917, 12924, 28, null));

    public static final Block HORN_CORAL_WALL_FAN = register(new Block(722, "horn_coral_wall_fan", false, 12925, 12932, 18, null));

    public static final Block SEA_PICKLE = register(new Block(723, "sea_pickle", true, 12933, 12940, 27, null));

    public static final Block BLUE_ICE = register(new Block(724, "blue_ice", true, 12941, 12941, 5, null));

    public static final Block CONDUIT = register(new Block(725, "conduit", true, 12942, 12943, 31, BlockEntityType.CONDUIT));

    public static final Block BAMBOO_SAPLING = register(new Block(726, "bamboo_sapling", false, 12944, 12944, 13, null));

    public static final Block BAMBOO = register(new Block(727, "bamboo", true, 12945, 12956, 7, null));

    public static final Block POTTED_BAMBOO = register(new Block(728, "potted_bamboo", true, 12957, 12957, 0, null));

    public static final Block VOID_AIR = register(new Block(729, "void_air", false, 12958, 12958, 0, null));

    public static final Block CAVE_AIR = register(new Block(730, "cave_air", false, 12959, 12959, 0, null));

    public static final Block BUBBLE_COLUMN = register(new Block(731, "bubble_column", false, 12960, 12961, 12, null));

    public static final Block POLISHED_GRANITE_STAIRS = register(new Block(732, "polished_granite_stairs", true, 12962, 13041, 10, null));

    public static final Block SMOOTH_RED_SANDSTONE_STAIRS = register(new Block(733, "smooth_red_sandstone_stairs", true, 13042, 13121, 15, null));

    public static final Block MOSSY_STONE_BRICK_STAIRS = register(new Block(734, "mossy_stone_brick_stairs", true, 13122, 13201, 11, null));

    public static final Block POLISHED_DIORITE_STAIRS = register(new Block(735, "polished_diorite_stairs", true, 13202, 13281, 14, null));

    public static final Block MOSSY_COBBLESTONE_STAIRS = register(new Block(736, "mossy_cobblestone_stairs", true, 13282, 13361, 11, null));

    public static final Block END_STONE_BRICK_STAIRS = register(new Block(737, "end_stone_brick_stairs", true, 13362, 13441, 2, null));

    public static final Block STONE_STAIRS = register(new Block(738, "stone_stairs", true, 13442, 13521, 11, null));

    public static final Block SMOOTH_SANDSTONE_STAIRS = register(new Block(739, "smooth_sandstone_stairs", true, 13522, 13601, 2, null));

    public static final Block SMOOTH_QUARTZ_STAIRS = register(new Block(740, "smooth_quartz_stairs", true, 13602, 13681, 14, null));

    public static final Block GRANITE_STAIRS = register(new Block(741, "granite_stairs", true, 13682, 13761, 10, null));

    public static final Block ANDESITE_STAIRS = register(new Block(742, "andesite_stairs", true, 13762, 13841, 11, null));

    public static final Block RED_NETHER_BRICK_STAIRS = register(new Block(743, "red_nether_brick_stairs", true, 13842, 13921, 35, null));

    public static final Block POLISHED_ANDESITE_STAIRS = register(new Block(744, "polished_andesite_stairs", true, 13922, 14001, 11, null));

    public static final Block DIORITE_STAIRS = register(new Block(745, "diorite_stairs", true, 14002, 14081, 14, null));

    public static final Block POLISHED_GRANITE_SLAB = register(new Block(746, "polished_granite_slab", true, 14082, 14087, 10, null));

    public static final Block SMOOTH_RED_SANDSTONE_SLAB = register(new Block(747, "smooth_red_sandstone_slab", true, 14088, 14093, 15, null));

    public static final Block MOSSY_STONE_BRICK_SLAB = register(new Block(748, "mossy_stone_brick_slab", true, 14094, 14099, 11, null));

    public static final Block POLISHED_DIORITE_SLAB = register(new Block(749, "polished_diorite_slab", true, 14100, 14105, 14, null));

    public static final Block MOSSY_COBBLESTONE_SLAB = register(new Block(750, "mossy_cobblestone_slab", true, 14106, 14111, 11, null));

    public static final Block END_STONE_BRICK_SLAB = register(new Block(751, "end_stone_brick_slab", true, 14112, 14117, 2, null));

    public static final Block SMOOTH_SANDSTONE_SLAB = register(new Block(752, "smooth_sandstone_slab", true, 14118, 14123, 2, null));

    public static final Block SMOOTH_QUARTZ_SLAB = register(new Block(753, "smooth_quartz_slab", true, 14124, 14129, 14, null));

    public static final Block GRANITE_SLAB = register(new Block(754, "granite_slab", true, 14130, 14135, 10, null));

    public static final Block ANDESITE_SLAB = register(new Block(755, "andesite_slab", true, 14136, 14141, 11, null));

    public static final Block RED_NETHER_BRICK_SLAB = register(new Block(756, "red_nether_brick_slab", true, 14142, 14147, 35, null));

    public static final Block POLISHED_ANDESITE_SLAB = register(new Block(757, "polished_andesite_slab", true, 14148, 14153, 11, null));

    public static final Block DIORITE_SLAB = register(new Block(758, "diorite_slab", true, 14154, 14159, 14, null));

    public static final Block BRICK_WALL = register(new Block(759, "brick_wall", true, 14160, 14483, 28, null));

    public static final Block PRISMARINE_WALL = register(new Block(760, "prismarine_wall", true, 14484, 14807, 23, null));

    public static final Block RED_SANDSTONE_WALL = register(new Block(761, "red_sandstone_wall", true, 14808, 15131, 15, null));

    public static final Block MOSSY_STONE_BRICK_WALL = register(new Block(762, "mossy_stone_brick_wall", true, 15132, 15455, 11, null));

    public static final Block GRANITE_WALL = register(new Block(763, "granite_wall", true, 15456, 15779, 10, null));

    public static final Block STONE_BRICK_WALL = register(new Block(764, "stone_brick_wall", true, 15780, 16103, 11, null));

    public static final Block MUD_BRICK_WALL = register(new Block(765, "mud_brick_wall", true, 16104, 16427, 44, null));

    public static final Block NETHER_BRICK_WALL = register(new Block(766, "nether_brick_wall", true, 16428, 16751, 35, null));

    public static final Block ANDESITE_WALL = register(new Block(767, "andesite_wall", true, 16752, 17075, 11, null));

    public static final Block RED_NETHER_BRICK_WALL = register(new Block(768, "red_nether_brick_wall", true, 17076, 17399, 35, null));

    public static final Block SANDSTONE_WALL = register(new Block(769, "sandstone_wall", true, 17400, 17723, 2, null));

    public static final Block END_STONE_BRICK_WALL = register(new Block(770, "end_stone_brick_wall", true, 17724, 18047, 2, null));

    public static final Block DIORITE_WALL = register(new Block(771, "diorite_wall", true, 18048, 18371, 14, null));

    public static final Block SCAFFOLDING = register(new Block(772, "scaffolding", true, 18372, 18403, 2, null));

    public static final Block LOOM = register(new Block(773, "loom", true, 18404, 18407, 13, null));

    public static final Block BARREL = register(new Block(774, "barrel", true, 18408, 18419, 13, BlockEntityType.BARREL));

    public static final Block SMOKER = register(new Block(775, "smoker", true, 18420, 18427, 11, BlockEntityType.SMOKER));

    public static final Block BLAST_FURNACE = register(new Block(776, "blast_furnace", true, 18428, 18435, 11, BlockEntityType.BLAST_FURNACE));

    public static final Block CARTOGRAPHY_TABLE = register(new Block(777, "cartography_table", true, 18436, 18436, 13, null));

    public static final Block FLETCHING_TABLE = register(new Block(778, "fletching_table", true, 18437, 18437, 13, null));

    public static final Block GRINDSTONE = register(new Block(779, "grindstone", true, 18438, 18449, 6, null));

    public static final Block LECTERN = register(new Block(780, "lectern", true, 18450, 18465, 13, BlockEntityType.LECTERN));

    public static final Block SMITHING_TABLE = register(new Block(781, "smithing_table", true, 18466, 18466, 13, null));

    public static final Block STONECUTTER = register(new Block(782, "stonecutter", true, 18467, 18470, 11, null));

    public static final Block BELL = register(new Block(783, "bell", true, 18471, 18502, 30, BlockEntityType.BELL));

    public static final Block LANTERN = register(new Block(784, "lantern", true, 18503, 18506, 6, null));

    public static final Block SOUL_LANTERN = register(new Block(785, "soul_lantern", true, 18507, 18510, 6, null));

    public static final Block CAMPFIRE = register(new Block(786, "campfire", true, 18511, 18542, 34, BlockEntityType.CAMPFIRE));

    public static final Block SOUL_CAMPFIRE = register(new Block(787, "soul_campfire", true, 18543, 18574, 34, BlockEntityType.CAMPFIRE));

    public static final Block SWEET_BERRY_BUSH = register(new Block(788, "sweet_berry_bush", false, 18575, 18578, 7, null));

    public static final Block WARPED_STEM = register(new Block(789, "warped_stem", true, 18579, 18581, 56, null));

    public static final Block STRIPPED_WARPED_STEM = register(new Block(790, "stripped_warped_stem", true, 18582, 18584, 56, null));

    public static final Block WARPED_HYPHAE = register(new Block(791, "warped_hyphae", true, 18585, 18587, 57, null));

    public static final Block STRIPPED_WARPED_HYPHAE = register(new Block(792, "stripped_warped_hyphae", true, 18588, 18590, 57, null));

    public static final Block WARPED_NYLIUM = register(new Block(793, "warped_nylium", true, 18591, 18591, 55, null));

    public static final Block WARPED_FUNGUS = register(new Block(794, "warped_fungus", false, 18592, 18592, 23, null));

    public static final Block WARPED_WART_BLOCK = register(new Block(795, "warped_wart_block", true, 18593, 18593, 58, null));

    public static final Block WARPED_ROOTS = register(new Block(796, "warped_roots", false, 18594, 18594, 23, null));

    public static final Block NETHER_SPROUTS = register(new Block(797, "nether_sprouts", false, 18595, 18595, 23, null));

    public static final Block CRIMSON_STEM = register(new Block(798, "crimson_stem", true, 18596, 18598, 53, null));

    public static final Block STRIPPED_CRIMSON_STEM = register(new Block(799, "stripped_crimson_stem", true, 18599, 18601, 53, null));

    public static final Block CRIMSON_HYPHAE = register(new Block(800, "crimson_hyphae", true, 18602, 18604, 54, null));

    public static final Block STRIPPED_CRIMSON_HYPHAE = register(new Block(801, "stripped_crimson_hyphae", true, 18605, 18607, 54, null));

    public static final Block CRIMSON_NYLIUM = register(new Block(802, "crimson_nylium", true, 18608, 18608, 52, null));

    public static final Block CRIMSON_FUNGUS = register(new Block(803, "crimson_fungus", false, 18609, 18609, 35, null));

    public static final Block SHROOMLIGHT = register(new Block(804, "shroomlight", true, 18610, 18610, 28, null));

    public static final Block WEEPING_VINES = register(new Block(805, "weeping_vines", false, 18611, 18636, 35, null));

    public static final Block WEEPING_VINES_PLANT = register(new Block(806, "weeping_vines_plant", false, 18637, 18637, 35, null));

    public static final Block TWISTING_VINES = register(new Block(807, "twisting_vines", false, 18638, 18663, 23, null));

    public static final Block TWISTING_VINES_PLANT = register(new Block(808, "twisting_vines_plant", false, 18664, 18664, 23, null));

    public static final Block CRIMSON_ROOTS = register(new Block(809, "crimson_roots", false, 18665, 18665, 35, null));

    public static final Block CRIMSON_PLANKS = register(new Block(810, "crimson_planks", true, 18666, 18666, 53, null));

    public static final Block WARPED_PLANKS = register(new Block(811, "warped_planks", true, 18667, 18667, 56, null));

    public static final Block CRIMSON_SLAB = register(new Block(812, "crimson_slab", true, 18668, 18673, 53, null));

    public static final Block WARPED_SLAB = register(new Block(813, "warped_slab", true, 18674, 18679, 56, null));

    public static final Block CRIMSON_PRESSURE_PLATE = register(new Block(814, "crimson_pressure_plate", false, 18680, 18681, 53, null));

    public static final Block WARPED_PRESSURE_PLATE = register(new Block(815, "warped_pressure_plate", false, 18682, 18683, 56, null));

    public static final Block CRIMSON_FENCE = register(new Block(816, "crimson_fence", true, 18684, 18715, 53, null));

    public static final Block WARPED_FENCE = register(new Block(817, "warped_fence", true, 18716, 18747, 56, null));

    public static final Block CRIMSON_TRAPDOOR = register(new Block(818, "crimson_trapdoor", true, 18748, 18811, 53, null));

    public static final Block WARPED_TRAPDOOR = register(new Block(819, "warped_trapdoor", true, 18812, 18875, 56, null));

    public static final Block CRIMSON_FENCE_GATE = register(new Block(820, "crimson_fence_gate", true, 18876, 18907, 53, null));

    public static final Block WARPED_FENCE_GATE = register(new Block(821, "warped_fence_gate", true, 18908, 18939, 56, null));

    public static final Block CRIMSON_STAIRS = register(new Block(822, "crimson_stairs", true, 18940, 19019, 53, null));

    public static final Block WARPED_STAIRS = register(new Block(823, "warped_stairs", true, 19020, 19099, 56, null));

    public static final Block CRIMSON_BUTTON = register(new Block(824, "crimson_button", false, 19100, 19123, 0, null));

    public static final Block WARPED_BUTTON = register(new Block(825, "warped_button", false, 19124, 19147, 0, null));

    public static final Block CRIMSON_DOOR = register(new Block(826, "crimson_door", true, 19148, 19211, 53, null));

    public static final Block WARPED_DOOR = register(new Block(827, "warped_door", true, 19212, 19275, 56, null));

    public static final Block CRIMSON_SIGN = register(new Block(828, "crimson_sign", false, 19276, 19307, 53, BlockEntityType.SIGN));

    public static final Block WARPED_SIGN = register(new Block(829, "warped_sign", false, 19308, 19339, 56, BlockEntityType.SIGN));

    public static final Block CRIMSON_WALL_SIGN = register(new Block(830, "crimson_wall_sign", false, 19340, 19347, 53, BlockEntityType.SIGN));

    public static final Block WARPED_WALL_SIGN = register(new Block(831, "warped_wall_sign", false, 19348, 19355, 56, BlockEntityType.SIGN));

    public static final Block STRUCTURE_BLOCK = register(new Block(832, "structure_block", true, 19356, 19359, 22, BlockEntityType.STRUCTURE_BLOCK));

    public static final Block JIGSAW = register(new Block(833, "jigsaw", true, 19360, 19371, 22, BlockEntityType.JIGSAW));

    public static final Block COMPOSTER = register(new Block(834, "composter", true, 19372, 19380, 13, null));

    public static final Block TARGET = register(new Block(835, "target", true, 19381, 19396, 14, null));

    public static final Block BEE_NEST = register(new Block(836, "bee_nest", true, 19397, 19420, 18, BlockEntityType.BEEHIVE));

    public static final Block BEEHIVE = register(new Block(837, "beehive", true, 19421, 19444, 13, BlockEntityType.BEEHIVE));

    public static final Block HONEY_BLOCK = register(new Block(838, "honey_block", true, 19445, 19445, 15, null));

    public static final Block HONEYCOMB_BLOCK = register(new Block(839, "honeycomb_block", true, 19446, 19446, 15, null));

    public static final Block NETHERITE_BLOCK = register(new Block(840, "netherite_block", true, 19447, 19447, 29, null));

    public static final Block ANCIENT_DEBRIS = register(new Block(841, "ancient_debris", true, 19448, 19448, 29, null));

    public static final Block CRYING_OBSIDIAN = register(new Block(842, "crying_obsidian", true, 19449, 19449, 29, null));

    public static final Block RESPAWN_ANCHOR = register(new Block(843, "respawn_anchor", true, 19450, 19454, 29, null));

    public static final Block POTTED_CRIMSON_FUNGUS = register(new Block(844, "potted_crimson_fungus", true, 19455, 19455, 0, null));

    public static final Block POTTED_WARPED_FUNGUS = register(new Block(845, "potted_warped_fungus", true, 19456, 19456, 0, null));

    public static final Block POTTED_CRIMSON_ROOTS = register(new Block(846, "potted_crimson_roots", true, 19457, 19457, 0, null));

    public static final Block POTTED_WARPED_ROOTS = register(new Block(847, "potted_warped_roots", true, 19458, 19458, 0, null));

    public static final Block LODESTONE = register(new Block(848, "lodestone", true, 19459, 19459, 6, null));

    public static final Block BLACKSTONE = register(new Block(849, "blackstone", true, 19460, 19460, 29, null));

    public static final Block BLACKSTONE_STAIRS = register(new Block(850, "blackstone_stairs", true, 19461, 19540, 29, null));

    public static final Block BLACKSTONE_WALL = register(new Block(851, "blackstone_wall", true, 19541, 19864, 29, null));

    public static final Block BLACKSTONE_SLAB = register(new Block(852, "blackstone_slab", true, 19865, 19870, 29, null));

    public static final Block POLISHED_BLACKSTONE = register(new Block(853, "polished_blackstone", true, 19871, 19871, 29, null));

    public static final Block POLISHED_BLACKSTONE_BRICKS = register(new Block(854, "polished_blackstone_bricks", true, 19872, 19872, 29, null));

    public static final Block CRACKED_POLISHED_BLACKSTONE_BRICKS = register(new Block(855, "cracked_polished_blackstone_bricks", true, 19873, 19873, 29, null));

    public static final Block CHISELED_POLISHED_BLACKSTONE = register(new Block(856, "chiseled_polished_blackstone", true, 19874, 19874, 29, null));

    public static final Block POLISHED_BLACKSTONE_BRICK_SLAB = register(new Block(857, "polished_blackstone_brick_slab", true, 19875, 19880, 29, null));

    public static final Block POLISHED_BLACKSTONE_BRICK_STAIRS = register(new Block(858, "polished_blackstone_brick_stairs", true, 19881, 19960, 29, null));

    public static final Block POLISHED_BLACKSTONE_BRICK_WALL = register(new Block(859, "polished_blackstone_brick_wall", true, 19961, 20284, 29, null));

    public static final Block GILDED_BLACKSTONE = register(new Block(860, "gilded_blackstone", true, 20285, 20285, 29, null));

    public static final Block POLISHED_BLACKSTONE_STAIRS = register(new Block(861, "polished_blackstone_stairs", true, 20286, 20365, 29, null));

    public static final Block POLISHED_BLACKSTONE_SLAB = register(new Block(862, "polished_blackstone_slab", true, 20366, 20371, 29, null));

    public static final Block POLISHED_BLACKSTONE_PRESSURE_PLATE = register(new Block(863, "polished_blackstone_pressure_plate", false, 20372, 20373, 29, null));

    public static final Block POLISHED_BLACKSTONE_BUTTON = register(new Block(864, "polished_blackstone_button", false, 20374, 20397, 0, null));

    public static final Block POLISHED_BLACKSTONE_WALL = register(new Block(865, "polished_blackstone_wall", true, 20398, 20721, 29, null));

    public static final Block CHISELED_NETHER_BRICKS = register(new Block(866, "chiseled_nether_bricks", true, 20722, 20722, 35, null));

    public static final Block CRACKED_NETHER_BRICKS = register(new Block(867, "cracked_nether_bricks", true, 20723, 20723, 35, null));

    public static final Block QUARTZ_BRICKS = register(new Block(868, "quartz_bricks", true, 20724, 20724, 14, null));

    public static final Block CANDLE = register(new Block(869, "candle", true, 20725, 20740, 2, null));

    public static final Block WHITE_CANDLE = register(new Block(870, "white_candle", true, 20741, 20756, 3, null));

    public static final Block ORANGE_CANDLE = register(new Block(871, "orange_candle", true, 20757, 20772, 15, null));

    public static final Block MAGENTA_CANDLE = register(new Block(872, "magenta_candle", true, 20773, 20788, 16, null));

    public static final Block LIGHT_BLUE_CANDLE = register(new Block(873, "light_blue_candle", true, 20789, 20804, 17, null));

    public static final Block YELLOW_CANDLE = register(new Block(874, "yellow_candle", true, 20805, 20820, 18, null));

    public static final Block LIME_CANDLE = register(new Block(875, "lime_candle", true, 20821, 20836, 19, null));

    public static final Block PINK_CANDLE = register(new Block(876, "pink_candle", true, 20837, 20852, 20, null));

    public static final Block GRAY_CANDLE = register(new Block(877, "gray_candle", true, 20853, 20868, 21, null));

    public static final Block LIGHT_GRAY_CANDLE = register(new Block(878, "light_gray_candle", true, 20869, 20884, 22, null));

    public static final Block CYAN_CANDLE = register(new Block(879, "cyan_candle", true, 20885, 20900, 23, null));

    public static final Block PURPLE_CANDLE = register(new Block(880, "purple_candle", true, 20901, 20916, 24, null));

    public static final Block BLUE_CANDLE = register(new Block(881, "blue_candle", true, 20917, 20932, 25, null));

    public static final Block BROWN_CANDLE = register(new Block(882, "brown_candle", true, 20933, 20948, 26, null));

    public static final Block GREEN_CANDLE = register(new Block(883, "green_candle", true, 20949, 20964, 27, null));

    public static final Block RED_CANDLE = register(new Block(884, "red_candle", true, 20965, 20980, 28, null));

    public static final Block BLACK_CANDLE = register(new Block(885, "black_candle", true, 20981, 20996, 29, null));

    public static final Block CANDLE_CAKE = register(new Block(886, "candle_cake", true, 20997, 20998, 0, null));

    public static final Block WHITE_CANDLE_CAKE = register(new Block(887, "white_candle_cake", true, 20999, 21000, 0, null));

    public static final Block ORANGE_CANDLE_CAKE = register(new Block(888, "orange_candle_cake", true, 21001, 21002, 0, null));

    public static final Block MAGENTA_CANDLE_CAKE = register(new Block(889, "magenta_candle_cake", true, 21003, 21004, 0, null));

    public static final Block LIGHT_BLUE_CANDLE_CAKE = register(new Block(890, "light_blue_candle_cake", true, 21005, 21006, 0, null));

    public static final Block YELLOW_CANDLE_CAKE = register(new Block(891, "yellow_candle_cake", true, 21007, 21008, 0, null));

    public static final Block LIME_CANDLE_CAKE = register(new Block(892, "lime_candle_cake", true, 21009, 21010, 0, null));

    public static final Block PINK_CANDLE_CAKE = register(new Block(893, "pink_candle_cake", true, 21011, 21012, 0, null));

    public static final Block GRAY_CANDLE_CAKE = register(new Block(894, "gray_candle_cake", true, 21013, 21014, 0, null));

    public static final Block LIGHT_GRAY_CANDLE_CAKE = register(new Block(895, "light_gray_candle_cake", true, 21015, 21016, 0, null));

    public static final Block CYAN_CANDLE_CAKE = register(new Block(896, "cyan_candle_cake", true, 21017, 21018, 0, null));

    public static final Block PURPLE_CANDLE_CAKE = register(new Block(897, "purple_candle_cake", true, 21019, 21020, 0, null));

    public static final Block BLUE_CANDLE_CAKE = register(new Block(898, "blue_candle_cake", true, 21021, 21022, 0, null));

    public static final Block BROWN_CANDLE_CAKE = register(new Block(899, "brown_candle_cake", true, 21023, 21024, 0, null));

    public static final Block GREEN_CANDLE_CAKE = register(new Block(900, "green_candle_cake", true, 21025, 21026, 0, null));

    public static final Block RED_CANDLE_CAKE = register(new Block(901, "red_candle_cake", true, 21027, 21028, 0, null));

    public static final Block BLACK_CANDLE_CAKE = register(new Block(902, "black_candle_cake", true, 21029, 21030, 0, null));

    public static final Block AMETHYST_BLOCK = register(new Block(903, "amethyst_block", true, 21031, 21031, 24, null));

    public static final Block BUDDING_AMETHYST = register(new Block(904, "budding_amethyst", true, 21032, 21032, 24, null));

    public static final Block AMETHYST_CLUSTER = register(new Block(905, "amethyst_cluster", true, 21033, 21044, 24, null));

    public static final Block LARGE_AMETHYST_BUD = register(new Block(906, "large_amethyst_bud", true, 21045, 21056, 24, null));

    public static final Block MEDIUM_AMETHYST_BUD = register(new Block(907, "medium_amethyst_bud", true, 21057, 21068, 24, null));

    public static final Block SMALL_AMETHYST_BUD = register(new Block(908, "small_amethyst_bud", true, 21069, 21080, 24, null));

    public static final Block TUFF = register(new Block(909, "tuff", true, 21081, 21081, 43, null));

    public static final Block TUFF_SLAB = register(new Block(910, "tuff_slab", true, 21082, 21087, 43, null));

    public static final Block TUFF_STAIRS = register(new Block(911, "tuff_stairs", true, 21088, 21167, 43, null));

    public static final Block TUFF_WALL = register(new Block(912, "tuff_wall", true, 21168, 21491, 43, null));

    public static final Block POLISHED_TUFF = register(new Block(913, "polished_tuff", true, 21492, 21492, 43, null));

    public static final Block POLISHED_TUFF_SLAB = register(new Block(914, "polished_tuff_slab", true, 21493, 21498, 43, null));

    public static final Block POLISHED_TUFF_STAIRS = register(new Block(915, "polished_tuff_stairs", true, 21499, 21578, 43, null));

    public static final Block POLISHED_TUFF_WALL = register(new Block(916, "polished_tuff_wall", true, 21579, 21902, 43, null));

    public static final Block CHISELED_TUFF = register(new Block(917, "chiseled_tuff", true, 21903, 21903, 43, null));

    public static final Block TUFF_BRICKS = register(new Block(918, "tuff_bricks", true, 21904, 21904, 43, null));

    public static final Block TUFF_BRICK_SLAB = register(new Block(919, "tuff_brick_slab", true, 21905, 21910, 43, null));

    public static final Block TUFF_BRICK_STAIRS = register(new Block(920, "tuff_brick_stairs", true, 21911, 21990, 43, null));

    public static final Block TUFF_BRICK_WALL = register(new Block(921, "tuff_brick_wall", true, 21991, 22314, 43, null));

    public static final Block CHISELED_TUFF_BRICKS = register(new Block(922, "chiseled_tuff_bricks", true, 22315, 22315, 43, null));

    public static final Block CALCITE = register(new Block(923, "calcite", true, 22316, 22316, 36, null));

    public static final Block TINTED_GLASS = register(new Block(924, "tinted_glass", true, 22317, 22317, 21, null));

    public static final Block POWDER_SNOW = register(new Block(925, "powder_snow", false, 22318, 22318, 8, null));

    public static final Block SCULK_SENSOR = register(new Block(926, "sculk_sensor", true, 22319, 22414, 23, BlockEntityType.SCULK_SENSOR));

    public static final Block CALIBRATED_SCULK_SENSOR = register(new Block(927, "calibrated_sculk_sensor", true, 22415, 22798, 23, BlockEntityType.CALIBRATED_SCULK_SENSOR));

    public static final Block SCULK = register(new Block(928, "sculk", true, 22799, 22799, 29, null));

    public static final Block SCULK_VEIN = register(new Block(929, "sculk_vein", false, 22800, 22927, 29, null));

    public static final Block SCULK_CATALYST = register(new Block(930, "sculk_catalyst", true, 22928, 22929, 29, BlockEntityType.SCULK_CATALYST));

    public static final Block SCULK_SHRIEKER = register(new Block(931, "sculk_shrieker", true, 22930, 22937, 29, BlockEntityType.SCULK_SHRIEKER));

    public static final Block COPPER_BLOCK = register(new Block(932, "copper_block", true, 22938, 22938, 15, null));

    public static final Block EXPOSED_COPPER = register(new Block(933, "exposed_copper", true, 22939, 22939, 44, null));

    public static final Block WEATHERED_COPPER = register(new Block(934, "weathered_copper", true, 22940, 22940, 56, null));

    public static final Block OXIDIZED_COPPER = register(new Block(935, "oxidized_copper", true, 22941, 22941, 55, null));

    public static final Block COPPER_ORE = register(new Block(936, "copper_ore", true, 22942, 22942, 11, null));

    public static final Block DEEPSLATE_COPPER_ORE = register(new Block(937, "deepslate_copper_ore", true, 22943, 22943, 59, null));

    public static final Block OXIDIZED_CUT_COPPER = register(new Block(938, "oxidized_cut_copper", true, 22944, 22944, 55, null));

    public static final Block WEATHERED_CUT_COPPER = register(new Block(939, "weathered_cut_copper", true, 22945, 22945, 56, null));

    public static final Block EXPOSED_CUT_COPPER = register(new Block(940, "exposed_cut_copper", true, 22946, 22946, 44, null));

    public static final Block CUT_COPPER = register(new Block(941, "cut_copper", true, 22947, 22947, 15, null));

    public static final Block OXIDIZED_CHISELED_COPPER = register(new Block(942, "oxidized_chiseled_copper", true, 22948, 22948, 55, null));

    public static final Block WEATHERED_CHISELED_COPPER = register(new Block(943, "weathered_chiseled_copper", true, 22949, 22949, 56, null));

    public static final Block EXPOSED_CHISELED_COPPER = register(new Block(944, "exposed_chiseled_copper", true, 22950, 22950, 44, null));

    public static final Block CHISELED_COPPER = register(new Block(945, "chiseled_copper", true, 22951, 22951, 15, null));

    public static final Block WAXED_OXIDIZED_CHISELED_COPPER = register(new Block(946, "waxed_oxidized_chiseled_copper", true, 22952, 22952, 55, null));

    public static final Block WAXED_WEATHERED_CHISELED_COPPER = register(new Block(947, "waxed_weathered_chiseled_copper", true, 22953, 22953, 56, null));

    public static final Block WAXED_EXPOSED_CHISELED_COPPER = register(new Block(948, "waxed_exposed_chiseled_copper", true, 22954, 22954, 44, null));

    public static final Block WAXED_CHISELED_COPPER = register(new Block(949, "waxed_chiseled_copper", true, 22955, 22955, 15, null));

    public static final Block OXIDIZED_CUT_COPPER_STAIRS = register(new Block(950, "oxidized_cut_copper_stairs", true, 22956, 23035, 55, null));

    public static final Block WEATHERED_CUT_COPPER_STAIRS = register(new Block(951, "weathered_cut_copper_stairs", true, 23036, 23115, 56, null));

    public static final Block EXPOSED_CUT_COPPER_STAIRS = register(new Block(952, "exposed_cut_copper_stairs", true, 23116, 23195, 44, null));

    public static final Block CUT_COPPER_STAIRS = register(new Block(953, "cut_copper_stairs", true, 23196, 23275, 15, null));

    public static final Block OXIDIZED_CUT_COPPER_SLAB = register(new Block(954, "oxidized_cut_copper_slab", true, 23276, 23281, 55, null));

    public static final Block WEATHERED_CUT_COPPER_SLAB = register(new Block(955, "weathered_cut_copper_slab", true, 23282, 23287, 56, null));

    public static final Block EXPOSED_CUT_COPPER_SLAB = register(new Block(956, "exposed_cut_copper_slab", true, 23288, 23293, 44, null));

    public static final Block CUT_COPPER_SLAB = register(new Block(957, "cut_copper_slab", true, 23294, 23299, 15, null));

    public static final Block WAXED_COPPER_BLOCK = register(new Block(958, "waxed_copper_block", true, 23300, 23300, 15, null));

    public static final Block WAXED_WEATHERED_COPPER = register(new Block(959, "waxed_weathered_copper", true, 23301, 23301, 56, null));

    public static final Block WAXED_EXPOSED_COPPER = register(new Block(960, "waxed_exposed_copper", true, 23302, 23302, 44, null));

    public static final Block WAXED_OXIDIZED_COPPER = register(new Block(961, "waxed_oxidized_copper", true, 23303, 23303, 55, null));

    public static final Block WAXED_OXIDIZED_CUT_COPPER = register(new Block(962, "waxed_oxidized_cut_copper", true, 23304, 23304, 55, null));

    public static final Block WAXED_WEATHERED_CUT_COPPER = register(new Block(963, "waxed_weathered_cut_copper", true, 23305, 23305, 56, null));

    public static final Block WAXED_EXPOSED_CUT_COPPER = register(new Block(964, "waxed_exposed_cut_copper", true, 23306, 23306, 44, null));

    public static final Block WAXED_CUT_COPPER = register(new Block(965, "waxed_cut_copper", true, 23307, 23307, 15, null));

    public static final Block WAXED_OXIDIZED_CUT_COPPER_STAIRS = register(new Block(966, "waxed_oxidized_cut_copper_stairs", true, 23308, 23387, 55, null));

    public static final Block WAXED_WEATHERED_CUT_COPPER_STAIRS = register(new Block(967, "waxed_weathered_cut_copper_stairs", true, 23388, 23467, 56, null));

    public static final Block WAXED_EXPOSED_CUT_COPPER_STAIRS = register(new Block(968, "waxed_exposed_cut_copper_stairs", true, 23468, 23547, 44, null));

    public static final Block WAXED_CUT_COPPER_STAIRS = register(new Block(969, "waxed_cut_copper_stairs", true, 23548, 23627, 15, null));

    public static final Block WAXED_OXIDIZED_CUT_COPPER_SLAB = register(new Block(970, "waxed_oxidized_cut_copper_slab", true, 23628, 23633, 55, null));

    public static final Block WAXED_WEATHERED_CUT_COPPER_SLAB = register(new Block(971, "waxed_weathered_cut_copper_slab", true, 23634, 23639, 56, null));

    public static final Block WAXED_EXPOSED_CUT_COPPER_SLAB = register(new Block(972, "waxed_exposed_cut_copper_slab", true, 23640, 23645, 44, null));

    public static final Block WAXED_CUT_COPPER_SLAB = register(new Block(973, "waxed_cut_copper_slab", true, 23646, 23651, 15, null));

    public static final Block COPPER_DOOR = register(new Block(974, "copper_door", true, 23652, 23715, 15, null));

    public static final Block EXPOSED_COPPER_DOOR = register(new Block(975, "exposed_copper_door", true, 23716, 23779, 44, null));

    public static final Block OXIDIZED_COPPER_DOOR = register(new Block(976, "oxidized_copper_door", true, 23780, 23843, 55, null));

    public static final Block WEATHERED_COPPER_DOOR = register(new Block(977, "weathered_copper_door", true, 23844, 23907, 56, null));

    public static final Block WAXED_COPPER_DOOR = register(new Block(978, "waxed_copper_door", true, 23908, 23971, 15, null));

    public static final Block WAXED_EXPOSED_COPPER_DOOR = register(new Block(979, "waxed_exposed_copper_door", true, 23972, 24035, 44, null));

    public static final Block WAXED_OXIDIZED_COPPER_DOOR = register(new Block(980, "waxed_oxidized_copper_door", true, 24036, 24099, 55, null));

    public static final Block WAXED_WEATHERED_COPPER_DOOR = register(new Block(981, "waxed_weathered_copper_door", true, 24100, 24163, 56, null));

    public static final Block COPPER_TRAPDOOR = register(new Block(982, "copper_trapdoor", true, 24164, 24227, 15, null));

    public static final Block EXPOSED_COPPER_TRAPDOOR = register(new Block(983, "exposed_copper_trapdoor", true, 24228, 24291, 44, null));

    public static final Block OXIDIZED_COPPER_TRAPDOOR = register(new Block(984, "oxidized_copper_trapdoor", true, 24292, 24355, 55, null));

    public static final Block WEATHERED_COPPER_TRAPDOOR = register(new Block(985, "weathered_copper_trapdoor", true, 24356, 24419, 56, null));

    public static final Block WAXED_COPPER_TRAPDOOR = register(new Block(986, "waxed_copper_trapdoor", true, 24420, 24483, 15, null));

    public static final Block WAXED_EXPOSED_COPPER_TRAPDOOR = register(new Block(987, "waxed_exposed_copper_trapdoor", true, 24484, 24547, 44, null));

    public static final Block WAXED_OXIDIZED_COPPER_TRAPDOOR = register(new Block(988, "waxed_oxidized_copper_trapdoor", true, 24548, 24611, 55, null));

    public static final Block WAXED_WEATHERED_COPPER_TRAPDOOR = register(new Block(989, "waxed_weathered_copper_trapdoor", true, 24612, 24675, 56, null));

    public static final Block COPPER_GRATE = register(new Block(990, "copper_grate", true, 24676, 24677, 15, null));

    public static final Block EXPOSED_COPPER_GRATE = register(new Block(991, "exposed_copper_grate", true, 24678, 24679, 44, null));

    public static final Block WEATHERED_COPPER_GRATE = register(new Block(992, "weathered_copper_grate", true, 24680, 24681, 56, null));

    public static final Block OXIDIZED_COPPER_GRATE = register(new Block(993, "oxidized_copper_grate", true, 24682, 24683, 55, null));

    public static final Block WAXED_COPPER_GRATE = register(new Block(994, "waxed_copper_grate", true, 24684, 24685, 15, null));

    public static final Block WAXED_EXPOSED_COPPER_GRATE = register(new Block(995, "waxed_exposed_copper_grate", true, 24686, 24687, 44, null));

    public static final Block WAXED_WEATHERED_COPPER_GRATE = register(new Block(996, "waxed_weathered_copper_grate", true, 24688, 24689, 56, null));

    public static final Block WAXED_OXIDIZED_COPPER_GRATE = register(new Block(997, "waxed_oxidized_copper_grate", true, 24690, 24691, 55, null));

    public static final Block COPPER_BULB = register(new Block(998, "copper_bulb", true, 24692, 24695, 15, null));

    public static final Block EXPOSED_COPPER_BULB = register(new Block(999, "exposed_copper_bulb", true, 24696, 24699, 44, null));

    public static final Block WEATHERED_COPPER_BULB = register(new Block(1000, "weathered_copper_bulb", true, 24700, 24703, 56, null));

    public static final Block OXIDIZED_COPPER_BULB = register(new Block(1001, "oxidized_copper_bulb", true, 24704, 24707, 55, null));

    public static final Block WAXED_COPPER_BULB = register(new Block(1002, "waxed_copper_bulb", true, 24708, 24711, 15, null));

    public static final Block WAXED_EXPOSED_COPPER_BULB = register(new Block(1003, "waxed_exposed_copper_bulb", true, 24712, 24715, 44, null));

    public static final Block WAXED_WEATHERED_COPPER_BULB = register(new Block(1004, "waxed_weathered_copper_bulb", true, 24716, 24719, 56, null));

    public static final Block WAXED_OXIDIZED_COPPER_BULB = register(new Block(1005, "waxed_oxidized_copper_bulb", true, 24720, 24723, 55, null));

    public static final Block LIGHTNING_ROD = register(new Block(1006, "lightning_rod", true, 24724, 24747, 15, null));

    public static final Block POINTED_DRIPSTONE = register(new Block(1007, "pointed_dripstone", true, 24748, 24767, 48, null));

    public static final Block DRIPSTONE_BLOCK = register(new Block(1008, "dripstone_block", true, 24768, 24768, 48, null));

    public static final Block CAVE_VINES = register(new Block(1009, "cave_vines", false, 24769, 24820, 7, null));

    public static final Block CAVE_VINES_PLANT = register(new Block(1010, "cave_vines_plant", false, 24821, 24822, 7, null));

    public static final Block SPORE_BLOSSOM = register(new Block(1011, "spore_blossom", false, 24823, 24823, 7, null));

    public static final Block AZALEA = register(new Block(1012, "azalea", true, 24824, 24824, 7, null));

    public static final Block FLOWERING_AZALEA = register(new Block(1013, "flowering_azalea", true, 24825, 24825, 7, null));

    public static final Block MOSS_CARPET = register(new Block(1014, "moss_carpet", true, 24826, 24826, 27, null));

    public static final Block PINK_PETALS = register(new Block(1015, "pink_petals", false, 24827, 24842, 7, null));

    public static final Block MOSS_BLOCK = register(new Block(1016, "moss_block", true, 24843, 24843, 27, null));

    public static final Block BIG_DRIPLEAF = register(new Block(1017, "big_dripleaf", true, 24844, 24875, 7, null));

    public static final Block BIG_DRIPLEAF_STEM = register(new Block(1018, "big_dripleaf_stem", false, 24876, 24883, 7, null));

    public static final Block SMALL_DRIPLEAF = register(new Block(1019, "small_dripleaf", false, 24884, 24899, 7, null));

    public static final Block HANGING_ROOTS = register(new Block(1020, "hanging_roots", false, 24900, 24901, 10, null));

    public static final Block ROOTED_DIRT = register(new Block(1021, "rooted_dirt", true, 24902, 24902, 10, null));

    public static final Block MUD = register(new Block(1022, "mud", true, 24903, 24903, 45, null));

    public static final Block DEEPSLATE = register(new Block(1023, "deepslate", true, 24904, 24906, 59, null));

    public static final Block COBBLED_DEEPSLATE = register(new Block(1024, "cobbled_deepslate", true, 24907, 24907, 59, null));

    public static final Block COBBLED_DEEPSLATE_STAIRS = register(new Block(1025, "cobbled_deepslate_stairs", true, 24908, 24987, 59, null));

    public static final Block COBBLED_DEEPSLATE_SLAB = register(new Block(1026, "cobbled_deepslate_slab", true, 24988, 24993, 59, null));

    public static final Block COBBLED_DEEPSLATE_WALL = register(new Block(1027, "cobbled_deepslate_wall", true, 24994, 25317, 59, null));

    public static final Block POLISHED_DEEPSLATE = register(new Block(1028, "polished_deepslate", true, 25318, 25318, 59, null));

    public static final Block POLISHED_DEEPSLATE_STAIRS = register(new Block(1029, "polished_deepslate_stairs", true, 25319, 25398, 59, null));

    public static final Block POLISHED_DEEPSLATE_SLAB = register(new Block(1030, "polished_deepslate_slab", true, 25399, 25404, 59, null));

    public static final Block POLISHED_DEEPSLATE_WALL = register(new Block(1031, "polished_deepslate_wall", true, 25405, 25728, 59, null));

    public static final Block DEEPSLATE_TILES = register(new Block(1032, "deepslate_tiles", true, 25729, 25729, 59, null));

    public static final Block DEEPSLATE_TILE_STAIRS = register(new Block(1033, "deepslate_tile_stairs", true, 25730, 25809, 59, null));

    public static final Block DEEPSLATE_TILE_SLAB = register(new Block(1034, "deepslate_tile_slab", true, 25810, 25815, 59, null));

    public static final Block DEEPSLATE_TILE_WALL = register(new Block(1035, "deepslate_tile_wall", true, 25816, 26139, 59, null));

    public static final Block DEEPSLATE_BRICKS = register(new Block(1036, "deepslate_bricks", true, 26140, 26140, 59, null));

    public static final Block DEEPSLATE_BRICK_STAIRS = register(new Block(1037, "deepslate_brick_stairs", true, 26141, 26220, 59, null));

    public static final Block DEEPSLATE_BRICK_SLAB = register(new Block(1038, "deepslate_brick_slab", true, 26221, 26226, 59, null));

    public static final Block DEEPSLATE_BRICK_WALL = register(new Block(1039, "deepslate_brick_wall", true, 26227, 26550, 59, null));

    public static final Block CHISELED_DEEPSLATE = register(new Block(1040, "chiseled_deepslate", true, 26551, 26551, 59, null));

    public static final Block CRACKED_DEEPSLATE_BRICKS = register(new Block(1041, "cracked_deepslate_bricks", true, 26552, 26552, 59, null));

    public static final Block CRACKED_DEEPSLATE_TILES = register(new Block(1042, "cracked_deepslate_tiles", true, 26553, 26553, 59, null));

    public static final Block INFESTED_DEEPSLATE = register(new Block(1043, "infested_deepslate", true, 26554, 26556, 59, null));

    public static final Block SMOOTH_BASALT = register(new Block(1044, "smooth_basalt", true, 26557, 26557, 29, null));

    public static final Block RAW_IRON_BLOCK = register(new Block(1045, "raw_iron_block", true, 26558, 26558, 60, null));

    public static final Block RAW_COPPER_BLOCK = register(new Block(1046, "raw_copper_block", true, 26559, 26559, 15, null));

    public static final Block RAW_GOLD_BLOCK = register(new Block(1047, "raw_gold_block", true, 26560, 26560, 30, null));

    public static final Block POTTED_AZALEA_BUSH = register(new Block(1048, "potted_azalea_bush", true, 26561, 26561, 0, null));

    public static final Block POTTED_FLOWERING_AZALEA_BUSH = register(new Block(1049, "potted_flowering_azalea_bush", true, 26562, 26562, 0, null));

    public static final Block OCHRE_FROGLIGHT = register(new Block(1050, "ochre_froglight", true, 26563, 26565, 2, null));

    public static final Block VERDANT_FROGLIGHT = register(new Block(1051, "verdant_froglight", true, 26566, 26568, 61, null));

    public static final Block PEARLESCENT_FROGLIGHT = register(new Block(1052, "pearlescent_froglight", true, 26569, 26571, 20, null));

    public static final Block FROGSPAWN = register(new Block(1053, "frogspawn", false, 26572, 26572, 12, null));

    public static final Block REINFORCED_DEEPSLATE = register(new Block(1054, "reinforced_deepslate", true, 26573, 26573, 59, null));

    public static final Block DECORATED_POT = register(new Block(1055, "decorated_pot", true, 26574, 26589, 50, BlockEntityType.DECORATED_POT));

    public static final Block CRAFTER = register(new Block(1056, "crafter", true, 26590, 26637, 11, BlockEntityType.CRAFTER));

    public static final Block TRIAL_SPAWNER = register(new Block(1057, "trial_spawner", true, 26638, 26649, 11, BlockEntityType.TRIAL_SPAWNER));

    public static final Block VAULT = register(new Block(1058, "vault", true, 26650, 26681, 11, BlockEntityType.VAULT));

    public static final Block HEAVY_CORE = register(new Block(1059, "heavy_core", true, 26682, 26683, 6, null));

    private static Block register(Block value) {
        REGISTRY.put(value.id(), value);
        return value;
    }
}
