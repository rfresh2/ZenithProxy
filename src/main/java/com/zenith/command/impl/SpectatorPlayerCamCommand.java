package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.cache.data.entity.Entity;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;

import static com.zenith.Shared.CACHE;

public class SpectatorPlayerCamCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simple(
            "playerCam",
            CommandCategory.MANAGE,
            "Toggles spectators between player and entity cameras. Only usable by spectators"
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("playerCam").requires(c -> Command.validateCommandSource(c, CommandSource.SPECTATOR)).executes(c -> {
            var session = c.getSource().getInGamePlayerInfo().session();
            final Entity existingTarget = session.getCameraTarget();
            if (existingTarget != null) {
                session.setCameraTarget(null);
                session.sendAsync(new ClientboundSetCameraPacket(session.getSpectatorSelfEntityId()));
                SpectatorSync.syncSpectatorPositionToEntity(session, existingTarget);
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<blue>Exited playercam!"), false));
            } else {
                session.setCameraTarget(CACHE.getPlayerCache().getThePlayer());
                session.sendAsync(new ClientboundSetCameraPacket(CACHE.getPlayerCache().getEntityId()));
                var connections = Proxy.getInstance().getActiveConnections().getArray();
                for (int i = 0; i < connections.length; i++) {
                    var connection = connections[i];
                    connection.sendAsync(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
                }
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<blue>Entered playercam!"), false));
            }
            c.getSource().setNoOutput(true);
        });
    }
}
