package com.zenith.network.client.handler.incoming;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.zenith.event.proxy.DeathMessageEvent;
import com.zenith.event.proxy.SelfDeathMessageEvent;
import com.zenith.event.proxy.ServerChatReceivedEvent;
import com.zenith.event.proxy.ServerRestartingEvent;
import com.zenith.feature.deathmessages.DeathMessageParseResult;
import com.zenith.feature.deathmessages.DeathMessagesParser;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.AsyncIncomingHandler;
import lombok.NonNull;
import net.daporkchop.lib.minecraft.text.component.MCTextRoot;
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class ChatHandler implements AsyncIncomingHandler<ServerChatPacket, ClientSession> {
    private final DeathMessagesParser deathMessagesHelper = new DeathMessagesParser();
    private Instant lastRestartEvent = Instant.EPOCH;

    @Override
    public boolean applyAsync(@NonNull ServerChatPacket packet, @NonNull ClientSession session) {
        try {
            CHAT_LOG.info(packet.getMessage().replace("\\n\\n", "")); // removes the chat clearing linebreaks from queue messages
            final MCTextRoot mcTextRoot = AutoMCFormatParser.DEFAULT.parse(packet.getMessage());
            final String messageString = mcTextRoot.toRawString();
            /*
             * example death message:
             * {"extra":[{"text":""},{"color":"dark_aqua","text":""},
             * {"color":"dark_aqua","clickEvent":{"action":"suggest_command","value":"/w DCI5135 "},
             * "hoverEvent":{"action":"show_text","value":[{"text":""},
             * {"color":"gold","text":"Message "},{"color":"dark_aqua","text":""},
             * {"color":"dark_aqua","text":"DCI5135"},{"color":"dark_aqua","text":""}]},"text":"DCI5135"},
             * {"color":"dark_aqua","text":" "},
             * {"color":"dark_red","text":"died inside lava somehow."}],"text":""}
             */
            if (!messageString.startsWith("<")) { // normal chat msg
                // death message color on 2b
                if (mcTextRoot.getChildren().stream().anyMatch(child -> nonNull(child.getColor()) && child.getColor().equals(new Color(170, 0, 0)))) {
                    final Optional<DeathMessageParseResult> deathMessageParseResult = deathMessagesHelper.parse(messageString);
                    if (deathMessageParseResult.isPresent()) {
                        EVENT_BUS.dispatch(new DeathMessageEvent(deathMessageParseResult.get(), messageString));
                        if (deathMessageParseResult.get().getVictim().equals(CACHE.getProfileCache().getProfile().getName())) {
                            EVENT_BUS.dispatch(new SelfDeathMessageEvent(messageString));
                        }
                    } else {
                        CLIENT_LOG.warn("Failed to parse death message: {}", messageString);
                    }
                } else if (messageString.startsWith(("[SERVER]"))) { // server message
                    if (messageString.startsWith("[SERVER] Server restarting in") && lastRestartEvent.isBefore(Instant.now().minus(Duration.ofMinutes(1)))) { // todo: include time till restart in event
                        lastRestartEvent = Instant.now();
                        EVENT_BUS.dispatch(new ServerRestartingEvent(messageString));
                    }
                }
            }

            boolean isWhisper = false;
            String playerName = null;
            if (messageString.startsWith("<")) {
                playerName = extractSenderNameNormalChat(messageString);
            } else {
                final String[] split = messageString.split(" ");
                if (split.length > 2 && split[1].startsWith("whispers")) {
                    isWhisper = true;
                    playerName = extractSenderNameWhisper(split);
                }
            }
            if (playerName == null) {
                EVENT_BUS.dispatch(new ServerChatReceivedEvent(Optional.empty(), messageString, isWhisper));
            } else {
                EVENT_BUS.dispatch(new ServerChatReceivedEvent(CACHE.getTabListCache().getTabList().getFromName(playerName), messageString, isWhisper));
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Caught exception in ChatHandler. Packet: " + packet, e);
        }
        return true;
    }

    @Override
    public Class<ServerChatPacket> getPacketClass() {
        return ServerChatPacket.class;
    }

    private String extractSenderNameNormalChat(final String message) {
        return message.substring(message.indexOf("<") + 1, message.indexOf(">"));
    }

    private String extractSenderNameWhisper(final String[] messageSplit) {
        return messageSplit[0].trim();
    }
}
