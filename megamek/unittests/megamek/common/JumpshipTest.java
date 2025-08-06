/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JumpshipTest {
    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void calculateArmorWeightISWithClanArmor() {
        final Jumpship ship = new Jumpship();
        ship.setWeight(100000); // 1.0 for Clan, 0.8 for IS
        ship.setOSI(0); // ignore the extra armor from SI
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
        ship.setOSI(0); // ignore the extra armor from SI
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
