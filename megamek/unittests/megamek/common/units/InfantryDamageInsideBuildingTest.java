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

import megamek.common.enums.BuildingType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Issue #8901: the Infantry Damage From Attacks Inside Buildings Table (TW p. 175) has light and medium buildings
 * absorbing nothing, heavy buildings 25 percent and hardened buildings 50 percent of an attack made from inside the
 * building against infantry in it.
 */
class InfantryDamageInsideBuildingTest {

    @ParameterizedTest(name = "{0} building absorbs {1}")
    @CsvSource({ "LIGHT, 0.0", "MEDIUM, 0.0", "HEAVY, 0.25", "HARDENED, 0.5" })
    void buildingAbsorbsThePrintedShareOfAnAttackFromInside(BuildingType buildingType, double absorbed) {
        IBuilding building = new BuildingEntity(buildingType, IBuilding.STANDARD);

        assertEquals(absorbed, building.getInfDmgFromInside(), 1e-9);
    }
}
