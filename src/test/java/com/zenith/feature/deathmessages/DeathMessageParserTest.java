package com.zenith.feature.deathmessages;

import com.zenith.Shared;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.*;

public class DeathMessageParserTest {

    private final DeathMessagesParser deathMessagesParser = new DeathMessagesParser();

    @Test
    public void basicSuicide() {
        parseTest("LordYes tripped too hard and died.", "LordYes", null, null, null);
    }

    @Test
    public void witheredAway() {
        parseTest("ClosetGirl withered away.", "ClosetGirl", null, null, null);
    }

    @Test
    public void pvpKillerAndWeapon() {
        parseTest("FarGaming was turned into bloody rain by zyzz_motivation with an end crystal",
                "FarGaming", "zyzz_motivation", KillerType.PLAYER, "end crystal");
    }

    @Test
    public void pvpKillerReversedAndNamedWeapon() {
        parseTest("BepSkra assassinated YoMomBroYo with Moving ur stash",
                "YoMomBroYo", "BepSkra", KillerType.PLAYER, "Moving ur stash");
    }

    @Test
    public void mobKill() {
        parseTest("Cap7ainDes7roy was killed to death by a creeper.",
                "Cap7ainDes7roy", "creeper", KillerType.MOB, null);
    }

    @Test
    public void namedMobKill() {
        parseTest("SpaceV0id was killed by a wither named New? Need Gear? Discord.gg/2BSupply",
                "SpaceV0id", "wither", KillerType.MOB, "New? Need Gear? Discord.gg/2BSupply");
    }

    @Test
    public void pvpKillTest() {
        parseTest("Torrentel was violently assassinated by stone_golem_ with an end crystal",
                "Torrentel", "stone_golem_", KillerType.PLAYER, "end crystal");
    }

    @Test
    public void weaponInMiddleOfSchemaTest() {
        parseTest("rfresh2 abused Alpha's Stacked 32k's on rfresh_",
                "rfresh_", "rfresh2", KillerType.PLAYER, "Alpha's Stacked 32k's");
    }

    @Test
    public void zombiePigmen() {
        parseTest("DeathsBlessing was removed by zombie pigmen wielding Golden Sword",
                "DeathsBlessing", "zombie pigmen", KillerType.MOB, "Golden Sword");
    }

    @Test
    public void zombiePigman() {
        parseTest("herrShimon was assassinated by a zombie pigman wielding Golden Sword",
                "herrShimon", "zombie pigman", KillerType.MOB, "Golden Sword");
    }

    @Test
    public void apostropheTest() {
        parseTest("The Zone claims rfresh2's life.",
                "rfresh2", null, null, null);
    }

    @Test
    public void byAZombie() {
        parseTest("April30th1945 was smashed to death by a zombie.",
                "April30th1945", "zombie", KillerType.MOB, null);
    }

    @Test
    public void zombieVillager() {
        parseTest("SandyHookVictim was beaten to death by a zombie villager.",
                "SandyHookVictim", "zombie villager", KillerType.MOB, null);
    }

    @Test
    public void stray() {
        parseTest("Arzexz was assassinated by a stray.",
                "Arzexz", "stray", KillerType.MOB, null);
    }

    @Test
    public void mobTrampleTest() {
        parseTest("Modcrafter72 was trampled to death by a zombie wielding Iron Shovel",
                "Modcrafter72", "zombie", KillerType.MOB, "Iron Shovel");
    }

    @Test
    public void namedAndWieldingMob() {
        parseTest("UhhMagnum2 was killed by a zombie named Free kits .gg/5ZscrDAsp7 wielding Air",
                "UhhMagnum2", "zombie", KillerType.MOB, "Air");
    }

    @Test
    public void namedWeaponWithSameWordAsInSchema() {
        parseTest("Warske abused Gysdall on Top! on Atket_",
                "Atket_", "Warske", KillerType.PLAYER, "Gysdall on Top!");
    }

    @Test
    public void thickPasteByZombieWielding() {
        parseTest("Etbes was reduced into thick paste by a zombie pigman wielding Golden Sword",
                "Etbes", "zombie pigman", KillerType.MOB, "Golden Sword");
    }

    @Test
    public void witherSkeletalWarriorMob() {
        parseTest("Gosha_Dibenko was destroyed by a wither skeletal warrior wielding Stone Sword",
                "Gosha_Dibenko", "wither skeletal warrior", KillerType.MOB, "Stone Sword");
    }

    @Test
    public void burnedApartByZombieTest() {
        parseTest("00nether burned apart whilst fighting a zombie",
                "00nether", "zombie", KillerType.MOB, null);
    }

    @Test
    public void assassinatedByPillagersTest() {
        parseTest("yakyyakyyou was assassinated by pillagers.",
                  "yakyyakyyou", "pillagers", KillerType.MOB, null);
    }

    private void parseTest(final String rawInput, final String victim, final String killerName, final KillerType killerType, final String weapon) {
        Shared.loadConfig();
        Shared.loadLaunchConfig();
        final Optional<DeathMessageParseResult> deathMessageParseResult = deathMessagesParser.parse(rawInput);
        assertTrue(deathMessageParseResult.isPresent());
        assertEquals(deathMessageParseResult.get().getVictim(), victim);
        if (nonNull(killerName)) {
            assertTrue(deathMessageParseResult.get().getKiller().isPresent());
            assertEquals(deathMessageParseResult.get().getKiller().get().getName(), killerName);
            assertEquals(deathMessageParseResult.get().getKiller().get().getType(), killerType);
        } else {
            assertFalse(deathMessageParseResult.get().getKiller().isPresent());
        }
        if (nonNull(weapon)) {
            assertTrue(deathMessageParseResult.get().getWeapon().isPresent());
            assertEquals(deathMessageParseResult.get().getWeapon().get(), weapon);
        } else {
            assertFalse(deathMessageParseResult.get().getWeapon().isPresent());
        }

    }
}
