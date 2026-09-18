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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import megamek.common.board.Coords;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the crane loading and unloading state kept by a grounded Small Craft or DropShip (TW p.90-91).
 */
class CraneOperationTest {

    private static final Coords WAITING_HEX = new Coords(4, 7);
    private static final int NORTH_EAST = 1;

    @Test
    @DisplayName("Loading needs four turns after the declaring turn (TW p.90)")
    void loadingTakesFourTurns() {
        CraneOperation operation = CraneOperation.load(12, WAITING_HEX);

        assertTrue(operation.isLoading(), "A load operation loads");
        assertFalse(operation.isStarted(), "A new operation starts in the declaring End Phase, not before");
        assertEquals(4, operation.getTurnsRequired(), "Loading takes four turns");

        operation.start();
        for (int turn = 1; turn <= 3; turn++) {
            assertEquals(turn, operation.bankTurn(), "Each End Phase banks one turn");
            assertFalse(operation.isComplete(), "Not aboard before the fourth turn");
        }
        assertEquals(4, operation.bankTurn(), "The fourth turn is banked");
        assertTrue(operation.isComplete(), "Aboard at the end of the fourth turn");
        assertNull(operation.getUnloadPosition(), "A load operation has no unload hex");
    }

    @Test
    @DisplayName("Unloading needs three turns and keeps the chosen hex and facing (TW p.91)")
    void unloadingTakesThreeTurnsAndKeepsHexAndFacing() {
        CraneOperation operation = CraneOperation.unload(12, WAITING_HEX, NORTH_EAST);

        assertFalse(operation.isLoading(), "An unload operation unloads");
        assertEquals(3, operation.getTurnsRequired(), "Unloading takes three turns");
        assertEquals(WAITING_HEX, operation.getUnloadPosition(), "The chosen hex is kept");
        assertEquals(NORTH_EAST, operation.getUnloadFacing(), "The chosen facing is kept");

        operation.start();
        operation.bankTurn();
        operation.bankTurn();
        assertEquals(3, operation.bankTurn(), "The third turn is banked");
        assertTrue(operation.isComplete(), "Out at the end of the third turn");
        assertEquals(3, operation.bankTurn(), "Banking after completion does not go past three, so a blocked unload can wait");
    }

    @Test
    @DisplayName("A crane operation survives being sent over the network with its progress")
    void operationSurvivesSerialization() throws IOException, ClassNotFoundException {
        CraneOperation operation = CraneOperation.unload(33, WAITING_HEX, NORTH_EAST);
        operation.start();
        operation.bankTurn();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(bytes)) {
            objectOutput.writeObject(operation);
        }
        CraneOperation copy;
        try (ObjectInputStream objectInput = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (CraneOperation) objectInput.readObject();
        }

        assertEquals(33, copy.getUnitId(), "The unit id is kept");
        assertEquals(WAITING_HEX, copy.getUnitPosition(), "The hex is kept");
        assertEquals(NORTH_EAST, copy.getUnloadFacing(), "The facing is kept");
        assertTrue(copy.isStarted(), "The started flag is kept");
        assertEquals(1, copy.getTurnsCompleted(), "The progress is kept");
    }

    @Test
    @DisplayName("Adding a crane operation for a unit replaces the one it already had")
    void carrierKeepsOneOperationPerUnit() {
        CraneOperations operations = new SmallCraft().getCraneOperations();

        operations.add(CraneOperation.load(12, WAITING_HEX));
        operations.add(CraneOperation.unload(12, WAITING_HEX, NORTH_EAST));
        operations.add(CraneOperation.load(13, WAITING_HEX));

        assertEquals(2, operations.getOperations().size(), "One operation per unit");
        assertFalse(operations.findFor(12).isLoading(), "The unload replaced the earlier load for unit 12");
        operations.remove(12);
        assertEquals(1, operations.getOperations().size(), "Removing a unit's operation leaves the others");
        assertNull(operations.findFor(12), "Unit 12 has no operation left");
        assertEquals(13, operations.getOperations().getFirst().getUnitId(), "The other unit's operation remains");
    }
}
