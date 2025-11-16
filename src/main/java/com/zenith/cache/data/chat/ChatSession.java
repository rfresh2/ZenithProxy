package com.zenith.cache.data.chat;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandSources;
import com.zenith.command.brigadier.MessageArgument;
import lombok.Data;
import lombok.experimental.Accessors;
import net.raphimc.minecraftauth.java.model.MinecraftPlayerCertificates;
import org.geysermc.mcprotocollib.protocol.data.game.ArgumentSignature;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandSignedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.zenith.Globals.CACHE;

@Data
@Accessors(chain = true)
public class ChatSession {
    private final UUID sessionId;
    protected MinecraftPlayerCertificates playerCertificates;
    protected int chainIndex = 0;

    public void sign(ServerboundChatPacket packet) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(playerCertificates.getKeyPair().getPrivate());
            signature.update(Ints.toByteArray(1));
            // message link
            signature.update(uuidToByteArray(CACHE.getProfileCache().getProfile().getId()));
            signature.update(uuidToByteArray(sessionId));
            signature.update(Ints.toByteArray(chainIndex++));

            // message body
            signature.update(Longs.toByteArray(packet.getSalt()));
            signature.update(Longs.toByteArray(packet.getTimeStamp() / 1000));
            var bs = packet.getMessage().getBytes(StandardCharsets.UTF_8);
            signature.update(Ints.toByteArray(bs.length));
            signature.update(bs);
            // last seen messages list (always empty)
            signature.update(Ints.toByteArray(0));
            var sign = signature.sign();
            packet.setSignature(sign);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign message", e);
        }
    }

    public void sign(final ServerboundChatCommandSignedPacket packet) {
        try {
            var parsedSignableNode = parseSignableCommand(packet.getCommand());
            if (parsedSignableNode == null) return;

            var msgRange = parsedSignableNode.getRange();
            String msg = msgRange.get(packet.getCommand());

            List<ArgumentSignature> signatures = new ArrayList<>(1);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(playerCertificates.getKeyPair().getPrivate());
            signature.update(Ints.toByteArray(1));
            // message link
            signature.update(uuidToByteArray(CACHE.getProfileCache().getProfile().getId()));
            signature.update(uuidToByteArray(sessionId));
            signature.update(Ints.toByteArray(chainIndex++));
            // message body
            signature.update(Longs.toByteArray(packet.getSalt()));
            signature.update(Longs.toByteArray(packet.getTimeStamp() / 1000));
            var bs = msg.getBytes(StandardCharsets.UTF_8);
            signature.update(Ints.toByteArray(bs.length));
            signature.update(bs);
            // last seen messages list (always empty)
            signature.update(Ints.toByteArray(0));
            var sign = signature.sign();
            signatures.add(new ArgumentSignature(parsedSignableNode.getNode().getName(), sign));
            packet.setSignatures(signatures);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign command", e);
        }
    }

    public static byte[] uuidToByteArray(UUID uuid) {
        byte[] bs = new byte[16];
        ByteBuffer.wrap(bs).order(ByteOrder.BIG_ENDIAN).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits());
        return bs;
    }

    public boolean isSignableCommand(final String command) {
        return parseSignableCommand(command) != null;
    }

    // todo: watch out for future updates with multiple signable arguments
    //  as is (1.21.4), the only one that can be signed is MessageArgument
    //  and a MessageArgument is basically a greedy string, so there can only be one per command (must be at the end)
    public @Nullable ParsedCommandNode<CommandContext> parseSignableCommand(final String command) {
        var commandDispatcher = CACHE.getChatCache().getCommandDispatcher();
        var parse = commandDispatcher.parse(command, CommandContext.create(command, CommandSources.TERMINAL));
        if (!parse.getExceptions().isEmpty()) {
            return null;
        }
        var parsedNodes = parse.getContext().getLastChild().getNodes();
        for (var node : parsedNodes) {
            if (!(node.getNode() instanceof ArgumentCommandNode<?,?> arg)) continue;
            if (arg.getType() instanceof MessageArgument) {
                return node;
            }
        }
        return null;
    }
}
