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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.equipment.EquipmentType;
import megamek.common.weapons.flamers.clan.CLHeavyFlamer;
import megamek.common.weapons.flamers.innerSphere.ISHeavyFlamer;
import megamek.common.weapons.flamers.innerSphere.ISVehicleFlamer;
import megamek.common.weapons.other.innerSphere.ISFluidGun;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Inferno Fuel missile-delivery rule (TO:AUE p.173): a Heavy Flamer delivers two Inferno
 * SRM missiles per hit, while a standard Vehicle Flamer or Fluid Gun delivers one.
 */
class InfernoFuelHandlerTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void heavyFlamerDeliversTwoInfernoMissiles() {
        assertEquals(2, InfernoFuelHandler.infernoMissilesFor(new ISHeavyFlamer()),
              "An IS Heavy Flamer firing Inferno Fuel delivers 2 Inferno SRM missiles");
        assertEquals(2, InfernoFuelHandler.infernoMissilesFor(new CLHeavyFlamer()),
              "A Clan Heavy Flamer firing Inferno Fuel delivers 2 Inferno SRM missiles");
    }

    @Test
    void standardFlamerAndFluidGunDeliverOneInfernoMissile() {
        assertEquals(1, InfernoFuelHandler.infernoMissilesFor(new ISVehicleFlamer()),
              "A standard Vehicle Flamer firing Inferno Fuel delivers 1 Inferno SRM missile");
        assertEquals(1, InfernoFuelHandler.infernoMissilesFor(new ISFluidGun()),
              "A Fluid Gun firing Inferno Fuel delivers 1 Inferno SRM missile");
    }
}
