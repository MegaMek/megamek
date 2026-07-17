/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
 * These tests verify the MoveStep.isMovementPossible() validation logic for elevator steps: the cases where movement
 * must be ILLEGAL due to elevator constraints, and legal rides - including on hexes whose surface is above board
 * level 0, which pins down that all shaft levels are relative to the hex surface.
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

        @Test
        void cannotRideOverCapacityElevator() {
            setBoard("BOARD_ELEVATOR");
            Coords elevatorCoords = new Coords(0, 0);
            BoardLocation elevatorLocation = BoardLocation.of(elevatorCoords, 0);
            // Capacity 40 tons; the default test Mek weighs 50 tons, so the loaded elevator is over capacity
            IndustrialElevator elevator = new IndustrialElevator(elevatorLocation, 0, 4, 40);
            elevator.setPlatformLevel(2);
            getGame().addIndustrialElevator(elevator);

            MovePath movePath = getMovePathFor(new BipedMek(), 2, EntityMovementMode.BIPED,
                  MoveStepType.ELEVATOR_DESCEND);

            assertFalse(movePath.isMoveLegal(), "Should not be able to ride an over-capacity elevator");
        }

        @Test
        void cannotTravelMoreThanOneLevelPerTurn() {
            setBoard("BOARD_ELEVATOR");
            Coords elevatorCoords = new Coords(0, 0);
            BoardLocation elevatorLocation = BoardLocation.of(elevatorCoords, 0);
            IndustrialElevator elevator = new IndustrialElevator(elevatorLocation, 0, 4, 500);
            elevator.setPlatformLevel(2);
            getGame().addIndustrialElevator(elevator);

            // The platform stays at level 2 during planning, so a second descend step (now from level 1)
            // has no platform at the unit's level and is illegal: 1 level per turn (TO:AR).
            MovePath movePath = getMovePathFor(new BipedMek(), 2, EntityMovementMode.BIPED,
                  MoveStepType.ELEVATOR_DESCEND, MoveStepType.ELEVATOR_DESCEND);

            assertFalse(movePath.isMoveLegal(), "Should only be able to move one level per turn");
        }
    }

    @Nested
    class LegalRidesOnRaisedHex {

        static {
            // The QA scenario: elevator in a hex whose SURFACE is at board level 2. All shaft levels are
            // relative to the hex surface: top 0 = the surface, bottom -2 = two levels down.
            // Encoding: level = -2, exits = (0 << 8) | 10 = 10 (100 tons)
            initializeBoard("BOARD_ELEVATOR_RAISED", """
                  size 1 2
                  hex 0101 2 "industrial_elevator:-2:10" ""
                  hex 0102 2 "" ""
                  end""");
        }

        @Test
        void canDescendFromSurfaceOfRaisedHex() {
            setBoard("BOARD_ELEVATOR_RAISED");
            BoardLocation elevatorLocation = BoardLocation.of(new Coords(0, 0), 0);
            // Initialize from terrain exactly like IndustrialElevatorProcessor does; platform starts at top (0)
            IndustrialElevator elevator = IndustrialElevator.fromTerrain(elevatorLocation, -2, 10);
            getGame().addIndustrialElevator(elevator);

            // Mek standing on the hex surface (elevation 0) with the platform at the top: ride down
            MovePath movePath = getMovePathFor(new BipedMek(), 0, EntityMovementMode.BIPED,
                  MoveStepType.ELEVATOR_DESCEND);

            assertTrue(movePath.isMoveLegal(),
                  "A unit on the surface of a raised elevator hex should be able to ride the platform down");
        }

        @Test
        void canAscendFromShaftBottomOfRaisedHex() {
            setBoard("BOARD_ELEVATOR_RAISED");
            BoardLocation elevatorLocation = BoardLocation.of(new Coords(0, 0), 0);
            IndustrialElevator elevator = IndustrialElevator.fromTerrain(elevatorLocation, -2, 10);
            elevator.setPlatformLevel(-2);
            getGame().addIndustrialElevator(elevator);

            // Mek at the shaft bottom (2 levels below the surface) with the platform: ride up
            MovePath movePath = getMovePathFor(new BipedMek(), -2, EntityMovementMode.BIPED,
                  MoveStepType.ELEVATOR_ASCEND);

            assertTrue(movePath.isMoveLegal(),
                  "A unit at the shaft bottom with the platform should be able to ride it up");
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
