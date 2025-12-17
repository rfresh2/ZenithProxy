package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.Proxy;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.inventory.Container;
import com.zenith.event.chat.DeathMessageChatEvent;
import com.zenith.event.client.ClientBotTick;
import com.zenith.event.client.ClientDeathEvent;
import com.zenith.event.module.ServerPlayerAttackedUsEvent;
import com.zenith.event.module.SpawnPatrolTargetAcquiredEvent;
import com.zenith.event.module.SpawnPatrolTargetKilledEvent;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.goals.GoalNear;
import com.zenith.feature.pathfinder.goals.GoalXZ;
import com.zenith.feature.player.Input;
import com.zenith.feature.player.InputRequest;
import com.zenith.feature.player.World;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.dimension.DimensionData;
import com.zenith.mc.dimension.DimensionRegistry;
import com.zenith.module.api.Module;
import com.zenith.util.math.MathHelper;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class SpawnPatrol extends Module {
    private final Timer pathTimer = Timers.tickTimer();
    private final Timer killTimer = Timers.tickTimer();
    private double lastX = Double.MIN_VALUE;
    private double lastY = Double.MIN_VALUE;
    private double lastZ = Double.MIN_VALUE;
    private int targetEntityId = -1;
    @Nullable private GameProfile targetProfile = null;
    private long lastDeath = 0;

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientBotTick.class, this::handleBotTick),
            of(ClientBotTick.Starting.class, this::handleBotTickStarting),
            of(DeathMessageChatEvent.class, this::handleDeathMessage),
            of(ServerPlayerAttackedUsEvent.class, this::handlePlayerAttackedUs),
            of(ClientDeathEvent.class, this::handleDeathEvent)
        );
    }

    private void handleDeathEvent(ClientDeathEvent event) {
        lastDeath = System.currentTimeMillis();
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.spawnPatrol.enabled;
    }

    public int getPriority() {
        return Objects.requireNonNullElse(CONFIG.client.extra.spawnPatrol.priority, 6000);
    }

    @Override
    public void onDisable() {
        BARITONE.stop();
        targetEntityId = -1;
    }

    private void handleBotTickStarting(ClientBotTick.Starting event) {
        pathTimer.reset();
        killTimer.reset();
        targetEntityId = -1;
    }

    private void awaitSlashKill() {
        if (System.currentTimeMillis() - lastDeath > 10000) {
            warn("/kill seems to have failed :(");
            warn("And we seem to be stuck");
            walkForwardAndJumpForAwhile(500);
        }
    }

    private ScheduledFuture<?> walkForwardAndJumpFuture = null;

    private void walkForwardAndJumpForAwhile(int ticks) {
        if (walkForwardAndJumpFuture != null && !walkForwardAndJumpFuture.isDone()) {
            warn("Already walking forward and jumping");
            return;
        }
        walkForwardAndJumpFuture = Proxy.getInstance().getClient().getClientEventLoop().scheduleAtFixedRate(() -> {
            if (BARITONE.getPathingBehavior().getFailedPathSearches().get() == 0) {
                walkForwardAndJumpFuture.cancel(true);
                return;
            }

            var in = Input.builder()
                .pressingForward(true)
                .jumping(true)
                .build();
            var req = InputRequest.builder()
                .owner(this)
                .input(in)
                .priority(getPriority());
            if (ThreadLocalRandom.current().nextFloat() > 0.95f) {
                req.yaw(ThreadLocalRandom.current().nextFloat() * 360);
            }
            INPUTS.submit(req.build());
        }, 0, 50, TimeUnit.MILLISECONDS);
        Proxy.getInstance().getClient().getClientEventLoop().schedule(() -> {
            if (walkForwardAndJumpFuture != null) {
                walkForwardAndJumpFuture.cancel(true);
                walkForwardAndJumpFuture = null;
            }
        }, ticks * 50L, TimeUnit.MILLISECONDS);
    }

    private void handleBotTick(ClientBotTick event) {
        if (CONFIG.client.extra.spawnPatrol.stuckKill && killTimer.tick(20L * CONFIG.client.extra.spawnPatrol.stuckKillSeconds) && !MODULE.get(KillAura.class).isActive()) {
            double dist = MathHelper.distance3d(lastX, lastY, lastZ, CACHE.getPlayerCache().getX(), CACHE.getPlayerCache().getY(), CACHE.getPlayerCache().getZ());
            if (dist < CONFIG.client.extra.spawnPatrol.stuckKillMinDist) {
                info("sending /kill. expected: {} actual: {}", CONFIG.client.extra.spawnPatrol.stuckKillMinDist, dist);
                sendClientPacketAsync(new ServerboundChatPacket("/kill"));
                if (CONFIG.client.extra.spawnPatrol.stuckKillAntiStuck) {
                    EXECUTOR.schedule(this::awaitSlashKill, 5L, TimeUnit.SECONDS);
                }
            }
            lastX = CACHE.getPlayerCache().getX();
            lastY = CACHE.getPlayerCache().getY();
            lastZ = CACHE.getPlayerCache().getZ();
        }

        if (pathTimer.tick(20L)) {
            boolean netherPathing = false;
            if (CONFIG.client.extra.spawnPatrol.nether && !BARITONE.getGetToBlockProcess().isActive() && !BARITONE.getFollowProcess().isActive()) {
                DimensionData currentDimension = World.getCurrentDimension();
                if (currentDimension != DimensionRegistry.THE_NETHER.get()) {
                    BARITONE.getTo(BlockRegistry.NETHER_PORTAL);
                    netherPathing = true;
                }
            }
            if (!BARITONE.isActive() && !BARITONE.getFollowProcess().isActive() && !netherPathing) {
                pathToGoal();
            }
            if (CONFIG.client.extra.spawnPatrol.stickyTargeting) {
                boolean targetUnset = targetEntityId == -1;
                EntityLiving targetEntity = null;
                if (targetEntityId != -1) {
                    var e = CACHE.getEntityCache().get(targetEntityId);
                    if (e instanceof EntityLiving el) targetEntity = el;
                }
                boolean targetEntityExists = !targetUnset && targetEntity != null;
                if (targetUnset) {
                    Optional<EntityPlayer> potentialTarget = findNewTarget();
                    if (potentialTarget.isPresent()) {
                        targetEntityId = potentialTarget.get().getEntityId();
                        var targetPlayerListEntry = CACHE.getTabListCache().get(potentialTarget.get().getUuid());
                        if (targetPlayerListEntry.isEmpty()) {
                            warn("Failed to get player list entry for target");
                            return;
                        }
                        targetProfile = targetPlayerListEntry.get().getProfile();
                        info("Target Acquired: {}", targetPlayerListEntry.map(PlayerListEntry::getName).orElse("???"));
                        EVENT_BUS.postAsync(new SpawnPatrolTargetAcquiredEvent(potentialTarget.get(), targetPlayerListEntry.get()));
                        BARITONE.follow(potentialTarget.get());
                    }
                } else if (!targetEntityExists) {
                    info("Target escaped :(");
                    targetEntityId = -1;
                    targetProfile = null;
                    BARITONE.getFollowProcess().onLostControl();
                } else {
                    if (!BARITONE.getFollowProcess().isActive()) {
                        BARITONE.follow(targetEntity);
                    }
                }
            } else if (isValidTargetsPresent()) {
                if (!BARITONE.getFollowProcess().isActive()) {
                    BARITONE.follow(this::targetFilter);
                }
            }
        }
    }

    private void handleDeathMessage(DeathMessageChatEvent event) {
        if (!CONFIG.client.extra.spawnPatrol.stickyTargeting) return;
        var profile = targetProfile;
        if (profile == null) return;
        if (event.deathMessage().victim().equals(profile.getName())) {
            info("Target killed :)");
            EVENT_BUS.postAsync(new SpawnPatrolTargetKilledEvent(profile, event.component(), event.message(), event.deathMessage()));
        }
    }

    private void handlePlayerAttackedUs(ServerPlayerAttackedUsEvent event) {
        if (!CONFIG.client.extra.spawnPatrol.stickyTargeting || !CONFIG.client.extra.spawnPatrol.targetAttackers) return;
        int currentTargetId = targetEntityId;
        EntityPlayer newTarget = event.attacker();
        if (targetEntityId == newTarget.getEntityId()) return;
        if (currentTargetId != -1) {
            Entity currentTarget = CACHE.getEntityCache().get(currentTargetId);
            if (currentTarget instanceof EntityPlayer player) {
                double distanceToCurrentTarget = CACHE.getPlayerCache().distanceSqToSelf(player);
                if (distanceToCurrentTarget < Math.pow(8, 2)) {
                    debug("Ignoring new target because current target is very close");
                    return;
                }
            }
        }
        if (PLAYER_LISTS.getSpawnPatrolIgnoreList().contains(newTarget.getUuid())) return;
        targetEntityId = newTarget.getEntityId();
        var targetPlayerListEntry = CACHE.getTabListCache().get(newTarget.getUuid());
        if (targetPlayerListEntry.isEmpty()) {
            warn("Failed to get player list entry for1 target");
            return;
        }
        targetProfile = targetPlayerListEntry.get().getProfile();
        info("Attacker Target Acquired: {}", targetPlayerListEntry.map(PlayerListEntry::getName).orElse("???"));
        BARITONE.follow(newTarget);
        EVENT_BUS.postAsync(new SpawnPatrolTargetAcquiredEvent(newTarget, targetPlayerListEntry.get()));
    }

    private boolean isValidTargetsPresent() {
        return findNewTarget().isPresent();
    }

    private Optional<EntityPlayer> findNewTarget() {
        return CACHE.getEntityCache().getEntities().values().stream()
            .filter(e -> e instanceof EntityPlayer)
            .map(e -> (EntityPlayer) e)
            .filter(this::targetFilter)
            .findFirst();
    }

    private boolean targetFilter(EntityLiving e) {
        if (!(e instanceof EntityPlayer player)) return false;
        if (player.isSelfPlayer()) return false;
        if (PLAYER_LISTS.getSpawnPatrolIgnoreList().contains(player.getUuid())) return false;
        if (CONFIG.client.extra.spawnPatrol.ignoreFriends && PLAYER_LISTS.getFriendsList().contains(player.getUuid())) return false;
        if (CONFIG.client.extra.spawnPatrol.targetOnlyBedrock) {
            var tablistEntry = CACHE.getTabListCache().get(e.getUuid());
            if (tablistEntry.isPresent()) {
                if (!tablistEntry.get().getName().startsWith(".")) {
                    return false;
                }
            }
        }
        if (CONFIG.client.extra.spawnPatrol.targetOnlyNakeds) {
            int equipCount = 0;
            for (var equipEntry : e.getEquipment().entrySet()) {
                if (equipEntry.getKey() == EquipmentSlot.MAIN_HAND || equipEntry.getKey() == EquipmentSlot.OFF_HAND) continue;
                if (equipEntry.getValue() != Container.EMPTY_STACK) equipCount++;
            }
            return equipCount <= 1;
        }
        return true;
    }

    private void pathToGoal() {
        Goal goal = new GoalNear(
            CONFIG.client.extra.spawnPatrol.goalX,
            CONFIG.client.extra.spawnPatrol.goalY,
            CONFIG.client.extra.spawnPatrol.goalZ,
            (int) Math.pow(10, 2));
        if (goal.isInGoal(
            MathHelper.floorI(CACHE.getPlayerCache().getX()),
            MathHelper.floorI(CACHE.getPlayerCache().getY()),
            MathHelper.floorI(CACHE.getPlayerCache().getZ()))
        ) {
            info("Reached goal");
            pathRandom();
        } else {
            info("Pathing to goal: {}", goal);
            BARITONE.pathTo(goal);
        }
    }

    private void pathRandom() {
        double range = CONFIG.client.extra.spawnPatrol.maxPatrolRange;
        double randomRange = ThreadLocalRandom.current().nextDouble(range / 2, range);
        double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
        double randomXOff = Math.cos(angle) * randomRange;
        double randomZOff = Math.sin(angle) * randomRange;
        var goal = new GoalXZ(
            MathHelper.floorI(randomXOff + CONFIG.client.extra.spawnPatrol.goalX),
            MathHelper.floorI(randomZOff + CONFIG.client.extra.spawnPatrol.goalZ)
        );
        info("Pathing to {}", goal);
        BARITONE.pathTo(goal);
    }
}
