package megamek.common.verifier;

import megamek.common.*;
import megamek.common.equipment.ArmorType;
import megamek.common.verifier.TestSupportVehicle.ChassisModification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static megamek.common.EquipmentType.T_ARMOR_FERRO_FIBROUS;
import static org.junit.jupiter.api.Assertions.*;

public class TestSupportVehicleTest {

    @BeforeAll
    public static void initialize() {
        EquipmentType.initializeTypes();
    }

    @Test
    public void testChassisModLookup() {
        for (ChassisModification mod : ChassisModification.values()) {
            assertNotNull(mod.equipment);
            assertTrue(mod.equipment.hasFlag(MiscType.F_SUPPORT_TANK_EQUIPMENT));
            assertTrue(mod.equipment.hasFlag(MiscType.F_CHASSIS_MODIFICATION));
        }
    }

    @Test
    public void testBAR10ArmorCorrectSlots() {
        SupportTank st = new SupportTank();
        st.setArmorType(EquipmentType.T_ARMOR_SV_BAR_10);
        // Rating E should return CV slots for IS FF
        st.setArmorTechRating(ITechnology.RATING_E);
        assertEquals(
                2,
                ArmorType.of(T_ARMOR_FERRO_FIBROUS, false).getSupportVeeSlots(st)
        );

        // Rating F should return CV slots for Clan FF
        st.setArmorTechRating(ITechnology.RATING_F);
        assertEquals(
                1,
                ArmorType.of(T_ARMOR_FERRO_FIBROUS, true).getSupportVeeSlots(st)
        );
    }
}
