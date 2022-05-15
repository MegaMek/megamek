package megamek.common.verifier;

import megamek.common.MiscType;
import megamek.common.verifier.TestSupportVehicle.ChassisModification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSupportVehicleTest {

    @Test
    public void testChassisModLookup() {
        for (ChassisModification mod : ChassisModification.values()) {
            assertNotNull(mod.equipment);
            assertTrue(mod.equipment.hasFlag(MiscType.F_SUPPORT_TANK_EQUIPMENT));
            assertTrue(mod.equipment.hasFlag(MiscType.F_CHASSIS_MODIFICATION));
        }
    }
}
