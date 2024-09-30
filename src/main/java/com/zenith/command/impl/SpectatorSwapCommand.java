package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.command.brigadier.CommandSource;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

import java.util.Optional;

import static com.zenith.Shared.CONFIG;
import static com.zenith.Shared.PLAYER_LISTS;
import static java.util.Arrays.asList;

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
        return command("swap").requires(c -> Command.validateCommandSource(c, asList(CommandSource.IN_GAME_PLAYER, CommandSource.SPECTATOR))).executes(c -> {
            if (c.getSource().getSource() == CommandSource.IN_GAME_PLAYER) {
                var player = Proxy.getInstance().getActivePlayer();
                if (player == null) {
                    c.getSource().getEmbed()
                        .title("Unable to Swap")
                        .errorColor()
                        .description("No player is currently controlling the proxy account");
                    return;
                }
                if (CONFIG.server.viaversion.enabled) {
                    Optional<ProtocolVersion> viaClientProtocolVersion = Via.getManager().getConnectionManager().getConnectedClients().values().stream()
                        .filter(client -> client.getChannel() == player.getChannel())
                        .map(con -> con.getProtocolInfo().protocolVersion())
                        .findFirst();
                    if (viaClientProtocolVersion.isPresent() && viaClientProtocolVersion.get().olderThan(ProtocolVersion.v1_20_5)) {
                        c.getSource().getEmbed()
                            .title("Unsupported Client MC Version")
                            .errorColor()
                            .addField("Client Version", viaClientProtocolVersion.get().getName(), false)
                            .addField("Error", "Client version must be at least 1.20.6", false);
                        return;
                    }
                }
                player.transferToSpectator(CONFIG.server.getProxyAddressForTransfer(), CONFIG.server.getProxyPortForTransfer());
            } else if (c.getSource().getSource() == CommandSource.SPECTATOR) {
                var session = c.getSource().getInGamePlayerInfo().session();
                var spectatorProfile = session.getProfileCache().getProfile();
                c.getSource().setNoOutput(true);
                if (spectatorProfile == null) return;
                if (!PLAYER_LISTS.getWhitelist().contains(spectatorProfile.getId())) {
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&cYou are not whitelisted!&r"), false));
                    return;
                }
                if (Proxy.getInstance().getActivePlayer() != null) {
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&cSomeone is already controlling the player!&r"), false));
                    return;
                }
                if (CONFIG.server.viaversion.enabled) {
                    Optional<ProtocolVersion> viaClientProtocolVersion = Via.getManager().getConnectionManager().getConnectedClients().values().stream()
                        .filter(client -> client.getChannel() == session.getChannel())
                        .map(con -> con.getProtocolInfo().protocolVersion())
                        .findFirst();
                    if (viaClientProtocolVersion.isPresent() && viaClientProtocolVersion.get().olderThan(ProtocolVersion.v1_20_5)) {
                        session.send(new ClientboundSystemChatPacket(ComponentSerializer.minedown("&cUnsupported Client MC Version&r"), false));
                        return;
                    }
                }
                session.transferToControllingPlayer(CONFIG.server.getProxyAddressForTransfer(), CONFIG.server.getProxyPortForTransfer());
            }
        });
    }
}
