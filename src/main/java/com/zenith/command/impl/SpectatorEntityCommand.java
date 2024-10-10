package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.feature.spectator.SpectatorEntityRegistry;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static java.util.Arrays.asList;

public class SpectatorEntityCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.full(
            "spectatorEntity",
            CommandCategory.MANAGE,
            "Changes the current spectator entity. Only usable by spectators",
            asList(
                "",
                "<entity>"
            ),
            asList("e")
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("spectatorEntity").requires(c -> Command.validateCommandSource(c, CommandSource.SPECTATOR))
            .executes(c -> {
                var session = c.getSource().getInGamePlayerInfo().session();
                session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>Entity id's: " + String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers())), false));
                c.getSource().setNoOutput(true);
            })
            .then(argument("entity", word()).executes(c -> {
                String entityId = getString(c, "entity");
                var session = c.getSource().getInGamePlayerInfo().session();
                boolean spectatorEntitySet = session.setSpectatorEntity(entityId);
                if (spectatorEntitySet) {
                    // respawn entity on all connections
                    var connections = Proxy.getInstance().getActiveConnections().getArray();
                    for (int i = 0; i < connections.length; i++) {
                        var connection = connections[i];
                        connection.sendAsync(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
                        if (!connection.equals(session) || session.isShowSelfEntity()) {
                            connection.sendAsync(session.getEntitySpawnPacket());
                            connection.sendAsync(session.getEntityMetadataPacket());
                            SpectatorSync.updateSpectatorPosition(session);
                        }
                    }
                    session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<blue>Updated entity to: " + entityId), false));
                } else {
                    session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>No entity found with id: " + entityId), false));
                    session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>Valid id's: " + String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers())), false));
                }
                c.getSource().setNoOutput(true);
                return OK;
        }));
    }
}
