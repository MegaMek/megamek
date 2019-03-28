package megamek.common.verifier;

import megamek.common.EquipmentType;
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
            EquipmentType eq = EquipmentType.get(mod.eqTypeKey);
            assertNotNull(eq);
            assertTrue(eq.hasFlag(MiscType.F_SUPPORT_TANK_EQUIPMENT));
            assertTrue(eq.hasFlag(MiscType.F_CHASSIS_MODIFICATION));
        }
    }

}