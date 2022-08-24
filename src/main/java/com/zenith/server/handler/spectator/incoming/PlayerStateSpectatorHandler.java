package com.zenith.server.handler.spectator.incoming;

import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.data.game.world.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.data.game.world.sound.SoundCategory;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerStatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayBuiltinSoundPacket;
import com.zenith.server.PorkServerConnection;
import com.zenith.util.handler.HandlerRegistry;

import java.util.Random;

public class PlayerStateSpectatorHandler implements HandlerRegistry.IncomingHandler<ClientPlayerStatePacket, PorkServerConnection> {
    private final Random rand = new Random();

    @Override
    public boolean apply(ClientPlayerStatePacket packet, PorkServerConnection session) {
        if (packet.getState() == PlayerState.START_SNEAKING || packet.getState() == PlayerState.START_SPRINTING) {
            final float randFloat = rand.nextFloat();
            final int randInt = rand.nextInt(4);
            session.getProxy().getServerConnections().forEach(connection -> {
                // meow :3
                connection.send(new ServerPlayBuiltinSoundPacket(
                        randInt == 0 ? BuiltinSound.ENTITY_CAT_PURREOW : BuiltinSound.ENTITY_CAT_AMBIENT,
                        SoundCategory.AMBIENT,
                        session.getSpectatorPlayerCache().getX(),
                        session.getSpectatorPlayerCache().getY(),
                        session.getSpectatorPlayerCache().getZ(),
                        1.0f - (randFloat / 2f),
                        1.0f + (randFloat / 10f) // slight pitch variations
                ));
            });
        }
        return false;
    }

    @Override
    public Class<ClientPlayerStatePacket> getPacketClass() {
        return ClientPlayerStatePacket.class;
    }
}
