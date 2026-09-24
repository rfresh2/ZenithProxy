package com.zenith.module.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandSources;
import com.zenith.command.impl.KillAuraCommand;
import com.zenith.discord.Embed;
import com.zenith.plugin.DefaultGsonConfigSerializer;
import com.zenith.util.config.Config;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.GSON;
import static org.junit.jupiter.api.Assertions.*;

@DisabledInNativeImage
public class KillAuraTest {
    private Config.Client.Extra.KillAura previousSettings;
    private KillAura killAura;
    private Method validTarget;

    @BeforeEach
    public void setUp() throws Exception {
        previousSettings = GSON.fromJson(GSON.toJson(CONFIG.client.extra.killAura), Config.Client.Extra.KillAura.class);
        applyTargetSettings(new Config.Client.Extra.KillAura());
        killAura = new KillAura();
        validTarget = KillAura.class.getDeclaredMethod("validTarget", EntityLiving.class);
        validTarget.setAccessible(true);
    }

    @AfterEach
    public void tearDown() {
        applyTargetSettings(previousSettings);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void namedHostileMobsAreIgnoredEvenWhenAggressive(boolean onlyAggressive) throws Exception {
        CONFIG.client.extra.killAura.ignoreNamedMobs = true;
        CONFIG.client.extra.killAura.onlyHostileAggressive = onlyAggressive;
        var zombie = mob(EntityType.ZOMBIE);
        makeAggressive(zombie);
        assertTrue(validTarget(zombie));

        name(zombie, Component.text("Keep me"));
        assertFalse(validTarget(zombie));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void namedNeutralMobsAreIgnoredEvenWhenAggressive(boolean onlyAggressive) throws Exception {
        CONFIG.client.extra.killAura.ignoreNamedMobs = true;
        CONFIG.client.extra.killAura.targetNeutralMobs = true;
        CONFIG.client.extra.killAura.onlyNeutralAggressive = onlyAggressive;
        var wolf = mob(EntityType.WOLF);
        makeAggressive(wolf);
        assertTrue(validTarget(wolf));

        name(wolf, Component.text("Keep me"));
        assertFalse(validTarget(wolf));
    }

    @ParameterizedTest
    @EnumSource(value = EntityType.class, names = {"COW", "ARMOR_STAND"})
    public void customTargetsCannotOverrideNamedEntityProtection(EntityType type) throws Exception {
        CONFIG.client.extra.killAura.ignoreNamedMobs = true;
        CONFIG.client.extra.killAura.targetCustom = true;
        CONFIG.client.extra.killAura.customTargets.add(type);
        var entity = mob(type);
        assertTrue(validTarget(entity));

        name(entity, Component.text("Keep me"));
        assertFalse(validTarget(entity));
    }

    @Test
    public void disablingOptionPreservesExistingTargetCategories() throws Exception {
        CONFIG.client.extra.killAura.targetNeutralMobs = true;
        CONFIG.client.extra.killAura.targetCustom = true;
        CONFIG.client.extra.killAura.customTargets.add(EntityType.COW);
        for (var type : new EntityType[]{EntityType.ZOMBIE, EntityType.WOLF, EntityType.COW}) {
            var entity = mob(type);
            name(entity, Component.text("Keep me"));
            assertTrue(validTarget(entity), type.name());
            CONFIG.client.extra.killAura.ignoreNamedMobs = true;
            assertFalse(validTarget(entity), type.name());
            CONFIG.client.extra.killAura.ignoreNamedMobs = false;
            assertTrue(validTarget(entity), type.name());
        }
    }

    @Test
    public void missingOrClearedCustomNameDoesNotProtectMob() throws Exception {
        CONFIG.client.extra.killAura.ignoreNamedMobs = true;
        var zombie = mob(EntityType.ZOMBIE);
        assertTrue(validTarget(zombie));
        name(zombie, Component.text("Keep me"));
        assertFalse(validTarget(zombie));

        zombie.getMetadata().put(2, new ObjectEntityMetadata<>(2, MetadataTypes.OPTIONAL_CHAT, Optional.empty()));
        assertTrue(validTarget(zombie));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void nameVisibilityDoesNotDetermineProtection(boolean visible) throws Exception {
        CONFIG.client.extra.killAura.ignoreNamedMobs = true;
        var zombie = mob(EntityType.ZOMBIE);
        zombie.getMetadata().put(3, new BooleanEntityMetadata(3, MetadataTypes.BOOLEAN, visible));
        assertTrue(validTarget(zombie));

        name(zombie, Component.text("Keep me"));
        assertFalse(validTarget(zombie));
    }

    @Test
    public void presentEmptyComponentStillCountsAsCustomName() throws Exception {
        CONFIG.client.extra.killAura.ignoreNamedMobs = true;
        var zombie = mob(EntityType.ZOMBIE);
        name(zombie, Component.empty());
        assertFalse(validTarget(zombie));
    }

    @Test
    public void playerTargetingIsUnaffectedByCustomName() throws Exception {
        CONFIG.client.extra.killAura.ignoreNamedMobs = true;
        var player = new EntityPlayer();
        player.setUuid(UUID.randomUUID());
        name(player, Component.text("Keep me"));
        assertFalse(validTarget(player));

        CONFIG.client.extra.killAura.targetPlayers = true;
        assertTrue(validTarget(player));
        player.setSelfPlayer(true);
        assertFalse(validTarget(player));
    }

    @Test
    public void newAndLegacyConfigsDefaultToExistingBehavior() {
        assertFalse(new Config.Client.Extra.KillAura().ignoreNamedMobs);
        var legacy = DefaultGsonConfigSerializer.INSTANCE.read(Config.Client.Extra.KillAura.class, new StringReader("{}"));
        assertFalse(legacy.ignoreNamedMobs);
    }

    @Test
    public void commandTogglesOptionAndSettingSurvivesSerialization() throws Exception {
        var command = new KillAuraCommand();
        var dispatcher = new CommandDispatcher<CommandContext>();
        dispatcher.register(command.register());

        assertEquals(1, dispatcher.execute("killaura ignoreNamedMobs on", CommandContext.create("killAura ignoreNamedMobs on", CommandSources.TERMINAL)));
        assertTrue(CONFIG.client.extra.killAura.ignoreNamedMobs);
        var output = new StringWriter();
        DefaultGsonConfigSerializer.INSTANCE.write(CONFIG.client.extra.killAura, output);
        var restored = DefaultGsonConfigSerializer.INSTANCE.read(Config.Client.Extra.KillAura.class, new StringReader(output.toString()));
        assertTrue(restored.ignoreNamedMobs);
        var embed = new Embed();
        command.defaultEmbed(embed);
        assertTrue(embed.fields().stream().anyMatch(field -> field.name().equals("Ignore Named Mobs") && field.value().equals("on")));

        assertEquals(1, dispatcher.execute("killaura ignoreNamedMobs off", CommandContext.create("killAura ignoreNamedMobs off", CommandSources.TERMINAL)));
        assertFalse(CONFIG.client.extra.killAura.ignoreNamedMobs);
    }

    private boolean validTarget(EntityLiving entity) throws Exception {
        return (boolean) validTarget.invoke(killAura, entity);
    }

    private static EntityStandard mob(EntityType type) {
        var entity = new EntityStandard();
        entity.setEntityType(type);
        return entity;
    }

    private static void name(EntityLiving entity, Component name) {
        entity.getMetadata().put(2, new ObjectEntityMetadata<>(2, MetadataTypes.OPTIONAL_CHAT, Optional.of(name)));
    }

    private static void makeAggressive(EntityLiving entity) {
        entity.getMetadata().put(15, new ByteEntityMetadata(15, MetadataTypes.BYTE, (byte) 0x04));
    }

    private static void applyTargetSettings(Config.Client.Extra.KillAura settings) {
        var config = CONFIG.client.extra.killAura;
        config.targetPlayers = settings.targetPlayers;
        config.targetHostileMobs = settings.targetHostileMobs;
        config.targetNeutralMobs = settings.targetNeutralMobs;
        config.targetCustom = settings.targetCustom;
        config.targetArmorStands = settings.targetArmorStands;
        config.onlyHostileAggressive = settings.onlyHostileAggressive;
        config.onlyNeutralAggressive = settings.onlyNeutralAggressive;
        config.ignoreNamedMobs = settings.ignoreNamedMobs;
        config.customTargets.clear();
        config.customTargets.addAll(settings.customTargets);
    }
}
