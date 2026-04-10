package com.zenith.module.api;

import com.zenith.Proxy;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandOutputHelper;
import com.zenith.command.api.CommandSource;
import com.zenith.discord.Embed;
import com.zenith.network.client.ClientSession;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.packet.Packet;

import java.util.List;

import static com.zenith.Globals.*;

public abstract class ModuleUtils {
    public void sendClientPacketAsync(final Packet packet) {
        ClientSession clientSession = Proxy.getInstance().getClient();
        if (clientSession != null && clientSession.isConnected()) {
            clientSession.sendAsync(packet);
        }
    }

    public void sendClientPacket(final Packet packet) {
        ClientSession clientSession = Proxy.getInstance().getClient();
        if (clientSession != null && clientSession.isConnected()) {
            clientSession.send(packet);
        }
    }

    public void sendClientPacketAwait(final Packet packet) {
        ClientSession clientSession = Proxy.getInstance().getClient();
        if (clientSession != null && clientSession.isConnected()) {
            try {
                clientSession.sendAwait(packet);
            } catch (Exception e) {
                error("Error sending awaited packet: {}", packet.getClass().getSimpleName(), e);
            }
        }
    }

    // preserves packet order
    public void sendClientPacketsAsync(final Packet... packets) {
        ClientSession clientSession = Proxy.getInstance().getClient();
        if (clientSession != null && clientSession.isConnected()) {
            for (Packet packet : packets) {
                clientSession.sendAsync(packet);
            }
        }
    }

    protected final String moduleLogPrefix = "[" + this.getClass().getSimpleName() + "] ";

    public void debug(String msg) {
        MODULE_LOG.debug("{}{}", moduleLogPrefix, msg);
    }

    public void debug(String msg, Object... args) {
        MODULE_LOG.debug(moduleLogPrefix + msg, args);
    }

    public void debug(Component msg) {
        MODULE_LOG.debug("{}{}", moduleLogPrefix, msg);
    }

    public void info(String msg) {
        MODULE_LOG.info("{}{}", moduleLogPrefix, msg);
    }

    public void info(String msg, Object... args) {
        MODULE_LOG.info(moduleLogPrefix + msg, args);
    }

    public void info(Component msg) {
        MODULE_LOG.info("{}{}", moduleLogPrefix, msg);
    }

    public void warn(String msg) {
        MODULE_LOG.warn("{}{}", moduleLogPrefix, msg);
    }

    public void warn(String msg, Object... args) {
        MODULE_LOG.warn(moduleLogPrefix + msg, args);
    }

    public void warn(Component msg) {
        MODULE_LOG.warn("{}{}", moduleLogPrefix, msg);
    }

    public void error(String msg) {
        MODULE_LOG.error("{}{}", moduleLogPrefix, msg);
    }

    public void error(String msg, Object... args) {
        MODULE_LOG.error(moduleLogPrefix + msg, args);
    }

    public void error(Component msg) {
        MODULE_LOG.error("{}{}", moduleLogPrefix, msg);
    }

    protected final String moduleAlertPrefix = "<gray>[<aqua>" + this.getClass().getSimpleName() + "<gray>]<reset> ";

    public void inGameAlert(String minimessage) {
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            connection.sendAsyncAlert(moduleAlertPrefix + minimessage);
        }
    }

    public void inGameAlertActivePlayer(String minimessage) {
        var connection = Proxy.getInstance().getActivePlayer();
        if (connection == null) return;
        connection.sendAsyncAlert(moduleAlertPrefix + minimessage);
    }

    // is also logged to the terminal
    public void discordNotification(Embed embed) {
        embed.title(moduleLogPrefix + (embed.isTitlePresent() ? embed.title() : ""));
        EXECUTOR.execute(() -> DISCORD.sendEmbedMessage(embed));
    }

    public void discordAndIngameNotification(Embed embed) {
        discordNotification(embed);
        EXECUTOR.execute(() -> CommandOutputHelper.logEmbedOutputToInGameAllConnectedPlayers(embed));
    }

    public void disconnect(ServerSession session, String minimessage) {
        session.disconnect(ComponentSerializer.minimessage("<red>" + moduleLogPrefix + "</red><gray>" + minimessage));
    }

    public void executeCommand(String command, boolean accountOwnerPerms) {
        class ModuleCommandSource implements CommandSource {
            @Override
            public String name() {
                return this.getClass().getSimpleName();
            }
            @Override
            public boolean validateAccountOwner(final CommandContext ctx) {
                return accountOwnerPerms;
            }
            @Override
            public void logEmbed(final CommandContext ctx, final Embed embed) {
                discordNotification(embed);
            }

            @Override
            public void logMultiLine(final List<String> multiLine) {
                CommandOutputHelper.logMultiLineOutputToDiscord(multiLine);
            }
        }
        var ctx = CommandContext.create(command, new ModuleCommandSource());
        COMMAND.execute(ctx);
        if (!ctx.isNoOutput() && !ctx.getEmbed().isTitlePresent() && ctx.getMultiLineOutput().isEmpty()) {
            CommandOutputHelper.logEmbedOutputToDiscord(Embed.builder()
                .title("[" + this.getClass().getSimpleName() + " Command Error")
                .addField("Error", "Unknown Command")
                .addField("Command", "`" + command + "`"));
            return;
        }
        CommandOutputHelper.logEmbedOutputToDiscord(ctx.getEmbed());
        CommandOutputHelper.logMultiLineOutputToDiscord(ctx.getMultiLineOutput());
    }
}
