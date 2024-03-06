package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.cache.data.inventory.Container;
import com.zenith.command.Command;
import com.zenith.command.CommandCategory;
import com.zenith.command.CommandContext;
import com.zenith.command.CommandUsage;

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

    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "inventory",
            CommandCategory.INFO,
            "Shows the player inventory",
            asList(),
            asList("inv")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("inventory").executes(c -> {
            c.getSource().getMultiLineOutput().add(inventoryAscii);
            var playerInv = CACHE.getPlayerCache().getPlayerInventory();
            var sb = new StringBuilder();
            sb.append("```\n");
            var heldSlot = CACHE.getPlayerCache().getHeldItemSlot() + 36;
            for (int i = 0; i < playerInv.size(); i++) {
                var itemStack = playerInv.get(i);
                if (itemStack == Container.EMPTY_STACK) continue;
                var itemData = ITEMS_MANAGER.getItemData(itemStack.getId());
                sb.append("  ").append(i).append(" -> ");
                if (itemStack.getAmount() > 1) sb.append(itemStack.getAmount()).append("x ");
                sb.append(itemData.getName());
                if (i == heldSlot) sb.append(" (held)");
                sb.append("\n");
            }
            sb.append("\n```");
            var items = sb.toString();
            if (items.isEmpty()) {
                c.getSource().getMultiLineOutput().add("Empty!");
            } else {
                c.getSource().getMultiLineOutput().add(items);
            }
        });
    }
}
