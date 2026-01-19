package com.zenith.mc.item;

import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;

import java.util.EnumMap;
import java.util.List;

public class ContainerTypeInfoRegistry {

    public record ContainerTypeInfo(
        String ascii,
        int totalSlots,
        int topSlots,
        int bottomSlots
    ) { }

    public static final EnumMap<ContainerType, ContainerTypeInfo> REGISTRY = new EnumMap<>(ContainerType.class);

    private static void register(ContainerType containerType, String topAscii, int topSlots, int bottomSlots) {
        var ascii = generateAscii(topAscii, topSlots, bottomSlots);
        int totalSlots = topSlots + 36 + bottomSlots;
        ContainerTypeInfo containerTypeInfo = new ContainerTypeInfo(ascii, totalSlots, topSlots, bottomSlots);
        REGISTRY.put(containerType, containerTypeInfo);
    }

    private static void register(ContainerType containerType, String topAscii, int topSlots) {
        register(containerType, topAscii, topSlots, 0);
    }

    private static void register(List<ContainerType> containerTypes, String topAscii, int topSlots, int bottomSlots) {
        var ascii = generateAscii(topAscii, topSlots, bottomSlots);
        int totalSlots = topSlots + 36 + bottomSlots;
        ContainerTypeInfo containerTypeInfo = new ContainerTypeInfo(ascii, totalSlots, topSlots, bottomSlots);
        containerTypes.forEach(type -> REGISTRY.put(type, containerTypeInfo));
    }

    private static void register(List<ContainerType> containerTypes, String topAscii, int topSlots) {
        register(containerTypes, topAscii, topSlots, 0);
    }

    public static final String playerInventoryAscii =
        """
        ```
        ╔═══╦═══════════╗
        ║%6$2s ║    ███    ║   ╔═══╦═══╗
        ╠═══╣    ███    ║   ║%2$2s ║%3$2s ║   ╔═══╗
        ║%7$2s ║  ███████  ║   ╠═══╬═══╣   ║%1$2s ║
        ╠═══╣  ███████  ║   ║%4$2s ║%5$2s ║   ╚═══╝
        ║%8$2s ║  ███████  ║   ╚═══╩═══╝
        ╠═══╣    ███    ╠═══╗
        ║%9$2s ║    ███    ║%46$2s ║
        ╚═══╩═══════════╩═══╝
        ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
        ║%10$2s ║%11$2s ║%12$2s ║%13$2s ║%14$2s ║%15$2s ║%16$2s ║%17$2s ║%18$2s ║
        ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
        ║%19$2s ║%20$2s ║%21$2s ║%22$2s ║%23$2s ║%24$2s ║%25$2s ║%26$2s ║%27$2s ║
        ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
        ║%28$2s ║%29$2s ║%30$2s ║%31$2s ║%32$2s ║%33$2s ║%34$2s ║%35$2s ║%36$2s ║
        ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
        ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
        ║%37$2s ║%38$2s ║%39$2s ║%40$2s ║%41$2s ║%42$2s ║%43$2s ║%44$2s ║%45$2s ║
        ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
        ```
        """;

    static {
        register(
            ContainerType.GENERIC_9X1,
            """
            ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
            ║%1$2s ║%2$2s ║%3$2s ║%4$2s ║%5$2s ║%6$2s ║%7$2s ║%8$2s ║%9$2s ║
            ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
            """,
            9
        );
        register(
            ContainerType.GENERIC_9X2,
            """
            ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
            ║%1$2s ║%2$2s ║%3$2s ║%4$2s ║%5$2s ║%6$2s ║%7$2s ║%8$2s ║%9$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%10$2s ║%11$2s ║%12$2s ║%13$2s ║%14$2s ║%15$2s ║%16$2s ║%17$2s ║%18$2s ║
            ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
            """,
            18
        );
        register(
            List.of(ContainerType.GENERIC_9X3, ContainerType.SHULKER_BOX),
            """
            ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
            ║%1$2s ║%2$2s ║%3$2s ║%4$2s ║%5$2s ║%6$2s ║%7$2s ║%8$2s ║%9$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%10$2s ║%11$2s ║%12$2s ║%13$2s ║%14$2s ║%15$2s ║%16$2s ║%17$2s ║%18$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%19$2s ║%20$2s ║%21$2s ║%22$2s ║%23$2s ║%24$2s ║%25$2s ║%26$2s ║%27$2s ║
            ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
            """,
            27
        );
        register(
            ContainerType.GENERIC_9X4,
            """
            ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
            ║%1$2s ║%2$2s ║%3$2s ║%4$2s ║%5$2s ║%6$2s ║%7$2s ║%8$2s ║%9$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%10$2s ║%11$2s ║%12$2s ║%13$2s ║%14$2s ║%15$2s ║%16$2s ║%17$2s ║%18$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%19$2s ║%20$2s ║%21$2s ║%22$2s ║%23$2s ║%24$2s ║%25$2s ║%26$2s ║%27$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%28$2s ║%29$2s ║%30$2s ║%31$2s ║%32$2s ║%33$2s ║%34$2s ║%35$2s ║%36$2s ║
            ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
            """,
            36
        );
        register(
            ContainerType.GENERIC_9X5,
            """
            ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
            ║%1$2s ║%2$2s ║%3$2s ║%4$2s ║%5$2s ║%6$2s ║%7$2s ║%8$2s ║%9$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%10$2s ║%11$2s ║%12$2s ║%13$2s ║%14$2s ║%15$2s ║%16$2s ║%17$2s ║%18$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%19$2s ║%20$2s ║%21$2s ║%22$2s ║%23$2s ║%24$2s ║%25$2s ║%26$2s ║%27$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%28$2s ║%29$2s ║%30$2s ║%31$2s ║%32$2s ║%33$2s ║%34$2s ║%35$2s ║%36$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%37$2s ║%38$2s ║%39$2s ║%40$2s ║%41$2s ║%42$2s ║%43$2s ║%44$2s ║%45$2s ║
            ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
            """,
            45
        );
        register(
            ContainerType.GENERIC_9X6,
            """
            ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
            ║%1$2s ║%2$2s ║%3$2s ║%4$2s ║%5$2s ║%6$2s ║%7$2s ║%8$2s ║%9$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%10$2s ║%11$2s ║%12$2s ║%13$2s ║%14$2s ║%15$2s ║%16$2s ║%17$2s ║%18$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%19$2s ║%20$2s ║%21$2s ║%22$2s ║%23$2s ║%24$2s ║%25$2s ║%26$2s ║%27$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%28$2s ║%29$2s ║%30$2s ║%31$2s ║%32$2s ║%33$2s ║%34$2s ║%35$2s ║%36$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%37$2s ║%38$2s ║%39$2s ║%40$2s ║%41$2s ║%42$2s ║%43$2s ║%44$2s ║%45$2s ║
            ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
            ║%46$2s ║%47$2s ║%48$2s ║%49$2s ║%50$2s ║%51$2s ║%52$2s ║%53$2s ║%54$2s ║
            ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
            """,
            54
        );
        register(
            ContainerType.GENERIC_3X3,
            """
            ╔═══╦═══╦═══╗
            ║%1$2s ║%2$2s ║%3$2s ║
            ╠═══╬═══╬═══╣
            ║%4$2s ║%5$2s ║%6$2s ║
            ╠═══╬═══╬═══╣
            ║%7$2s ║%8$2s ║%9$2s ║
            ╚═══╩═══╩═══╝
            """,
            9
        );
        register(
            ContainerType.CRAFTER_3x3,
            """
            ╔═══╦═══╦═══╗
            ║%1$2s ║%2$2s ║%3$2s ║
            ╠═══╬═══╬═══╣    ╔═══╗
            ║%4$2s ║%5$2s ║%6$2s ║    ║%46$2s ║
            ╠═══╬═══╬═══╣    ╚═══╝
            ║%7$2s ║%8$2s ║%9$2s ║
            ╚═══╩═══╩═══╝
            """,
            9,
            1
        );
        register(
            List.of(ContainerType.ANVIL, ContainerType.MERCHANT),
            """
            ╔═══╗    ╔═══╗    ╔═══╗
            ║%1$2s ║    ║%2$2s ║    ║%3$2s ║
            ╚═══╝    ╚═══╝    ╚═══╝
            """,
            3
        );
        register(
            List.of(ContainerType.BEACON, ContainerType.LECTERN),
            """
            ╔═══╗
            ║%1$2s ║
            ╚═══╝
            """,
            1
        );
        register(
            List.of(ContainerType.BLAST_FURNACE, ContainerType.FURNACE, ContainerType.GRINDSTONE, ContainerType.SMOKER, ContainerType.CARTOGRAPHY),
            """
            ╔═══╗
            ║%1$2s ║
            ╚═══╝    ╔═══╗
            ╔═══╗    ║%3$2s ║
            ║%2$2s ║    ╚═══╝
            ╚═══╝
            """,
            3
        );
        register(
            ContainerType.BREWING_STAND,
            """
            ╔═══╗    ╔═══╗
            ║%5$2s ║    ║%4$2s ║
            ╚═══╝    ╚═══╝
            ╔═══╗    ╔═══╗    ╔═══╗
            ║%1$2s ║    ║%2$2s ║    ║%3$2s ║
            ╚═══╝    ╚═══╝    ╚═══╝
            """,
            5
        );
        register(
            ContainerType.CRAFTING,
            """
            ╔═══╦═══╦═══╗
            ║%2$2s ║%3$2s ║%4$2s ║
            ╠═══╬═══╬═══╣    ╔═══╗
            ║%5$2s ║%6$2s ║%7$2s ║    ║%1$2s ║
            ╠═══╬═══╬═══╣    ╚═══╝
            ║%8$2s ║%9$2s ║%10$2s ║
            ╚═══╩═══╩═══╝
            """,
            10
        );
        register(
            List.of(ContainerType.ENCHANTMENT, ContainerType.STONECUTTER),
            """
            ╔═══╗    ╔═══╗
            ║%1$2s ║    ║%2$2s ║
            ╚═══╝    ╚═══╝
            """,
            2
        );
        register(
            ContainerType.HOPPER,
            """
            ╔═══╗    ╔═══╗    ╔═══╗    ╔═══╗    ╔═══╗
            ║%1$2s ║    ║%2$2s ║    ║%3$2s ║    ║%4$2s ║    ║%5$2s ║
            ╚═══╝    ╚═══╝    ╚═══╝    ╚═══╝    ╚═══╝
            """,
            5
        );
        register(
            ContainerType.LOOM,
            """
            ╔═══╗    ╔═══╗
            ║%1$2s ║    ║%2$2s ║
            ╚═══╝    ╚═══╝
                ╔═══╗        ╔═══╗
                ║%3$2s ║        ║%4$2s ║
                ╚═══╝        ╚═══╝
            """,
            4
        );
        register(
            ContainerType.SMITHING,
            """
            ╔═══╗    ╔═══╗    ╔═══╗    ╔═══╗
            ║%1$2s ║    ║%2$2s ║    ║%3$2s ║    ║%4$2s ║
            ╚═══╝    ╚═══╝    ╚═══╝    ╚═══╝
            """,
            4
        );
    }

    private static final String BOTTOM_SECTION_BASE_ASCII = """
        ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
        ║%1$2s ║%2$2s ║%3$2s ║%4$2s ║%5$2s ║%6$2s ║%7$2s ║%8$2s ║%9$2s ║
        ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
        ║%10$2s ║%11$2s ║%12$2s ║%13$2s ║%14$2s ║%15$2s ║%16$2s ║%17$2s ║%18$2s ║
        ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
        ║%19$2s ║%20$2s ║%21$2s ║%22$2s ║%23$2s ║%24$2s ║%25$2s ║%26$2s ║%27$2s ║
        ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
        ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
        ║%28$2s ║%29$2s ║%30$2s ║%31$2s ║%32$2s ║%33$2s ║%34$2s ║%35$2s ║%36$2s ║
        ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
        """;

    private static String generateAscii(String topContainerAscii, int topContainerSize, final int bottomSlots) {
        String[] bottomSectionReplacements = new String[36];
        for (int i = 0; i < 36; i++) {
            int index = i + topContainerSize + 1;
            bottomSectionReplacements[i] = "%" + index + "$2s";
        }
        String bottomAscii = BOTTOM_SECTION_BASE_ASCII.formatted((Object[]) bottomSectionReplacements);
        return
           """
           ```
           %s
           %s
           ```
           """.formatted(topContainerAscii, bottomAscii);
    }
}
