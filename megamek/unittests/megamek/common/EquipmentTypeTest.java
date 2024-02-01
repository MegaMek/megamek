package megamek.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EquipmentTypeTest {
    @Test
    public void structureCostArraySameLengthAsStructureNames() {
        assertEquals(EquipmentType.structureCosts.length, EquipmentType.structureNames.length);
    }

    @Test
    public void armorCostArraySameLengthAsArmorNames() {
        assertEquals(EquipmentType.armorCosts.length, EquipmentType.armorNames.length);
    }
}
