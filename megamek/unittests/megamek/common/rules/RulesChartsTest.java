package megamek.common.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.ToHitData;
import megamek.common.rules.core.CoreRulesCharts;
import megamek.common.rules.totalwarfare.TWRulesCharts;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;

class RulesChartsTest {
    @Test
    void coreAndTotalWarfareRulesExerciseChartLogic() {
        CoreRulesCharts core = new CoreRulesCharts();
        TWRulesCharts total = new TWRulesCharts();

        assertNotNull(core);
        assertNotNull(total);
        assertEquals(2, core.escalatingFailure(0));
        assertEquals(11, total.escalatingFailure(6));
        assertTrue(core.getLocationName(Mek.LOC_LEFT_ARM, false).length() >= 0);
        assertTrue(total.getLocationName(Mek.LOC_RIGHT_LEG, true).length() >= 0);
        assertEquals(Mek.LOC_LEFT_TORSO, core.getPunchHitLocationSide(1, ToHitData.SIDE_LEFT, false));
        assertEquals(Mek.LOC_HEAD, total.getPunchHitLocationSide(6, ToHitData.SIDE_RIGHT, true));
        assertEquals(Mek.LOC_LEFT_ARM, core.getPunchHitLocation(5, ToHitData.SIDE_LEFT));
        assertEquals(Mek.LOC_HEAD, total.getPunchHitLocation(6, ToHitData.SIDE_RIGHT, true));
    }
}
