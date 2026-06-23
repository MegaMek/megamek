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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Coolant Ammo heat-reduction rule for flamers (TO:AUE p.173): a Heavy Flamer removes 4
 * heat per hit, a standard Vehicle Flamer removes 3.
 */
class VehicleFlamerCoolHandlerTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @Test
    void heavyFlamerRemovesFourHeat() {
        assertEquals(4, VehicleFlamerCoolHandler.coolingPointsFor(new ISHeavyFlamer()),
              "An IS Heavy Flamer firing Coolant removes 4 heat per hit");
        assertEquals(4, VehicleFlamerCoolHandler.coolingPointsFor(new CLHeavyFlamer()),
              "A Clan Heavy Flamer firing Coolant removes 4 heat per hit");
    }

    @Test
    void standardVehicleFlamerRemovesThreeHeat() {
        assertEquals(3, VehicleFlamerCoolHandler.coolingPointsFor(new ISVehicleFlamer()),
              "A standard Vehicle Flamer firing Coolant removes 3 heat per hit");
    }
}
