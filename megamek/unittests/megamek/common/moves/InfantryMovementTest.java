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
package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.Coords;
import megamek.common.GameBoardTestCase;
import megamek.common.Infantry;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test class for MovePath executing JUMP the Jump movement type.
 * @author Luana Coppio
 */
public class InfantryMovementTest extends GameBoardTestCase {

    @Nested
    class ClimbIntoBuilding {

        static {
            initializeBoard("BOARD_1x2_START_LOW", """
                  size 1 2
                  hex 0101 0 "" ""
                  hex 0102 1 "bldg_elev:1;building:2:8;bldg_cf:50" ""
                  end""");

            initializeBoard("BOARD_1x2_SAME_LEVEL", """
                  size 1 2
                  hex 0101 0 "" ""
                  hex 0102 0 "bldg_elev:1;building:2:8;bldg_cf:50" ""
                  end""");

            initializeBoard("BOARD_1x2_HIGH_LEVEL", """
                  size 1 2
                  hex 0101 1 "" ""
                  hex 0102 0 "bldg_elev:1;building:2:8;bldg_cf:50" ""
                  end""");


            initializeBoard("BOARD_1x2_HIGH_LEVEL_INTO_BUILDING", """
                  size 1 2
                  hex 0101 1 "" ""
                  hex 0102 0 "bldg_elev:2;building:2:8;bldg_cf:50" ""
                  end""");
        }

        private static Infantry getInfantry() {
            Infantry infantry = new Infantry();
            infantry.setId(2);
            infantry.setWeight(2.0f);
            infantry.initializeInternal(10, Infantry.LOC_INFANTRY);
            return infantry;
        }

        @Test
        void searchForMultiplePathsWithFuzzer() {
            setBoard("BOARD_1x2_START_LOW");
            List<MovePath> movePaths = getMovePathFuzzerFor(getInfantry(), 2,
                  (mp) -> mp.isMoveLegal() && mp.getFinalCoords().equals(new Coords(0, 1))
            );
            System.out.println(movePaths);
            assertFalse(movePaths.isEmpty());
        }


        @Test
        void canClimbIntoBuildingFromLowerLevel() {
            setBoard("BOARD_1x2_START_LOW");
            List<MovePath> movePaths = getMovePathFuzzerFor(getInfantry(), 2, (mp) ->
                     mp.isMoveLegal() && mp.getFinalCoords().equals(new Coords(0, 1))
            );

            System.out.println(movePaths);

            MovePath movePath = getMovePathFor(getInfantry(),
                  MovePath.MoveStepType.CLIMB_MODE_ON,
                  MovePath.MoveStepType.FORWARDS
            );

            assertTrue(movePath.isMoveLegal(), "Infantry can climb into a building hex paying just 1 like a mek would");

            assertMovePathElevations(movePath,
                  ExpectedElevation.of(0, "Starts on ground level 0"),
                  ExpectedElevation.of(1, "Moving climbing to building hex, level 1"));
        }

        @Test
        void canClimbIntoBuildingHexFromLowerLevel() {
            setBoard("BOARD_1x2_START_LOW");
            MovePath movePath = getMovePathFor(getInfantry(),
                  MovePath.MoveStepType.FORWARDS
            );

            assertTrue(movePath.isMoveLegal(), "Infantry can climb into a building roof if it has the right " +
                                                      "elevation difference, but it cannot enter through any floor " +
                                                      "other than the one on the same level it is on, unless it is " +
                                                      "jump-capable");

            assertMovePathElevations(movePath,
                  ExpectedElevation.of(1, "Moving climbing to building hex, level 0"));
        }

        @Test
        void canClimbIntoBuildingsRoof() {
            setBoard("BOARD_1x2_SAME_LEVEL");
            MovePath movePath = getMovePathFor(getInfantry(),
                  MovePath.MoveStepType.CLIMB_MODE_ON,
                  MovePath.MoveStepType.FORWARDS);

            assertTrue(movePath.isMoveLegal(), "Infantry can climb into a building hex paying just 1 like a mek would");

            assertMovePathElevations(movePath,
                  ExpectedElevation.of(0, "Starts on ground level 0"),
                  ExpectedElevation.of(1, "Moving climbing to building hex, level 1"));
        }

        @Test
        void canMoveIntoBuilding() {
            setBoard("BOARD_1x2_SAME_LEVEL");
            MovePath movePath = getMovePathFor(getInfantry(),
                  MovePath.MoveStepType.CLIMB_MODE_OFF,
                  MovePath.MoveStepType.FORWARDS);

            assertTrue(movePath.isMoveLegal(), "Infantry can walk into a building like its clear hex");
            assertMovePathElevations(movePath,
                  ExpectedElevation.of(0, "Setting ClimbMode Off"),
                  ExpectedElevation.of(0, "Moving to building hex, level 0"));
        }


        @Test
        void canMoveIntoBuildingRoof() {
            setBoard("BOARD_1x2_HIGH_LEVEL");
            MovePath movePath = getMovePathFor(getInfantry(),
                  MovePath.MoveStepType.CLIMB_MODE_ON,
                  MovePath.MoveStepType.FORWARDS);

            assertTrue(movePath.isMoveLegal(), "Infantry can walk into a building roof from a high place like its " +
                                                     "clear hex just like a mech would");
            assertMovePathElevations(movePath,
                  ExpectedElevation.of(0, "Moving to building hex rooftop, level 0 elevation 1"),
                  ExpectedElevation.of(1, "Moving to building hex rooftop, level 0 elevation 1"));
        }

        @Test
        void canMoveIntoBuildingFloor() {
            setBoard("BOARD_1x2_HIGH_LEVEL_INTO_BUILDING");
            MovePath movePath = getMovePathFor(getInfantry(),
                  MovePath.MoveStepType.CLIMB_MODE_OFF,
                  MovePath.MoveStepType.FORWARDS);

            assertTrue(movePath.isMoveLegal(), "Infantry can walk into a building floor from a high place like its " +
                                                     "clear hex");
            assertMovePathElevations(movePath,
                  ExpectedElevation.of(0, "ClimbMode Off on top of a hill elevation 1"),
                  ExpectedElevation.of(1, "Moving to building hex floor, level 0 elevation 1"));
        }
    }
}
