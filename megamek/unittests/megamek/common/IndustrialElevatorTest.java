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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.IndustrialElevator.ElevatorCall;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link IndustrialElevator}.
 */
class IndustrialElevatorTest {

    private static final int BOARD_ID = 0;
    private Coords testCoords;
    private BoardLocation testLocation;

    @BeforeEach
    void setUp() {
        testCoords = new Coords(5, 5);
        testLocation = BoardLocation.of(testCoords, BOARD_ID);
    }

    // --- Constructor Tests ---

    @Test
    void testConstructor() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);

        assertEquals(testLocation, elevator.getLocation());
        assertEquals(testCoords, elevator.getCoords());
        assertEquals(BOARD_ID, elevator.getBoardId());
        assertEquals(0, elevator.getShaftBottom());
        assertEquals(5, elevator.getShaftTop());
        assertEquals(100, elevator.getCapacityTons());
        assertEquals(5, elevator.getPlatformLevel()); // Starts at top
        assertTrue(elevator.isFunctional());
        assertTrue(elevator.getCallQueue().isEmpty());
    }

    @Test
    void testConstructorWithNegativeShaftBottom() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, -2, 4, 200);

        assertEquals(-2, elevator.getShaftBottom());
        assertEquals(4, elevator.getShaftTop());
        assertEquals(4, elevator.getPlatformLevel()); // Starts at top (4)
    }

    // --- fromTerrain Tests ---

    @Test
    void testFromTerrain() {
        // Encoding: exits = (shaftTop << 8) | capacityTens
        // shaftTop = 6, capacityTens = 34 (340 tons)
        int exits = (6 << 8) | 34;
        int level = -2; // shaft bottom

        IndustrialElevator elevator = IndustrialElevator.fromTerrain(testLocation, level, exits);

        assertEquals(-2, elevator.getShaftBottom());
        assertEquals(6, elevator.getShaftTop());
        assertEquals(340, elevator.getCapacityTons());
    }

    @Test
    void testFromTerrainMaxCapacity() {
        // Max capacity: 255 * 10 = 2550 tons
        int exits = (10 << 8) | 255;
        int level = 0;

        IndustrialElevator elevator = IndustrialElevator.fromTerrain(testLocation, level, exits);

        assertEquals(2550, elevator.getCapacityTons());
    }

    @Test
    void testEncodeExits() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 8, 150);

        int exits = elevator.encodeExits();
        int expectedExits = (8 << 8) | 15; // 150 / 10 = 15

        assertEquals(expectedExits, exits);
    }

    @Test
    void testFromTerrainAndEncodeRoundTrip() {
        int originalExits = (5 << 8) | 25;
        int originalLevel = 1;

        IndustrialElevator elevator = IndustrialElevator.fromTerrain(testLocation, originalLevel, originalExits);
        int encodedExits = elevator.encodeExits();

        assertEquals(originalExits, encodedExits);
    }

    // --- Platform Level Tests ---

    @Test
    void testSetPlatformLevel() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);

        elevator.setPlatformLevel(3);
        assertEquals(3, elevator.getPlatformLevel());

        elevator.setPlatformLevel(0);
        assertEquals(0, elevator.getPlatformLevel());

        elevator.setPlatformLevel(5);
        assertEquals(5, elevator.getPlatformLevel());
    }

    @Test
    void testSetPlatformLevelBelowShaftThrows() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);

        assertThrows(IllegalArgumentException.class, () -> elevator.setPlatformLevel(-1));
    }

    @Test
    void testSetPlatformLevelAboveShaftThrows() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);

        assertThrows(IllegalArgumentException.class, () -> elevator.setPlatformLevel(6));
    }

    @Test
    void testIsPlatformAt() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);

        assertTrue(elevator.isPlatformAt(5)); // Default at top
        assertFalse(elevator.isPlatformAt(0));
        assertFalse(elevator.isPlatformAt(1));

        elevator.setPlatformLevel(3);
        assertFalse(elevator.isPlatformAt(5));
        assertTrue(elevator.isPlatformAt(3));
    }

    // --- Shaft Range Tests ---

    @Test
    void testIsWithinShaft() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, -1, 4, 100);

        assertTrue(elevator.isWithinShaft(-1));
        assertTrue(elevator.isWithinShaft(0));
        assertTrue(elevator.isWithinShaft(2));
        assertTrue(elevator.isWithinShaft(4));

        assertFalse(elevator.isWithinShaft(-2));
        assertFalse(elevator.isWithinShaft(5));
    }

    @Test
    void testGetShaftHeight() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        assertEquals(6, elevator.getShaftHeight()); // 0,1,2,3,4,5 = 6 levels

        elevator = new IndustrialElevator(testLocation, -2, 3, 100);
        assertEquals(6, elevator.getShaftHeight()); // -2,-1,0,1,2,3 = 6 levels

        elevator = new IndustrialElevator(testLocation, 0, 0, 100);
        assertEquals(1, elevator.getShaftHeight()); // Single level shaft
    }

    // --- Functional State Tests ---

    @Test
    void testSetFunctional() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);

        assertTrue(elevator.isFunctional());

        elevator.setFunctional(false);
        assertFalse(elevator.isFunctional());

        elevator.setFunctional(true);
        assertTrue(elevator.isFunctional());
    }

    // --- Call Queue Tests ---

    @Test
    void testAddCall() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        Coords callerPos = new Coords(4, 5);

        ElevatorCall call = new ElevatorCall(1, callerPos, 3, 1, 5, 0);
        elevator.addCall(call);

        assertEquals(1, elevator.getCallQueue().size());
        assertEquals(call, elevator.getNextCall());
    }

    @Test
    void testRemoveCall() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        Coords callerPos = new Coords(4, 5);

        ElevatorCall call = new ElevatorCall(1, callerPos, 3, 1, 5, 0);
        elevator.addCall(call);
        assertTrue(elevator.removeCall(call));
        assertTrue(elevator.getCallQueue().isEmpty());
    }

    @Test
    void testClearCallQueue() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);

        elevator.addCall(new ElevatorCall(1, new Coords(4, 5), 1, 1, 5, 0));
        elevator.addCall(new ElevatorCall(2, new Coords(6, 5), 2, 1, 3, 0));

        assertEquals(2, elevator.getCallQueue().size());

        elevator.clearCallQueue();
        assertTrue(elevator.getCallQueue().isEmpty());
    }

    @Test
    void testGetNextCallEmpty() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        assertNull(elevator.getNextCall());
    }

    @Test
    void testCallQueueSortedByDistance() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        // Platform at 0

        // Call to level 4 (distance 4)
        ElevatorCall farCall = new ElevatorCall(1, new Coords(4, 5), 4, 1, 5, 0);
        // Call to level 2 (distance 2)
        ElevatorCall nearCall = new ElevatorCall(2, new Coords(6, 5), 2, 1, 3, 0);

        elevator.addCall(farCall);
        elevator.addCall(nearCall);

        // Near call should be first
        assertEquals(nearCall, elevator.getNextCall());
    }

    @Test
    void testCallQueueTieBreakByTurn() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        elevator.setPlatformLevel(2);

        // Both at distance 2, but different turns
        ElevatorCall laterCall = new ElevatorCall(1, new Coords(4, 5), 4, 3, 5, 2);
        ElevatorCall earlierCall = new ElevatorCall(2, new Coords(6, 5), 0, 1, 3, 2);

        elevator.addCall(laterCall);
        elevator.addCall(earlierCall);

        // Earlier turn should be first
        assertEquals(earlierCall, elevator.getNextCall());
    }

    @Test
    void testCallQueueTieBreakByInitiative() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        elevator.setPlatformLevel(2);

        // Same distance, same turn, different initiative
        ElevatorCall lowInitCall = new ElevatorCall(1, new Coords(4, 5), 4, 1, 3, 2);
        ElevatorCall highInitCall = new ElevatorCall(2, new Coords(6, 5), 0, 1, 8, 2);

        elevator.addCall(lowInitCall);
        elevator.addCall(highInitCall);

        // Higher initiative should be first
        assertEquals(highInitCall, elevator.getNextCall());
    }

    // --- Platform Movement Tests ---

    @Test
    void testMovePlatformTowardUp() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        elevator.setPlatformLevel(0); // Start at bottom for this test

        int newLevel = elevator.movePlatformToward(3);
        assertEquals(1, newLevel);
        assertEquals(1, elevator.getPlatformLevel());
    }

    @Test
    void testMovePlatformTowardDown() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        elevator.setPlatformLevel(4);

        int newLevel = elevator.movePlatformToward(1);
        assertEquals(3, newLevel);
        assertEquals(3, elevator.getPlatformLevel());
    }

    @Test
    void testMovePlatformTowardSameLevel() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        elevator.setPlatformLevel(3);

        int newLevel = elevator.movePlatformToward(3);
        assertEquals(3, newLevel); // No change
    }

    @Test
    void testMovePlatformTowardRespectsShaftTop() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        elevator.setPlatformLevel(5);

        int newLevel = elevator.movePlatformToward(10); // Above shaft
        assertEquals(5, newLevel); // Capped at shaft top
    }

    @Test
    void testMovePlatformTowardRespectsShaftBottom() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        elevator.setPlatformLevel(0);

        int newLevel = elevator.movePlatformToward(-5); // Below shaft
        assertEquals(0, newLevel); // Capped at shaft bottom
    }

    @Test
    void testMovePlatformWhenDisabled() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        elevator.setFunctional(false);

        int newLevel = elevator.movePlatformToward(3);
        assertEquals(5, newLevel); // No movement when disabled - stays at top
    }

    // --- Recalculate Distances Tests ---

    @Test
    void testRecalculateCallDistances() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        // Platform at 0

        // Call to level 4 (initially distance 4)
        ElevatorCall call4 = new ElevatorCall(1, new Coords(4, 5), 4, 1, 5, 0);
        // Call to level 2 (initially distance 2)
        ElevatorCall call2 = new ElevatorCall(2, new Coords(6, 5), 2, 1, 3, 0);

        elevator.addCall(call4);
        elevator.addCall(call2);

        // call2 should be first (distance 2)
        assertEquals(call2, elevator.getNextCall());

        // Move platform to level 3
        elevator.setPlatformLevel(3);
        elevator.recalculateCallDistances();

        // Now call4 is distance 1, call2 is distance 1 - tie break by turn (same)
        // Then by initiative: call4 has 5, call2 has 3 - call4 wins
        assertEquals(call4, elevator.getNextCall());
    }

    // --- toString Tests ---

    @Test
    void testToString() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        String str = elevator.toString();

        assertNotNull(str);
        assertTrue(str.contains("IndustrialElevator"));
        assertTrue(str.contains("shaft=0-5"));
        assertTrue(str.contains("platform=5")); // Starts at top
        assertTrue(str.contains("capacity=100t"));
        assertTrue(str.contains("functional"));
    }

    @Test
    void testToStringDisabled() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        elevator.setFunctional(false);
        String str = elevator.toString();

        assertTrue(str.contains("disabled"));
    }

    // --- ElevatorCall Tests ---

    @Test
    void testElevatorCallConstructor() {
        Coords callerPos = new Coords(4, 5);
        ElevatorCall call = new ElevatorCall(1, callerPos, 3, 2, 5, 0);

        assertEquals(1, call.getPlayerId());
        assertEquals(callerPos, call.getCallerPosition());
        assertEquals(3, call.getTargetLevel());
        assertEquals(2, call.getTurnCalled());
        assertEquals(5, call.getInitiativeBonus());
        assertEquals(3, call.getDistanceFromPlatform()); // |3 - 0| = 3
    }

    @Test
    void testElevatorCallUpdateDistance() {
        ElevatorCall call = new ElevatorCall(1, new Coords(4, 5), 5, 1, 5, 0);

        assertEquals(5, call.getDistanceFromPlatform()); // |5 - 0| = 5

        call.updateDistance(3);
        assertEquals(2, call.getDistanceFromPlatform()); // |5 - 3| = 2

        call.updateDistance(5);
        assertEquals(0, call.getDistanceFromPlatform()); // |5 - 5| = 0
    }

    @Test
    void testElevatorCallCompareTo() {
        // Test distance comparison
        ElevatorCall near = new ElevatorCall(1, new Coords(4, 5), 1, 1, 5, 0);
        ElevatorCall far = new ElevatorCall(2, new Coords(6, 5), 3, 1, 5, 0);

        assertTrue(near.compareTo(far) < 0); // Near comes first

        // Test turn comparison (same distance)
        ElevatorCall earlier = new ElevatorCall(1, new Coords(4, 5), 2, 1, 5, 0);
        ElevatorCall later = new ElevatorCall(2, new Coords(6, 5), 2, 3, 5, 0);

        assertTrue(earlier.compareTo(later) < 0); // Earlier comes first

        // Test initiative comparison (same distance, same turn)
        ElevatorCall lowInit = new ElevatorCall(1, new Coords(4, 5), 2, 1, 3, 0);
        ElevatorCall highInit = new ElevatorCall(2, new Coords(6, 5), 2, 1, 8, 0);

        assertTrue(highInit.compareTo(lowInit) < 0); // Higher initiative comes first
    }

    @Test
    void testElevatorCallToString() {
        ElevatorCall call = new ElevatorCall(1, new Coords(4, 5), 3, 2, 5, 0);
        String str = call.toString();

        assertNotNull(str);
        assertTrue(str.contains("ElevatorCall"));
        assertTrue(str.contains("player=1"));
        assertTrue(str.contains("level=3"));
        assertTrue(str.contains("turn=2"));
    }

    // --- Call Queue Immutability Test ---

    @Test
    void testGetCallQueueReturnsUnmodifiableList() {
        IndustrialElevator elevator = new IndustrialElevator(testLocation, 0, 5, 100);
        elevator.addCall(new ElevatorCall(1, new Coords(4, 5), 3, 1, 5, 0));

        List<ElevatorCall> queue = elevator.getCallQueue();

        assertThrows(UnsupportedOperationException.class, () -> queue.clear());
    }
}
