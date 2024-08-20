package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;

import static java.util.Arrays.asList;

public class SpectatorEntityToggleCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simpleAliases(
            "entityToggle",
            CommandCategory.MANAGE,
            "Toggles the visibility of spectator entities. Only usable by spectators.",
            asList("etoggle")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("entityToggle").requires(c -> Command.validateCommandSource(c, CommandSource.SPECTATOR)).executes(c -> {
            var session = c.getSource().getInGamePlayerInfo().session();
            session.setShowSelfEntity(!session.isShowSelfEntity());
            if (session.isShowSelfEntity()) {
                session.sendAsync(session.getEntitySpawnPacket());
                session.sendAsync(session.getEntityMetadataPacket());
            } else {
                session.sendAsync(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
            }
            session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&9Show self entity toggled " + (session.isShowSelfEntity() ? "on!" : "off!") + "&r"), false));
            c.getSource().setNoOutput(true);
        });
    }
}
