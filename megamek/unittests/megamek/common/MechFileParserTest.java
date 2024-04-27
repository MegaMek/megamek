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

import megamek.common.equipment.WeaponMounted;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MechFileParserTest {

    @Test
    public void splitMGsBetweenMGAs() throws LocationFullException {
        Mech mech = new BipedMech();
        WeaponMounted mga1 = (WeaponMounted) mech.addEquipment(EquipmentType.get("ISMGA"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISMG"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISMG"), Mech.LOC_LT);
        WeaponMounted mga2 = (WeaponMounted) mech.addEquipment(EquipmentType.get("ISMGA"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISMG"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISMG"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISMG"), Mech.LOC_LT);

        MechFileParser.linkMGAs(mech);

        assertEquals(2, mga1.getBayWeapons().size());
        assertEquals(3, mga2.getBayWeapons().size());
    }

    @Test
    public void loadMGAsFromContiguousBlocks() throws LocationFullException {
        Mech mech = new BipedMech();
        WeaponMounted mga1 = (WeaponMounted) mech.addEquipment(EquipmentType.get("ISLMGA"), Mech.LOC_LT);
        WeaponMounted mga2 = (WeaponMounted) mech.addEquipment(EquipmentType.get("ISMGA"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISMG"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISLightMG"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISLightMG"), Mech.LOC_LT);
        WeaponMounted lastMG = (WeaponMounted) mech.addEquipment(EquipmentType.get("ISMG"), Mech.LOC_LT);

        MechFileParser.linkMGAs(mech);

        // The first MGA should load the second and third, and the second MGA only the first
        assertEquals(2, mga1.getBayWeapons().size());
        assertEquals(1, mga2.getBayWeapons().size());
        assertFalse(mech.hasLinkedMGA(lastMG));
    }
}
