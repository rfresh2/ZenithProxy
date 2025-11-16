package com.zenith.cache.data.chat;

import com.mojang.brigadier.CommandDispatcher;
import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import com.zenith.command.api.CommandContext;
import com.zenith.command.brigadier.McplBrigadierConverter;
import lombok.Data;
import lombok.Locked;
import lombok.experimental.Accessors;
import net.raphimc.minecraftauth.java.model.MinecraftPlayerCertificates;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

import static com.zenith.Globals.CACHE_LOG;
import static com.zenith.Globals.CONFIG;

@Data
@Accessors(chain = true)
public class ChatCache implements CachedData {
    protected CommandNode[] commandNodes = new CommandNode[0];
    protected CommandDispatcher<CommandContext> commandDispatcher = new CommandDispatcher<>();
    private boolean commandTreeParsed = false;
    protected int firstCommandNodeIndex;
    protected volatile long lastChatTimestamp = System.currentTimeMillis();
    protected boolean enforcesSecureChat = false;
    protected ChatSession chatSession = new ChatSession(UUID.randomUUID());
    protected @Nullable MinecraftPlayerCertificates playerCertificates;

    @Override
    public void getPackets(@NonNull final Consumer<Packet> consumer, final @NonNull TcpSession session) {
        consumer.accept(new ClientboundCommandsPacket(this.commandNodes, this.firstCommandNodeIndex));
    }

    @Override
    public void reset(CacheResetType type) {
        if (type == CacheResetType.PROTOCOL_SWITCH || type == CacheResetType.FULL) {
            this.commandNodes = new CommandNode[0];
            this.commandDispatcher = new CommandDispatcher<>();
            this.commandTreeParsed = false;
            this.firstCommandNodeIndex = 0;
        }
        if (type == CacheResetType.FULL) {
            this.enforcesSecureChat = false;
            this.lastChatTimestamp = System.currentTimeMillis();
        }
    }

    @Override
    public String getSendingMessage()  {
        return String.format("Sending %s server commands", this.commandNodes.length);
    }

    public boolean canUseChatSigning() {
        return this.enforcesSecureChat && this.playerCertificates != null && CONFIG.client.chatSigning.enabled;
    }

    public ChatSession startNewChatSession() {
        this.chatSession = new ChatSession(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        if (this.playerCertificates == null) {
            CACHE_LOG.error("Initializing chat session without player certificates");
        }
        this.chatSession.setPlayerCertificates(this.playerCertificates);
        return this.chatSession;
    }

    @Locked
    public CommandDispatcher<CommandContext> getCommandDispatcher() {
        if (!this.commandTreeParsed) {
            this.parseCommandTree();
        }
        return this.commandDispatcher;
    }

    private void parseCommandTree() {
        try {
            this.commandDispatcher = McplBrigadierConverter.toBrigadier(this.commandNodes);
        } catch (Exception e) {
            CACHE_LOG.error("Failed to parse command tree", e);
        }
        this.commandTreeParsed = true;
    }

    /**
     * Pass full raw command string
     */
    public boolean isSignableCommand(final String command) {
        return chatSession.isSignableCommand(command);
    }
}
