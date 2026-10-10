/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Large craft round the mass of their ammunition up to the nearest ton, whatever the number of rounds carried. A bin
 * loaded from a unit file by round count is sized the same way.
 */
class LargeCraftAmmoBinSizeTest {
    private static final int GAUSS_SHOTS_PER_TON = 8;

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    private static double binSizeFor(int rounds) throws Exception {
        Dropship dropship = new Dropship();
        AmmoMounted ammo = (AmmoMounted) dropship.addEquipment(EquipmentType.get("IS Gauss Ammo"),
              Dropship.LOC_NOSE, false, rounds);
        return ammo.getSize();
    }

    @Test
    void aPartTonOfRoundsTakesAWholeTon() throws Exception {
        // the Union-X carries 60 Gauss rounds: 7.5 tons, rounded up to 8
        assertEquals(8.0, binSizeFor(60));
    }

    @Test
    void roundsFillingWholeTonsAreNotRoundedFurther() throws Exception {
        assertEquals(7.0, binSizeFor(7 * GAUSS_SHOTS_PER_TON));
    }

    @Test
    void fewerRoundsThanATonStillTakeOneTon() throws Exception {
        assertEquals(1.0, binSizeFor(5));
    }
}
