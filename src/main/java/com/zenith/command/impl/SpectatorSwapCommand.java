package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;

import static com.zenith.Shared.CONFIG;

public class SpectatorSwapCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simple(
            "swap",
            CommandCategory.MODULE,
            """
            Swaps the current controlling player to spectator mode.
            """
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        // todo: requires?
        return command("swap").executes(c -> {
            var player = Proxy.getInstance().getActivePlayer();
            if (player == null) {
                c.getSource().getEmbed()
                    .title("Unable to Swap")
                    .errorColor()
                    .description("No player is currently controlling the proxy account");
                return;
            }
            player.transferToSpectator(CONFIG.server.getProxyAddressForTransfer(), CONFIG.server.getProxyPortForTransfer());
            var currentProfile = player.getProfileCache().getProfile();
            c.getSource().getEmbed()
                .title("Swap Sent")
                .primaryColor()
                .addField("Player", currentProfile != null ? currentProfile.getName() : "Unknown", false);
        });
    }
}
