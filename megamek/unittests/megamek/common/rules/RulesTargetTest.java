package megamek.common.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.LosEffects;
import megamek.common.rules.core.CoreRulesTarget;
import megamek.common.rules.totalwarfare.TWRulesTarget;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RulesTargetTest {
    @Test
    void coreAndTotalWarfareRulesExerciseTargetLogic() {
        CoreRulesTarget core = new CoreRulesTarget();
        TWRulesTarget total = new TWRulesTarget();
        Entity entityTarget = Mockito.mock(Entity.class);

        Mockito.when(entityTarget.getBadCriticalSlots(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(0);

        assertNotNull(core);
        assertNotNull(total);
        assertEquals(-1, core.largeTargetModifier(4, true));
        assertEquals(-1, total.largeTargetModifier(5, false));
        assertEquals(1, core.getSecondaryArcModifier());
        assertEquals(2, total.getSecondaryArcModifier());
        assertEquals(0, core.getArmActuatorHitMod(entityTarget, 0));
        assertEquals(0, total.getArmActuatorHitMod(entityTarget, 1));
        assertTrue(core.getBAPSmokeReduction(new LosEffects()) >= 0);
        assertTrue(total.getBAPSmokeReduction(new LosEffects()) >= 0);
        assertEquals(1, core.getSecondaryTargetModifier());
        assertEquals(1, total.getSecondaryTargetModifier());
    }
}
