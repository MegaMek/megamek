package megamek.common.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.rules.core.CoreRulesPhysical;
import megamek.common.rules.totalwarfare.TWRulesPhysical;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RulesPhysicalTest {
    @Test
    void coreAndTotalWarfareRulesExercisePhysicalLogic() {
        CoreRulesPhysical core = new CoreRulesPhysical();
        TWRulesPhysical total = new TWRulesPhysical();
        Entity attacker = Mockito.mock(Entity.class);
        Entity target = Mockito.mock(Entity.class);
        Mounted<?> mounted = Mockito.mock(Mounted.class);
        ToHitData toHit = new ToHitData();
        Game game = Mockito.mock(Game.class);

        Mockito.when(attacker.getWeight()).thenReturn(50.0);
        Mockito.when(target.getWeight()).thenReturn(40.0);
        Mockito.when(attacker.getLocationStatus(1)).thenReturn(0);

        assertNotNull(core);
        assertNotNull(total);
        assertEquals(0, core.getShieldDamageBoost(attacker, 0));
        assertEquals(0, total.getShieldDamageBoost(attacker, 1));
        assertEquals(0, core.getClawToHitModifier());
        assertEquals(1, total.getClawToHitModifier());
        assertEquals(-1, core.getKickModifier());
        assertEquals(-2, total.getKickModifier());
        assertEquals(-1, core.getPunchModifier());
        assertEquals(0, total.getPunchModifier());
        assertTrue(core.getChargeDamage(attacker, target, false, 3, 2) >= 0);
        assertTrue(total.getChargeDamage(attacker, target, true, 3, 2) >= 0);
        assertTrue(core.getChargeDamageTakenBy(attacker, 5.0, false, 2) >= 0);
        assertTrue(total.getChargeDamageTakenBy(attacker, 6.0, true, 2) >= 0);
        assertEquals(-1, core.getPilotDiffModifier(4, 5, false));
        assertEquals(1, total.getPilotDiffModifier(5, 4, true));
        assertTrue(core.getClubFindInRubble() >= 1);
        assertTrue(total.getClubFindInRubble() >= 1);
        assertEquals(9, core.getLanceTarget());
        assertEquals(10, total.getLanceTarget());
        assertTrue(core.canChargeCancel());
        assertTrue(!total.canChargeCancel());
    }
}
