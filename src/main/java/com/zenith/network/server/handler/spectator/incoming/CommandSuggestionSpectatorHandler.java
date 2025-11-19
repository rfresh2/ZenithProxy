package com.zenith.network.server.handler.spectator.incoming;

import com.mojang.brigadier.suggestion.Suggestions;
import com.zenith.Proxy;
import com.zenith.command.api.CommandSources;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandSuggestionsPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.zenith.Globals.*;

public class CommandSuggestionSpectatorHandler implements PacketHandler<ServerboundCommandSuggestionPacket, ServerSession> {
    @Override
    public ServerboundCommandSuggestionPacket apply(final ServerboundCommandSuggestionPacket packet, final ServerSession session) {
        if (CONFIG.inGameCommands.slashCommands
            && CONFIG.inGameCommands.enable
            && CONFIG.server.spectator.fullCommandsEnabled
            && CONFIG.server.spectator.fullCommandsAcceptSlashCommands
            && (CONFIG.server.spectator.fullCommandsRequireRegularWhitelist
            ? PLAYER_LISTS.getWhitelist().contains(session.getProfileCache().getProfile().getId())
            : true)
            && CONFIG.inGameCommands.suggestions
        ) {
            EXECUTOR.execute(() -> retrieveSuggestions(packet, session));
        }
        return null;
    }

    private static void retrieveSuggestions(final ServerboundCommandSuggestionPacket packet, final ServerSession session) {
        Suggestions suggestions;
        try {
            suggestions = COMMAND.suggestions(packet.getText(), CommandSources.SPECTATOR, session).get(5L, TimeUnit.SECONDS);
        } catch (Exception e) {
            SERVER_LOG.debug("Timed out retrieving command suggestions", e);
            suggestions = Suggestions.empty().resultNow();
        }
        if (suggestions.isEmpty()) {
            Proxy.getInstance().getClient().sendAsync(packet);
            return;
        }
        final List<String> matches = new ArrayList<>();
        final List<Component> tooltips = new ArrayList<>();
        for (var s : suggestions.getList()) {
            matches.add(s.getText());
            tooltips.add(Optional.ofNullable(s.getTooltip())
                             .map(t -> Component.text(t.getString()))
                             .orElse(null)
            );
        }
        var response = new ClientboundCommandSuggestionsPacket(
            packet.getTransactionId(),
            suggestions.getRange().getStart(),
            suggestions.getRange().getLength(),
            matches.toArray(new String[0]),
            tooltips.toArray(new Component[0])
        );
        session.sendAsync(response);
    }
}
