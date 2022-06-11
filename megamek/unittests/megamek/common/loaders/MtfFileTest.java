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
package megamek.common.loaders;

import megamek.common.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MtfFileTest {

    private MtfFile toMtfFile(Mech mech) throws EntityLoadingException {
        if (!mech.hasEngine() || mech.getEngine().getEngineType() == Engine.NONE) {
            mech.setWeight(20.0);
            mech.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));
        }
        String mtf = mech.getMtf();
        byte[] bytes = mtf.getBytes();
        InputStream istream = new ByteArrayInputStream(bytes);
        return new MtfFile(istream);
    }

    @Test
    public void testLoadEquipment() throws Exception {
        Mech mech = new BipedMech();
        Mounted mount = new Mounted(mech, EquipmentType.get("Medium Laser"));
        mount.setOmniPodMounted(true);
        mount.setMechTurretMounted(true);
        mount.setArmored(true);
        mech.addEquipment(mount, Mech.LOC_LT, true);

        MtfFile loader = toMtfFile(mech);
        Mounted found = loader.getEntity().getCritical(Mech.LOC_LT, 0).getMount();

        assertEquals(mount.getType(), found.getType());
        assertTrue(found.isRearMounted());
        assertTrue(found.isMechTurretMounted());
        assertTrue(found.isArmored());
    }

    @Test
    public void setVGLFacing() throws Exception {
        Mech mech = new BipedMech();
        EquipmentType vgl = EquipmentType.get("ISVehicularGrenadeLauncher");
        mech.addEquipment(vgl, Mech.LOC_LT).setFacing(0);
        mech.addEquipment(vgl, Mech.LOC_LT).setFacing(1);
        mech.addEquipment(vgl, Mech.LOC_LT).setFacing(2);
        mech.addEquipment(vgl, Mech.LOC_LT, true).setFacing(3);
        mech.addEquipment(vgl, Mech.LOC_LT).setFacing(4);
        mech.addEquipment(vgl, Mech.LOC_LT).setFacing(5);

        MtfFile loader = toMtfFile(mech);
        Entity loaded = loader.getEntity();

        assertEquals(0, loaded.getCritical(Mech.LOC_LT, 0).getMount().getFacing());
        assertEquals(1, loaded.getCritical(Mech.LOC_LT, 1).getMount().getFacing());
        assertEquals(2, loaded.getCritical(Mech.LOC_LT, 2).getMount().getFacing());
        assertEquals(3, loaded.getCritical(Mech.LOC_LT, 3).getMount().getFacing());
        assertEquals(4, loaded.getCritical(Mech.LOC_LT, 4).getMount().getFacing());
        assertEquals(5, loaded.getCritical(Mech.LOC_LT, 5).getMount().getFacing());
    }

    @Test
    public void loadSuperheavyDoubleSlot() throws Exception {
        Mech mech = new BipedMech();
        mech.setWeight(120.0);
        mech.setEngine(new Engine(360, Engine.NORMAL_ENGINE, 0));
        EquipmentType hs = EquipmentType.get(EquipmentTypeLookup.SINGLE_HS);
        mech.addEquipment(hs, hs, Mech.LOC_LT, true, true);

        MtfFile loader = toMtfFile(mech);
        CriticalSlot slot = loader.getEntity().getCritical(Mech.LOC_LT, 0);

        assertEquals(hs, slot.getMount().getType());
        assertEquals(hs, slot.getMount2().getType());
        assertTrue(slot.getMount().isOmniPodMounted());
        assertTrue(slot.getMount2().isOmniPodMounted());
        assertTrue(slot.isArmored());
    }
}
