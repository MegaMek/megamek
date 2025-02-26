/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JumpshipTest {

    @Test
    void calculateArmorWeightISWithClanArmor() {
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
    void calculateArmorWeightClanWithISArmor() {
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
