package com.zenith.command.impl;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.Proxy;
import com.zenith.command.Command;
import com.zenith.command.CommandUsage;
import com.zenith.command.brigadier.CommandCategory;
import com.zenith.command.brigadier.CommandContext;
import com.zenith.feature.pathing.raycast.RaycastHelper;
import com.zenith.util.math.MathHelper;
import org.cloudburstmc.math.vector.Vector3i;

import static com.zenith.Shared.CACHE;

public class ClickCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.simple(
            "click",
            CommandCategory.MODULE,
            "Clicks the block in front of you"
        );
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("click")
            .then(literal("left").executes(c -> {
                c.getSource().setNoOutput(true);
                if (!Proxy.getInstance().isConnected()) return 1;
                var client = Proxy.getInstance().getClient();
                var raycast = RaycastHelper.playerBlockOrEntityRaycast(4.5);
                client.sendAsync(new ServerboundSwingPacket(Hand.MAIN_HAND));
                if (raycast.hit()) {
                    if (raycast.isBlock()) {
                        var bRay = raycast.block();
                        client.sendAsync(new ServerboundPlayerActionPacket(
                            PlayerAction.START_DIGGING,
                            Vector3i.from(
                                MathHelper.floorI(bRay.x()),
                                MathHelper.floorI(bRay.y()),
                                MathHelper.floorI(bRay.z())
                            ),
                            bRay.direction(),
                            CACHE.getPlayerCache().getSeqId().incrementAndGet()
                        ));
                    } else {
                        var eRay = raycast.entity();
                        client.sendAsync(new ServerboundInteractPacket(
                            eRay.entity().getEntityId(),
                            InteractAction.ATTACK,
                            false
                        ));
                    }
                }
                return 1;
            }))
            .then(literal("right").executes(c -> {
                c.getSource().setNoOutput(true);
                if (!Proxy.getInstance().isConnected()) return 1;
                var client = Proxy.getInstance().getClient();
                var raycast = RaycastHelper.playerBlockOrEntityRaycast(4.5);
                client.sendAsync(new ServerboundSwingPacket(Hand.MAIN_HAND));
                if (raycast.hit()) {
                    if (raycast.isBlock()) {
                        var bRay = raycast.block();
                        client.sendAsync(new ServerboundUseItemOnPacket(
                            Vector3i.from(
                                MathHelper.floorI(bRay.x()),
                                MathHelper.floorI(bRay.y()),
                                MathHelper.floorI(bRay.z())
                            ),
                            bRay.direction(),
                            Hand.MAIN_HAND,
                            (float) MathHelper.frac(bRay.x()),
                            (float) MathHelper.frac(bRay.y()),
                            (float) MathHelper.frac(bRay.z()),
                            false,
                            CACHE.getPlayerCache().getSeqId().incrementAndGet()
                        ));
                    } else {
                        var eRay = raycast.entity();
                        // there's a lot more complexity to which interaction packet should be sent depending on the entity and the item we're holding
                        // this is just a simple example
                        client.sendAsync(new ServerboundInteractPacket(
                            eRay.entity().getEntityId(),
                            InteractAction.INTERACT,
                            false
                        ));
                    }
                }
                return 1;
            }));
    }
}
