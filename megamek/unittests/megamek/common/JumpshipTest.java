package megamek.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JumpshipTest {

    @Test
    public void calculateArmorWeightISWithClanArmor() {
        final Jumpship ship = new Jumpship();
        ship.setWeight(100000); // 1.0 for Clan, 0.8 for IS
        ship.set0SI(0); // ignore the extra armor from SI
        ship.setTechLevel(TechConstants.T_IS_ADVANCED);
        ship.setMixedTech(true);
        ship.setArmorType(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_AEROSPACE, true));
        ship.setArmorTechLevel(TechConstants.T_CLAN_ADVANCED);
        for (int loc = 0; loc < 6; loc++) {
            ship.initializeArmor(100, loc);
        }

        assertEquals(600.0, ship.getArmorWeight(ship.locations()), 0.1);
    }

    @Test
    public void calculateArmorWeightClanWithISArmor() {
        final Jumpship ship = new Jumpship();
        ship.setWeight(100000); // 1.0 for Clan, 0.8 for IS
        ship.set0SI(0); // ignore the extra armor from SI
        ship.setTechLevel(TechConstants.T_CLAN_ADVANCED);
        ship.setMixedTech(true);
        ship.setArmorType(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_AEROSPACE, false));
        ship.setArmorTechLevel(TechConstants.T_IS_ADVANCED);
        for (int loc = 0; loc < 6; loc++) {
            ship.initializeArmor(100, loc);
        }

        assertEquals(RoundWeight.nextHalfTon(600.0 / 0.8), ship.getArmorWeight(ship.locations()), 0.1);
    }
}
