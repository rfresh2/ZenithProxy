package com.zenith.cache.data.chunk;

import com.zenith.Proxy;
import com.zenith.feature.player.World;
import com.zenith.mc.block.BlockPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import static com.zenith.Globals.BOT;
import static com.zenith.Globals.CACHE;

public class BlockStatePredictionHandler implements AutoCloseable {
    private final Long2ObjectOpenHashMap<ServerVerifiedState> serverVerifiedStates = new Long2ObjectOpenHashMap<>();
    private int currentSequenceNr;
    private boolean isPredicting;

    public void retainKnownServerState(int posX, int posY, int posZ, int blockStateId, double playerX, double playerY, double playerZ) {
        this.serverVerifiedStates
            .compute(
                BlockPos.asLong(posX, posY, posZ),
                (posLong, serverVerifiedState) -> serverVerifiedState != null
                    ? serverVerifiedState.setSequence(this.currentSequenceNr)
                    : new BlockStatePredictionHandler.ServerVerifiedState(this.currentSequenceNr, blockStateId, playerX, playerY, playerZ)
            );
    }

    public boolean updateKnownServerState(int posX, int posY, int posZ, int blockStateId) {
        var serverVerifiedState = this.serverVerifiedStates.get(BlockPos.asLong(posX, posY, posZ));
        if (serverVerifiedState == null) {
            return false;
        } else {
            serverVerifiedState.setBlockStateId(blockStateId);
            return true;
        }
    }

    public void endPredictionsUpTo(int sequence) {
        var serverStatesIterator = this.serverVerifiedStates.long2ObjectEntrySet().iterator();

        while (serverStatesIterator.hasNext()) {
            var entry = serverStatesIterator.next();
            var serverVerifiedState = entry.getValue();
            if (serverVerifiedState.sequence <= sequence) {
                var blockX = BlockPos.getX(entry.getLongKey());
                var blockY = BlockPos.getY(entry.getLongKey());
                var blockZ = BlockPos.getZ(entry.getLongKey());
                serverStatesIterator.remove();
                var blockStateInWorld = World.getBlockStateId(blockX, blockY, blockZ);
                if (blockStateInWorld != serverVerifiedState.blockStateId) {
                    CACHE.getChunkCache().updateBlock(blockX, blockY, blockZ, serverVerifiedState.blockStateId);
                    if (!Proxy.getInstance().hasActivePlayer()) {
                        var blockCollisions = World.getBlockState(blockX, blockY, blockZ).getLocalizedCollisionBoxes();
                        for (var collisionBox : blockCollisions) {
                            if (BOT.getPlayerCollisionBox().intersects(collisionBox)) {
                                BOT.absMoveTo(serverVerifiedState.playerX, serverVerifiedState.playerY, serverVerifiedState.playerZ);
                            }
                        }
                    }
                }
            }
        }
    }

    public BlockStatePredictionHandler startPredicting() {
        this.currentSequenceNr++;
        this.isPredicting = true;
        return this;
    }

    public void close() {
        this.isPredicting = false;
    }

    public int currentSequence() {
        return this.currentSequenceNr;
    }

    public boolean isPredicting() {
        return this.isPredicting;
    }

    static class ServerVerifiedState {
        final double playerX;
        final double playerY;
        final double playerZ;
        int sequence;
        int blockStateId;

        ServerVerifiedState(int sequence, int blockStateId, double playerX, double playerY, double playerZ) {
            this.sequence = sequence;
            this.blockStateId = blockStateId;
            this.playerX = playerX;
            this.playerY = playerY;
            this.playerZ = playerZ;
        }

        BlockStatePredictionHandler.ServerVerifiedState setSequence(int sequence) {
            this.sequence = sequence;
            return this;
        }

        void setBlockStateId(int blockStateId) {
            this.blockStateId = blockStateId;
        }
    }
}
