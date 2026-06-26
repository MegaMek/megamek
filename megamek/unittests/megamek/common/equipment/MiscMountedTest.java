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
package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.Messages;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MiscMounted} mine-dispenser state: mine type, EMP weight threshold, and description. Guards the fix
 * for issue #8359 (dispensers must support all land-type mines, not just Conventional and Vibrabomb).
 */
class MiscMountedTest {

    private MiscMounted newDispenser() {
        Entity entity = mock(Entity.class);
        MiscType type = mock(MiscType.class);
        when(type.hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)).thenReturn(true);
        return new MiscMounted(entity, type);
    }

    @Test
    void vehicleMineDispenserDefaultsToConventionalWithTwoShots() {
        MiscMounted dispenser = newDispenser();

        assertEquals(MiscMounted.MINE_CONVENTIONAL, dispenser.getMineType());
        assertEquals(2, dispenser.getUsableShotsLeft());
    }

    @Test
    void empSettingDefaultsToTenAndRoundTrips() {
        MiscMounted dispenser = newDispenser();

        assertEquals(10, dispenser.getEmpSetting());

        dispenser.setEmpSetting(75);
        assertEquals(75, dispenser.getEmpSetting());
    }

    @Test
    void mineTypeRoundTripsForEachLandType() {
        MiscMounted dispenser = newDispenser();

        for (int mineType : new int[] { MiscMounted.MINE_CONVENTIONAL, MiscMounted.MINE_VIBRABOMB,
                                        MiscMounted.MINE_ACTIVE, MiscMounted.MINE_INFERNO, MiscMounted.MINE_EMP,
                                        MiscMounted.MINE_COMMAND_DETONATED }) {
            dispenser.setMineType(mineType);
            assertEquals(mineType, dispenser.getMineType());
        }
    }

    @Test
    void getBaseDescReturnsEmpNameForEmpMine() {
        MiscMounted dispenser = newDispenser();
        dispenser.setMineType(MiscMounted.MINE_EMP);

        assertEquals(Messages.getString("Mounted.EMPMine"), dispenser.getBaseDesc());
    }
}
