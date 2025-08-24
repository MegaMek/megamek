/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import java.io.File;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.HandheldWeapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BLKHandheldWeaponFileTest {

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    @Test
    void testLoadHandheldWeaponBLK() throws Exception {
        File f;
        MekFileParser mfp;
        HandheldWeapon e;


        f = new File("testresources/megamek/common/units/TestHandheldWeapon.blk");
        mfp = new MekFileParser(f);
        e = (HandheldWeapon) mfp.getEntity();
        assertEquals(10, e.getOArmor(HandheldWeapon.LOC_GUN), "Failed to load tonnage");
        assertEquals(2, e.getEquipment().size(), "Failed to load equipment");
        assertEquals(6, e.getAmmo().get(0).getOriginalShots(), "Failed to load ammo");
        assertEquals(12.0, e.getWeight(), "Failed to load weight");

    }
}
