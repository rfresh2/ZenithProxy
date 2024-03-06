package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.cache.data.inventory.Container;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;

import java.util.Arrays;

import static com.zenith.Shared.CACHE;
import static com.zenith.Shared.ITEMS_MANAGER;
import static java.util.Arrays.asList;

public class InventoryCommand extends Command {
    private static final String inventoryAscii =
"""
```

╔═══╦═══════════╗
║ 5 ║    ███    ║   ╔═══╦═══╗
╠═══╣    ███    ║   ║ 1 ║ 2 ║   ╔═══╗
║ 6 ║  ███████  ║   ╠═══╬═══╣   ║ 0 ║
╠═══╣  ███████  ║   ║ 3 ║ 4 ║   ╚═══╝
║ 7 ║  ███████  ║   ╚═══╩═══╝
╠═══╣    ███    ╠═══╗
║ 8 ║    ███    ║45 ║
╚═══╩═══════════╩═══╝
╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
║ 9 ║10 ║11 ║12 ║13 ║14 ║15 ║16 ║17 ║
╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
║18 ║19 ║20 ║21 ║22 ║23 ║24 ║25 ║26 ║
╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣
║27 ║28 ║29 ║30 ║31 ║32 ║33 ║34 ║35 ║
╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝
╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗
║36 ║37 ║38 ║39 ║40 ║41 ║42 ║43 ║44 ║
╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝

```
""";

    private static final String inventoryAsciiFormatter =
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

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "inventory",
            CommandCategory.INFO,
            "Shows the player inventory",
            asList("", "slotIds"),
            asList("inv")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("inventory")
            .executes(c -> {
                var playerInv = CACHE.getPlayerCache().getPlayerInventory();
                var slotArray = new String[46];
                Arrays.fill(slotArray, "");
                var sb = new StringBuilder();
                sb.append("```\n");
                var heldSlot = CACHE.getPlayerCache().getHeldItemSlot() + 36;
                for (int i = 0; i < playerInv.size(); i++) {
                    var itemStack = playerInv.get(i);
                    if (itemStack == Container.EMPTY_STACK) continue;
                    slotArray[i] = i+"";
                    var itemData = ITEMS_MANAGER.getItemData(itemStack.getId());
                    sb.append("  ").append(i).append(" -> ");
                    if (itemStack.getAmount() > 1) sb.append(itemStack.getAmount()).append("x ");
                    sb.append(itemData.getName());
                    if (i == heldSlot) sb.append(" (held)");
                    sb.append("\n");
                }
                sb.append("\n```");
                var items = sb.toString();
                c.getSource().getMultiLineOutput().add(inventoryAsciiFormatter.formatted((Object[]) slotArray));
                if (items.isEmpty()) {
                    c.getSource().getMultiLineOutput().add("Empty!");
                } else {
                    c.getSource().getMultiLineOutput().add(items);
                }})
            .then(literal("slotIds").executes(c -> {
                // shows the slot ids ascii, doesn't print actual player inventory
                c.getSource().getMultiLineOutput().add(inventoryAscii);
            }));
    }
}
