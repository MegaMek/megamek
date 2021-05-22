/*
 * Copyright 2020 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

import org.junit.Test;

import static org.junit.Assert.*;

public class MechFileParserTest {

    @Test
    public void splitMGsBetweenMGAs() throws LocationFullException {
        Mech mech = new BipedMech();
        Mounted mga1 = mech.addEquipment(EquipmentType.get("ISMGA"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISMG"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISMG"), Mech.LOC_LT);
        Mounted mga2 = mech.addEquipment(EquipmentType.get("ISMGA"), Mech.LOC_LT);
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
        Mounted mga1 = mech.addEquipment(EquipmentType.get("ISLMGA"), Mech.LOC_LT);
        Mounted mga2 = mech.addEquipment(EquipmentType.get("ISMGA"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISMG"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISLightMG"), Mech.LOC_LT);
        mech.addEquipment(EquipmentType.get("ISLightMG"), Mech.LOC_LT);
        Mounted lastMG = mech.addEquipment(EquipmentType.get("ISMG"), Mech.LOC_LT);

        MechFileParser.linkMGAs(mech);

        // The first MGA should load the second and third, and the second MGA only the first
        assertEquals(2, mga1.getBayWeapons().size());
        assertEquals(1, mga2.getBayWeapons().size());
        assertFalse(mech.hasLinkedMGA(lastMG));
    }
}