package megamek.common.rules;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rules.core.CoreRulesPSR;
import megamek.common.rules.totalwarfare.TWRulesPSR;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.MekWithArms;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RulesPSRTest {
    @Test
    void coreAndTotalWarfareRulesExercisePSRLogic() {
        CoreRulesPSR core = new CoreRulesPSR();
        TWRulesPSR total = new TWRulesPSR();
        Entity entity = Mockito.mock(Entity.class);
        MekWithArms arms = Mockito.mock(MekWithArms.class);
        PilotingRollData roll = new PilotingRollData(1, 0, "test", 0);
        Game game = Mockito.mock(Game.class);
        ArrayList<PilotingRollData> list = new ArrayList<>();
        list.add(new PilotingRollData(1, 2, "first", 0));
        list.add(new PilotingRollData(2, 1, "second", 0));

        assertNotNull(core);
        assertNotNull(total);
        core.rollRemoveHighest(list);
        total.rollRemoveHighest(list);
        assertTrue(list.size() <= 2);
        assertTrue(core.getHipPenalty() >= 1);
        assertTrue(total.getHipPenalty() >= 1);
        assertTrue(core.getGyroModifier(1, 0) >= 2);
        assertTrue(total.getGyroModifier(1, 0) >= 2);
        assertTrue(core.getLegDestroyedModifier() >= 4);
        assertTrue(total.getLegDestroyedModifier() >= 4);
        assertTrue(core.getSuccessfulDFAModifier() >= 2);
        assertTrue(total.getSuccessfulDFAModifier() >= 2);
        assertTrue(core.getGyroJumpModifier(1, 0) >= 0);
        assertTrue(total.getGyroJumpModifier(1, 0) >= 0);
        assertTrue(core.psrForWaterEntry(EntityMovementType.MOVE_RUN));
        assertTrue(total.psrForWaterEntry(EntityMovementType.MOVE_RUN));
        assertTrue(core.getGyroModifier(0, 0) >= 2);
        assertTrue(total.getGyroModifier(0, 0) >= 2);
    }
}
