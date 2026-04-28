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
package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.units.Infantry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Mountain Troops infantry elevation movement costs.
 * <p>
 * Per TO:AUE p.153, Mountain Troops "only expend 1 MP per 2 levels moved up or down" instead of the normal infantry
 * cost of 2 MP per level of elevation change.
 * </p>
 */
public class MountainInfantryMovementTest extends GameBoardTestCase {

    private static Infantry createStandardInfantry() {
        Infantry infantry = new Infantry();
        infantry.setId(2);
        infantry.setWeight(2.0f);
        infantry.initializeInternal(10, Infantry.LOC_INFANTRY);
        return infantry;
    }

    private static Infantry createMountainInfantry() {
        Infantry infantry = createStandardInfantry();
        infantry.setSpecializations(Infantry.MOUNTAIN_TROOPS);
        return infantry;
    }

    @Nested
    @DisplayName("Mountain Troops Elevation Movement Cost (TO:AUE p.153)")
    class ElevationMovementCost {

        static {
            // 1-level elevation change: level 0 -> level 1
            initializeBoard("BOARD_1_LEVEL_UP", """
                  size 1 2
                  hex 0101 0 "" ""
                  hex 0102 1 "" ""
                  end""");

            // 2-level elevation change: level 0 -> level 2
            initializeBoard("BOARD_2_LEVEL_UP", """
                  size 1 2
                  hex 0101 0 "" ""
                  hex 0102 2 "" ""
                  end""");

            // 3-level elevation change: level 0 -> level 3
            initializeBoard("BOARD_3_LEVEL_UP", """
                  size 1 2
                  hex 0101 0 "" ""
                  hex 0102 3 "" ""
                  end""");

            // Flat board for baseline MP comparison
            initializeBoard("BOARD_FLAT", """
                  size 1 2
                  hex 0101 0 "" ""
                  hex 0102 0 "" ""
                  end""");
        }

        @Test
        @DisplayName("Flat terrain costs 1 MP for both standard and mountain infantry")
        void flatTerrainCostsSameForBoth() {
            setBoard("BOARD_FLAT");

            MovePath standardPath = getMovePathFor(createStandardInfantry(), 0, null,
                  MoveStepType.FORWARDS);
            MovePath mountainPath = getMovePathFor(createMountainInfantry(), 0, null,
                  MoveStepType.FORWARDS);

            assertTrue(standardPath.isMoveLegal(), "Standard infantry move on flat should be legal");
            assertTrue(mountainPath.isMoveLegal(), "Mountain infantry move on flat should be legal");
            assertEquals(standardPath.getMpUsed(), mountainPath.getMpUsed(),
                  "Flat terrain should cost the same MP for both unit types");
        }

        @Test
        @DisplayName("1-level up: standard infantry pays 2 MP, mountain troops pay 1 MP")
        void oneLevelUpMountainTroopsPayLess() {
            setBoard("BOARD_1_LEVEL_UP");

            // Standard infantry: 1 MP base + (1 level * 2 doubled) = 3 MP
            MovePath standardPath = getMovePathFor(createStandardInfantry(), 0, null,
                  MoveStepType.FORWARDS);
            // Mountain troops: 1 MP base + ceil(1/2) = 1 MP elevation = 2 MP total
            MovePath mountainPath = getMovePathFor(createMountainInfantry(), 0, null,
                  MoveStepType.FORWARDS);

            assertTrue(standardPath.isMoveLegal(), "Standard infantry 1-level climb should be legal");
            assertTrue(mountainPath.isMoveLegal(), "Mountain infantry 1-level climb should be legal");

            int standardMp = standardPath.getMpUsed();
            int mountainMp = mountainPath.getMpUsed();

            assertTrue(mountainMp < standardMp,
                  "Mountain troops should pay less MP than standard infantry for 1-level climb. " +
                        "Mountain: " + mountainMp + " MP, Standard: " + standardMp + " MP");
        }

        @Test
        @DisplayName("2-level up: standard infantry CANNOT climb 2 levels (max is 1)")
        void twoLevelUpStandardInfantryCannotClimb() {
            setBoard("BOARD_2_LEVEL_UP");

            MovePath standardPath = getMovePathFor(createStandardInfantry(), 0, null,
                  MoveStepType.FORWARDS);

            assertFalse(standardPath.isMoveLegal(),
                  "Standard infantry should NOT be able to climb 2 levels (max elevation change is 1)");
        }

        @Test
        @DisplayName("2-level up: mountain troops can climb 2 levels")
        void twoLevelUpMountainTroopsCanClimb() {
            setBoard("BOARD_2_LEVEL_UP");

            MovePath mountainPath = getMovePathFor(createMountainInfantry(), 0, null,
                  MoveStepType.FORWARDS);

            assertTrue(mountainPath.isMoveLegal(),
                  "Mountain troops should be able to climb 2 levels (max elevation change is 3)");
        }

        @Test
        @DisplayName("3-level up: mountain troops can climb 3 levels (their max elevation change)")
        void threeLevelUpMountainTroopsCanClimb() {
            setBoard("BOARD_3_LEVEL_UP");

            MovePath mountainPath = getMovePathFor(createMountainInfantry(), 0, null,
                  MoveStepType.FORWARDS);

            assertTrue(mountainPath.isMoveLegal(),
                  "Mountain troops should be able to climb 3 levels in one hex (their max elevation change)");
        }

        @Test
        @DisplayName("3-level up: standard infantry CANNOT climb 3 levels (max is 1)")
        void threeLevelUpStandardInfantryCannotClimb() {
            setBoard("BOARD_3_LEVEL_UP");

            MovePath standardPath = getMovePathFor(createStandardInfantry(), 0, null,
                  MoveStepType.FORWARDS);

            assertFalse(standardPath.isMoveLegal(),
                  "Standard infantry should NOT be able to climb 3 levels (max elevation change is 1)");
        }
    }
}
