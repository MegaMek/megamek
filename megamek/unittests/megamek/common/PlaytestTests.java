package megamek.common;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.TripodMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static megamek.common.ToHitData.HIT_KICK;
import static megamek.common.ToHitData.HIT_NORMAL;
import static megamek.common.ToHitData.HIT_PUNCH;
import static megamek.common.ToHitData.SIDE_LEFT;
import static megamek.common.ToHitData.SIDE_RIGHT;
import static megamek.common.units.Mek.LOC_CENTER_LEG;
import static megamek.common.units.Mek.LOC_CENTER_TORSO;
import static megamek.common.units.Mek.LOC_HEAD;
import static megamek.common.units.Mek.LOC_LEFT_ARM;
import static megamek.common.units.Mek.LOC_LEFT_TORSO;
import static megamek.common.units.Mek.LOC_RIGHT_ARM;
import static megamek.common.units.Mek.LOC_RIGHT_LEG;
import static megamek.common.units.Mek.LOC_RIGHT_TORSO;
import static org.junit.jupiter.api.Assertions.*;

public class PlaytestTests {
    @BeforeAll
    static void setUpAll() {
        // Need equipment initialized
        EquipmentType.initializeTypes();
    }

    @Test
    void testBipedSideTable() {
        Mek mek = new BipedMek();
        for (int i = 0; i < 100; i++) {
            var location = mek.getPlaytestSideLocation(HIT_NORMAL, SIDE_LEFT, LosEffects.COVER_NONE).getLocation();
            assertNotEquals(LOC_RIGHT_ARM, location);
            assertNotEquals(LOC_RIGHT_LEG, location);
            assertNotEquals(LOC_RIGHT_TORSO, location);
        }
    }

    @Test
    void testTripodKickSideTable() {
        Mek mek = new TripodMek();
        for (int i = 0; i < 100; i++) {
            var location = mek.getPlaytestSideLocation(HIT_KICK, SIDE_RIGHT, LosEffects.COVER_NONE).getLocation();
            assertTrue(location == LOC_RIGHT_LEG || location == LOC_CENTER_LEG);
        }
    }

    @Test
    void testQuadPunchSideTable() {
        Mek mek = new QuadMek();
        for (int i = 0; i < 100; i++) {
            var location = mek.getPlaytestSideLocation(HIT_PUNCH, SIDE_LEFT, LosEffects.COVER_NONE).getLocation();
            assertTrue(location == LOC_LEFT_ARM || location == LOC_LEFT_TORSO || location == LOC_HEAD || location == LOC_CENTER_TORSO);
        }
    }
}
