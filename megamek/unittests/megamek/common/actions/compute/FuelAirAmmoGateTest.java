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
package megamek.common.actions.compute;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.AmmoType.AmmoTypeEnum;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Airborne units may only employ fuel-air munitions as bombs (IO:AE p.159), so firing fuel-air artillery ammo from the
 * air is impossible (issue #9317).
 */
class FuelAirAmmoGateTest {

    private static AmmoType fuelAirArrow;
    private static AmmoType standardArrow;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
        for (AmmoType ammo : AmmoType.getMunitionsFor(AmmoTypeEnum.ARROW_IV)) {
            if (ammo.isClan()) {
                continue;
            }
            if (ammo.getMunitionType().contains(AmmoType.Munitions.M_FAE)) {
                fuelAirArrow = ammo;
            } else if (ammo.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)) {
                standardArrow = ammo;
            }
        }
    }

    private static Entity attacker(boolean airborne, boolean airborneVtol) {
        Entity attacker = mock(Entity.class);
        doReturn(airborne).when(attacker).isAirborne();
        doReturn(airborneVtol).when(attacker).isAirborneVTOLorWIGE();
        return attacker;
    }

    @Test
    void airborneFighterCannotFireFuelAirArtillery() {
        assertNotNull(fuelAirArrow, "the IS Arrow IV has a fuel-air round");
        assertNotNull(ComputeToHitIsImpossible.fuelAirAmmoGateReason(attacker(true, false), fuelAirArrow, true));
    }

    @Test
    void airborneVtolCannotFireFuelAirArtillery() {
        assertNotNull(ComputeToHitIsImpossible.fuelAirAmmoGateReason(attacker(false, true), fuelAirArrow, true));
    }

    @Test
    void groundUnitMayFireFuelAirArtillery() {
        assertNull(ComputeToHitIsImpossible.fuelAirAmmoGateReason(attacker(false, false), fuelAirArrow, true));
    }

    @Test
    void airborneUnitMayFireOtherArtilleryAmmo() {
        assertNotNull(standardArrow, "the IS Arrow IV has a standard round");
        assertNull(ComputeToHitIsImpossible.fuelAirAmmoGateReason(attacker(true, false), standardArrow, true));
    }
}
