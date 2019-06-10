package megamek.common.verifier;

import megamek.common.MiscType;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class TestSupportVehicleTest {

    @Test
    public void testChassisModLookup() {
        for (TestSupportVehicle.ChassisModification mod : TestSupportVehicle.ChassisModification.values()) {
            assertNotNull(mod.equipment);
            assertTrue(mod.equipment.hasFlag(MiscType.F_SUPPORT_TANK_EQUIPMENT));
            assertTrue(mod.equipment.hasFlag(MiscType.F_CHASSIS_MODIFICATION));
        }
    }

}