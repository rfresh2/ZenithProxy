package com.zenith.via;

import com.viaversion.viaversion.api.command.ViaCommandSender;
import com.zenith.command.api.CommandContext;
import com.zenith.discord.Embed;
import com.zenith.util.ComponentSerializer;
import lombok.Data;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Optional;
import java.util.UUID;

import static com.zenith.Globals.DISCORD;

@Data
public class ZenithViaCommandSender implements ViaCommandSender {
    private final CommandContext ctx;

    @Override
    public boolean hasPermission(final String s) {
        return true;
    }

    @Override
    public void sendMessage(final String s) {
        var component = LegacyComponentSerializer.legacySection().deserialize(s);
        String text = ComponentSerializer.serializePlain(component).trim();
        if (!text.isBlank()) {
            DISCORD.sendEmbedMessage(
                Embed.builder().description(text));
        }
        // embed not logged because it has no title so we have to do it manually
        ComponentLogger.logger("ViaVersion").info(component);
        if (ctx.getInGamePlayerInfo() != null) {
            ctx.getInGamePlayerInfo().session().sendAsyncMessage(component);
        }
    }

    @Override
    public UUID getUUID() {
        return Optional.ofNullable(ctx.getInGamePlayerInfo())
            .map(i -> i.session().getUUID())
            .orElse(null);
    }

    @Override
    public String getName() {
        return Optional.ofNullable(ctx.getInGamePlayerInfo())
            .map(i -> i.session().getName())
            .orElse(ctx.getSource().name());
    }
}
