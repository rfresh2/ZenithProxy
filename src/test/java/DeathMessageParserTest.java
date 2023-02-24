import com.zenith.util.deathmessages.DeathMessageParseResult;
import com.zenith.util.deathmessages.DeathMessagesParser;
import com.zenith.util.deathmessages.Killer;
import com.zenith.util.deathmessages.KillerType;
import lombok.Builder;
import lombok.Data;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeathMessageParserTest {

    private final DeathMessagesParser deathMessagesParser = new DeathMessagesParser();

    @Test
    public void basicSuicide() {
        parseTest(DeathMessageTestCase.builder()
                .rawInput("LordYes tripped too hard and died.")
                .expectedParseResult(new DeathMessageParseResult("LordYes", Optional.empty(), Optional.empty(), null))
                .build());
    }

    @Test
    public void witheredAway() {
        parseTest(DeathMessageTestCase.builder()
                .rawInput("ClosetGirl withered away.")
                .expectedParseResult(new DeathMessageParseResult("ClosetGirl", Optional.empty(), Optional.empty(), null))
                .build());
    }

    @Test
    public void pvpKillerAndWeapon() {
        parseTest(DeathMessageTestCase.builder()
                .rawInput("FarGaming was turned into bloody rain by zyzz_motivation with an end crystal")
                .expectedParseResult(new DeathMessageParseResult("FarGaming", Optional.of(new Killer("zyzz_motivation", KillerType.PLAYER)), Optional.of("end crystal"), null))
                .build());
    }

    @Test
    public void pvpKillerReversedAndNamedWeapon() {
        parseTest(DeathMessageTestCase.builder()
                .rawInput("BepSkra assassinated YoMomBroYo with Moving ur stash")
                .expectedParseResult(new DeathMessageParseResult("YoMomBroYo", Optional.of(new Killer("BepSkra", KillerType.PLAYER)), Optional.of("Moving ur stash"), null))
                .build());
    }

    @Test
    public void mobKill() {
        parseTest(DeathMessageTestCase.builder()
                .rawInput("Cap7ainDes7roy was killed to death by a creeper.")
                .expectedParseResult(new DeathMessageParseResult("Cap7ainDes7roy", Optional.of(new Killer("creeper", KillerType.MOB)), Optional.empty(), null))
                .build());
    }

    @Test
    public void namedMobKill() {
        parseTest(DeathMessageTestCase.builder()
                .rawInput("SpaceV0id was killed by a wither named New? Need Gear? Discord.gg/2BSupply")
                .expectedParseResult(new DeathMessageParseResult("SpaceV0id", Optional.of(new Killer("wither", KillerType.MOB)), Optional.of("New? Need Gear? Discord.gg/2BSupply"), null))
                .build());
    }

    @Test
    public void pvpKillTest() {
        parseTest(DeathMessageTestCase.builder()
                .rawInput("Torrentel was violently assassinated by stone_golem_ with an end crystal")
                .expectedParseResult(new DeathMessageParseResult("Torrentel", Optional.of(new Killer("stone_golem_", KillerType.PLAYER)), Optional.of("end crystal"), null))
                .build());
    }

    private void parseTest(final DeathMessageTestCase testCase) {
        final Optional<DeathMessageParseResult> deathMessageParseResult = deathMessagesParser.parse(testCase.rawInput);
        assertTrue(deathMessageParseResult.isPresent());
        assertEquals(deathMessageParseResult.get().getVictim(), testCase.expectedParseResult.getVictim());
        assertEquals(deathMessageParseResult.get().getKiller(), testCase.expectedParseResult.getKiller());
        assertEquals(deathMessageParseResult.get().getWeapon(), testCase.expectedParseResult.getWeapon());
    }

    @Data
    @Builder
    private static class DeathMessageTestCase {
        private final String rawInput;
        private final DeathMessageParseResult expectedParseResult;
    }
}
