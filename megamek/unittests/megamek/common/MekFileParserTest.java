/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import megamek.common.equipment.WeaponMounted;

class MekFileParserTest {

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
