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
package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import megamek.common.CriticalSlot;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.TripodMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MtfFileTest {
    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private MtfFile toMtfFile(Mek mek) throws EntityLoadingException {
        if (!mek.hasEngine() || mek.getEngine().getEngineType() == Engine.NONE) {
            mek.setWeight(20.0);
            mek.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));
        }
        String mtf = mek.getMtf();
        byte[] bytes = mtf.getBytes();
        InputStream inputStream = new ByteArrayInputStream(bytes);
        return new MtfFile(inputStream);
    }

    @Test
    void testLoadEquipment() throws Exception {
        Mek mek = new BipedMek();
        Mounted<?> mount = Mounted.createMounted(mek, EquipmentType.get("Medium Laser"));
        mount.setOmniPodMounted(true);
        mount.setMekTurretMounted(true);
        mount.setArmored(true);
        mek.addEquipment(mount, Mek.LOC_LEFT_TORSO, true);

        MtfFile loader = toMtfFile(mek);
        Mounted<?> found = loader.getEntity().getCritical(Mek.LOC_LEFT_TORSO, 0).getMount();

        assertEquals(mount.getType(), found.getType());
        assertTrue(found.isRearMounted());
        assertTrue(found.isMekTurretMounted());
        assertTrue(found.isArmored());
    }

    @Test
    void setVGLFacing() throws Exception {
        Mek mek = new BipedMek();
        EquipmentType vgl = EquipmentType.get("ISVehicularGrenadeLauncher");
        mek.addEquipment(vgl, Mek.LOC_LEFT_TORSO).setFacing(0);
        mek.addEquipment(vgl, Mek.LOC_LEFT_TORSO).setFacing(1);
        mek.addEquipment(vgl, Mek.LOC_LEFT_TORSO).setFacing(2);
        mek.addEquipment(vgl, Mek.LOC_LEFT_TORSO, true).setFacing(3);
        mek.addEquipment(vgl, Mek.LOC_LEFT_TORSO).setFacing(4);
        mek.addEquipment(vgl, Mek.LOC_LEFT_TORSO).setFacing(5);

        MtfFile loader = toMtfFile(mek);
        Entity loaded = loader.getEntity();

        assertEquals(0, loaded.getCritical(Mek.LOC_LEFT_TORSO, 0).getMount().getFacing());
        assertEquals(1, loaded.getCritical(Mek.LOC_LEFT_TORSO, 1).getMount().getFacing());
        assertEquals(2, loaded.getCritical(Mek.LOC_LEFT_TORSO, 2).getMount().getFacing());
        assertEquals(3, loaded.getCritical(Mek.LOC_LEFT_TORSO, 3).getMount().getFacing());
        assertEquals(4, loaded.getCritical(Mek.LOC_LEFT_TORSO, 4).getMount().getFacing());
        assertEquals(5, loaded.getCritical(Mek.LOC_LEFT_TORSO, 5).getMount().getFacing());
    }

    @Test
    void loadSuperheavyDoubleSlot() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(120.0);
        mek.setEngine(new Engine(360, Engine.NORMAL_ENGINE, 0));
        EquipmentType hs = EquipmentType.get(EquipmentTypeLookup.SINGLE_HS);
        mek.addEquipment(hs, hs, Mek.LOC_LEFT_TORSO, true, true);

        MtfFile loader = toMtfFile(mek);
        CriticalSlot slot = loader.getEntity().getCritical(Mek.LOC_LEFT_TORSO, 0);

        assertEquals(hs, slot.getMount().getType());
        assertEquals(hs, slot.getMount2().getType());
        assertTrue(slot.getMount().isOmniPodMounted());
        assertTrue(slot.getMount2().isOmniPodMounted());
        assertTrue(slot.isArmored());
    }

    // Exercises new MtfFile.java code
    // We should be able to load a Size 24 CommsGear component into 12 Superheavy
    // slots, filling
    // the Left torso.
    @Test
    void loadSuperheavyVariableSizeSlot() throws Exception {
        Mek mek = new TripodMek();
        double varSize = 24.0;
        mek.setWeight(150.0);
        mek.setEngine(new Engine(300, Engine.NORMAL_ENGINE, 0));
        EquipmentType commsGear = EquipmentType.get("CommsGear");
        Mounted<?> mount = mek.addEquipment(commsGear, Mek.LOC_LEFT_TORSO, false);
        mount.setSize(varSize);

        MtfFile loader = toMtfFile(mek);
        CriticalSlot slot = loader.getEntity().getCritical(Mek.LOC_LEFT_TORSO, 0);

        assertEquals(commsGear, slot.getMount().getType());
        assertEquals(varSize, slot.getMount().getSize());
        assertFalse(slot.getMount().isOmniPodMounted());
        assertFalse(slot.isArmored());
    }

    // Should _not_ allow loading size 25 CommsGear; 25 / 2.0 -> 13 crits, 1 more
    // than allowed
    @Test
    void ExceptionLoadSuperheavyVariableSizeSlot() throws Exception {
        Mek mek = new TripodMek();
        double varSize = 25.0;
        mek.setWeight(150.0);
        mek.setEngine(new Engine(300, Engine.NORMAL_ENGINE, 0));
        EquipmentType commsGear = EquipmentType.get("CommsGear");
        Mounted<?> mount = mek.addEquipment(commsGear, Mek.LOC_LEFT_TORSO, false);
        mount.setSize(varSize);
        MtfFile loader = toMtfFile(mek);

        Exception e = assertThrowsExactly(
              Exception.class,
              () -> loader.getEntity().getCritical(Mek.LOC_LEFT_TORSO, 0));
        assertEquals(
              "java.lang.ArrayIndexOutOfBoundsException: Index 12 out of bounds for length 12",
              e.getMessage());

    }
}
