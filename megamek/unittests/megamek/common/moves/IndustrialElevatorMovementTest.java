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

import megamek.common.GameBoardTestCase;
import megamek.common.IndustrialElevator;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.units.BipedMek;
import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for industrial elevator movement validation.
 * <p>
 * These tests verify the MoveStep.isMovementPossible() validation logic for elevator steps. They focus on cases where
 * movement should be ILLEGAL due to various elevator constraints.
 */
public class IndustrialElevatorMovementTest extends GameBoardTestCase {

    // Industrial elevator terrain encoding:
    // level = shaft bottom
    // exits = (shaftTop << 8) | capacityTens
    // Example: shaft from 0 to 4, capacity 500 tons = (4 << 8) | 50 = 1074

    @Nested
    class ElevatorValidationTests {

        static {
            // Board with industrial elevator shaft from level 0 to 4, capacity 500 tons
            initializeBoard("BOARD_ELEVATOR", """
                  size 1 2
                  hex 0101 0 "industrial_elevator:0:1074" ""
                  hex 0102 0 "" ""
                  end""");
        }

        @Test
        void cannotAscendElevatorNotOnPlatform() {
            setBoard("BOARD_ELEVATOR");
            Coords elevatorCoords = new Coords(0, 0);
            BoardLocation elevatorLocation = BoardLocation.of(elevatorCoords, 0);
            IndustrialElevator elevator = new IndustrialElevator(elevatorLocation, 0, 4, 500);
            elevator.setPlatformLevel(2); // Platform at level 2
            getGame().addIndustrialElevator(elevator);

            // Mek at level 0, but platform is at level 2
            MovePath movePath = getMovePathFor(new BipedMek(), 0, EntityMovementMode.BIPED,
                  MoveStepType.ELEVATOR_ASCEND);

            assertFalse(movePath.isMoveLegal(), "Should not be able to ascend when not on platform");
        }

        @Test
        void cannotAscendAboveShaftTop() {
            setBoard("BOARD_ELEVATOR");
            Coords elevatorCoords = new Coords(0, 0);
            BoardLocation elevatorLocation = BoardLocation.of(elevatorCoords, 0);
            IndustrialElevator elevator = new IndustrialElevator(elevatorLocation, 0, 4, 500);
            elevator.setPlatformLevel(4); // Platform at top of shaft
            getGame().addIndustrialElevator(elevator);

            // Mek on platform at level 4 (shaft top)
            MovePath movePath = getMovePathFor(new BipedMek(), 4, EntityMovementMode.BIPED,
                  MoveStepType.ELEVATOR_ASCEND);

            assertFalse(movePath.isMoveLegal(), "Should not be able to ascend above shaft top");
        }

        @Test
        void cannotAscendDisabledElevator() {
            setBoard("BOARD_ELEVATOR");
            Coords elevatorCoords = new Coords(0, 0);
            BoardLocation elevatorLocation = BoardLocation.of(elevatorCoords, 0);
            IndustrialElevator elevator = new IndustrialElevator(elevatorLocation, 0, 4, 500);
            elevator.setPlatformLevel(0);
            elevator.setFunctional(false); // Disabled
            getGame().addIndustrialElevator(elevator);

            MovePath movePath = getMovePathFor(new BipedMek(), 0, EntityMovementMode.BIPED,
                  MoveStepType.ELEVATOR_ASCEND);

            assertFalse(movePath.isMoveLegal(), "Should not be able to use disabled elevator");
        }

        @Test
        void cannotDescendBelowShaftBottom() {
            setBoard("BOARD_ELEVATOR");
            Coords elevatorCoords = new Coords(0, 0);
            BoardLocation elevatorLocation = BoardLocation.of(elevatorCoords, 0);
            IndustrialElevator elevator = new IndustrialElevator(elevatorLocation, 0, 4, 500);
            elevator.setPlatformLevel(0); // Platform at bottom
            getGame().addIndustrialElevator(elevator);

            // Mek on platform at level 0 (shaft bottom)
            MovePath movePath = getMovePathFor(new BipedMek(), 0, EntityMovementMode.BIPED,
                  MoveStepType.ELEVATOR_DESCEND);

            assertFalse(movePath.isMoveLegal(), "Should not be able to descend below shaft bottom");
        }
    }

    @Nested
    class ElevatorWithNegativeShaftBottom {

        static {
            // Elevator with shaft from -2 to 3
            // Encoding: level = -2, exits = (3 << 8) | 30 = 798
            initializeBoard("BOARD_ELEVATOR_NEG", """
                  size 1 2
                  hex 0101 -2 "industrial_elevator:-2:798" ""
                  hex 0102 0 "" ""
                  end""");
        }

        @Test
        void cannotDescendBelowNegativeShaftBottom() {
            setBoard("BOARD_ELEVATOR_NEG");
            Coords elevatorCoords = new Coords(0, 0);
            BoardLocation elevatorLocation = BoardLocation.of(elevatorCoords, 0);
            IndustrialElevator elevator = new IndustrialElevator(elevatorLocation, -2, 3, 300);
            elevator.setPlatformLevel(-2); // Platform at bottom
            getGame().addIndustrialElevator(elevator);

            MovePath movePath = getMovePathFor(new BipedMek(), -2, EntityMovementMode.BIPED,
                  MoveStepType.ELEVATOR_DESCEND);

            assertFalse(movePath.isMoveLegal(), "Should not be able to descend below shaft bottom");
        }
    }

    @Nested
    class ElevatorWithoutRegistration {

        static {
            initializeBoard("BOARD_ELEVATOR_NOREG", """
                  size 1 2
                  hex 0101 0 "industrial_elevator:0:1074" ""
                  hex 0102 0 "" ""
                  end""");
        }

        @Test
        void cannotUseElevatorNotRegisteredInGame() {
            setBoard("BOARD_ELEVATOR_NOREG");
            // Do NOT register elevator in game

            MovePath movePath = getMovePathFor(new BipedMek(), 0, EntityMovementMode.BIPED,
                  MoveStepType.ELEVATOR_ASCEND);

            assertFalse(movePath.isMoveLegal(), "Should not be able to use unregistered elevator");
        }
    }

    @Nested
    class ElevatorOnNonElevatorHex {

        static {
            initializeBoard("BOARD_NO_ELEVATOR", """
                  size 1 2
                  hex 0101 0 "" ""
                  hex 0102 0 "" ""
                  end""");
        }

        @Test
        void cannotUseElevatorOnNormalHex() {
            setBoard("BOARD_NO_ELEVATOR");

            MovePath movePath = getMovePathFor(new BipedMek(), 0, EntityMovementMode.BIPED,
                  MoveStepType.ELEVATOR_ASCEND);

            assertFalse(movePath.isMoveLegal(), "Should not be able to use elevator on normal hex");
        }
    }
}
