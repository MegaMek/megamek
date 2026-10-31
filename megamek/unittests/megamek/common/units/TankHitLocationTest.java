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

import java.util.stream.Stream;

import megamek.common.HitData;
import megamek.common.ToHitData;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Tests the 5 and 9 rows of the Ground Combat Vehicle Hit Location Table (TW p.193). These are the rows that send a hit
 * to a different side from the one it came in on. Regression for #8850: a 5 from the front hit the left side instead of
 * the right.
 */
class TankHitLocationTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    static Stream<Arguments> tableRows() {
        return Stream.of(
              Arguments.of(ToHitData.SIDE_FRONT, 5, Tank.LOC_RIGHT),
              Arguments.of(ToHitData.SIDE_REAR, 5, Tank.LOC_LEFT),
              Arguments.of(ToHitData.SIDE_LEFT, 5, Tank.LOC_FRONT),
              Arguments.of(ToHitData.SIDE_RIGHT, 5, Tank.LOC_FRONT),
              Arguments.of(ToHitData.SIDE_FRONT, 9, Tank.LOC_LEFT),
              Arguments.of(ToHitData.SIDE_REAR, 9, Tank.LOC_RIGHT),
              Arguments.of(ToHitData.SIDE_LEFT, 9, Tank.LOC_REAR),
              Arguments.of(ToHitData.SIDE_RIGHT, 9, Tank.LOC_REAR));
    }

    @ParameterizedTest(name = "attack from side {0}, roll {1}")
    @MethodSource("tableRows")
    void hitLandsOnTheSideTheTableGives(int attackSide, int roll, int expectedLocation) {
        Tank tank = new Tank();
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.d6(2)).thenReturn(roll);

            HitData hit = tank.rollHitLocation(ToHitData.HIT_NORMAL, attackSide);

            assertEquals(expectedLocation, hit.getLocation());
        }
    }
}
