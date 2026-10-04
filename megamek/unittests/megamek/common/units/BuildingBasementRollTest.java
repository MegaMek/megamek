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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.rolls.Roll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Issue #8902: the Basements Table (TW p. 179) gives a small basement, which only infantry can enter, on a roll of
 * 9. The roll used to count 9 as no basement.
 */
class BuildingBasementRollTest {

    @ParameterizedTest(name = "a roll of {0} gives {1}")
    @CsvSource({
          "2, TWO_DEEP_FEET",
          "3, ONE_DEEP_FEET",
          "4, ONE_DEEP_NORMAL",
          "5, NONE",
          "6, NONE",
          "7, NONE",
          "8, NONE",
          "9, ONE_DEEP_NORMAL_INFANTRY_ONLY",
          "10, ONE_DEEP_NORMAL",
          "11, ONE_DEEP_HEAD",
          "12, TWO_DEEP_HEAD" })
    void basementsTableMatchesTotalWarfare(int roll, BasementType expected) {
        assertEquals(expected, Building.basementTypeForRoll(roll));
    }

    @Test
    void rollingANineGivesTheHexASmallBasement() {
        Building building = new Building(BuildingType.MEDIUM, IBuilding.STANDARD, 1, Terrains.BUILDING);
        building.addHex(CubeCoords.ZERO, 50, 0, BasementType.UNKNOWN, false);
        Roll nine = mock(Roll.class);
        when(nine.getIntValue()).thenReturn(9);
        when(nine.getReport()).thenReturn("9");

        boolean changed;
        try (MockedStatic<Compute> mockedCompute = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            mockedCompute.when(() -> Compute.rollD6(2)).thenReturn(nine);
            changed = building.rollBasement(CubeCoords.ZERO, null, new Vector<Report>());
        }

        assertTrue(changed, "an unknown basement must be resolved by the roll");
        assertEquals(BasementType.ONE_DEEP_NORMAL_INFANTRY_ONLY, building.getBasement(CubeCoords.ZERO));
    }
}
