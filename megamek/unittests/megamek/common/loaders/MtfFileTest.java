/*
 * MegaMek
 * Copyright (C) 2020 The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package megamek.common.loaders;

import megamek.common.*;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

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
    public void testLoadEquipment() throws LocationFullException, EntityLoadingException {
        Mech mech = new BipedMech();
        EquipmentType laser = EquipmentType.get("Medium Laser");
        Mounted mount = mech.addEquipment(laser, Mech.LOC_LT, true);
        mount.setOmniPodMounted(true);
        mount.setMechTurretMounted(true);
        mount.setArmored(true);

        MtfFile loader = toMtfFile(mech);
        Mounted found = loader.getEntity().getCritical(Mech.LOC_LT, 0).getMount();

        assertEquals(laser, found.getType());
        assertTrue(found.isRearMounted());
        assertTrue(found.isMechTurretMounted());
    }
}