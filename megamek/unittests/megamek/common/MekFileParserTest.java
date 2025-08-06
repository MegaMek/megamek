/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertFalse;

import megamek.common.equipment.WeaponMounted;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MekFileParserTest {
    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void splitMGsBetweenMGAs() throws LocationFullException {
        Mek mek = new BipedMek();
        WeaponMounted mga1 = (WeaponMounted) mek.addEquipment(EquipmentType.get("ISMGA"), Mek.LOC_LT);
        mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LT);
        mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LT);
        WeaponMounted mga2 = (WeaponMounted) mek.addEquipment(EquipmentType.get("ISMGA"), Mek.LOC_LT);
        mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LT);
        mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LT);
        mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LT);

        MekFileParser.linkMGAs(mek);

        assertEquals(2, mga1.getBayWeapons().size());
        assertEquals(3, mga2.getBayWeapons().size());
    }

    @Test
    void loadMGAsFromContiguousBlocks() throws LocationFullException {
        Mek mek = new BipedMek();
        WeaponMounted mga1 = (WeaponMounted) mek.addEquipment(EquipmentType.get("ISLMGA"), Mek.LOC_LT);
        WeaponMounted mga2 = (WeaponMounted) mek.addEquipment(EquipmentType.get("ISMGA"), Mek.LOC_LT);
        mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LT);
        mek.addEquipment(EquipmentType.get("ISLightMG"), Mek.LOC_LT);
        mek.addEquipment(EquipmentType.get("ISLightMG"), Mek.LOC_LT);
        WeaponMounted lastMG = (WeaponMounted) mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LT);

        MekFileParser.linkMGAs(mek);

        // The first MGA should load the second and third, and the second MGA only the
        // first
        assertEquals(2, mga1.getBayWeapons().size());
        assertEquals(1, mga2.getBayWeapons().size());
        assertFalse(mek.hasLinkedMGA(lastMG));
    }
}
