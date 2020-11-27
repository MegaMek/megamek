package megamek.common;

import static org.junit.Assert.*;

import org.junit.Test;

public class EquipmentTypeTest {

    @Test
    public void structureCostArraySameLengthAsStructureNames() {
        assertEquals(EquipmentType.structureCosts.length, EquipmentType.structureNames.length);
    }

    @Test
    public void armorCostArraySameLengthAsArmorNames() {
        assertEquals(EquipmentType.armorCosts.length, EquipmentType.armorNames.length);
    }

    @Test
    public void armorPointMultiplierArraySameLengthAsArmorNames() {
        assertEquals(EquipmentType.armorPointMultipliers.length, EquipmentType.armorNames.length);
    }

}
