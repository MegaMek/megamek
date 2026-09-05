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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for detaching trailers from a train.
 *
 * <p>Detaching a mid-train trailer used to drop the train membership on whichever entity the call was made
 * against instead of on the tractor heading the train. The tractor kept listing trailers it no longer towed, and
 * re-hitching one of those trailers recorded it as a unit trailing itself. Disconnecting it after that walked a
 * list it was clearing as it went, which threw ConcurrentModificationException in the lobby and on the server's
 * train rollback path.</p>
 */
class TrainDisconnectTest {

    private static final double TRACTOR_TONS = 75.0;
    private static final double CARRIAGE_TONS = 10.0;

    private Game game;
    private Player owner;
    private int nextId = 1;

    @BeforeEach
    void setUp() {
        EquipmentType.initializeTypes();
        owner = new Player(0, "Owner");
        owner.setTeam(1);
        game = new Game();
        game.addPlayer(0, owner);
    }

    private Tank buildVehicle(double tonnage, boolean isTrailer) throws Exception {
        Tank vehicle = new Tank();
        vehicle.setId(nextId++);
        vehicle.setOwner(owner);
        vehicle.setWeight(tonnage);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        vehicle.setTrailer(isTrailer);
        vehicle.addEquipment(EquipmentType.get(EquipmentTypeLookup.HITCH), Tank.LOC_BODY);
        vehicle.setTrailerHitches();
        game.addEntity(vehicle);
        return vehicle;
    }

    /** A tractor with the requested number of trailers hitched behind it, front to back. */
    private Tank buildTrain(int trailerCount) throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, false);
        for (int index = 0; index < trailerCount; index++) {
            Tank carriage = buildVehicle(CARRIAGE_TONS, true);
            tractor.towUnit(carriage.getId());
        }
        return tractor;
    }

    private Entity trailerAt(Tank tractor, int trailerNumber) {
        return game.getEntity(tractor.getAllTowedUnits().get(trailerNumber));
    }

    @Test
    void detachingTheLeadTrailerEmptiesTheWholeTrain() throws Exception {
        Tank tractor = buildTrain(2);
        Entity leadTrailer = trailerAt(tractor, 0);
        Entity secondTrailer = trailerAt(tractor, 1);

        tractor.disconnectUnit(leadTrailer.getId());

        assertTrue(tractor.getAllTowedUnits().isEmpty(), "The tractor tows nothing after dropping the lead trailer");
        assertEquals(Entity.NONE, tractor.getTowing());
        assertEquals(Entity.NONE, leadTrailer.getTractor());
        assertEquals(Entity.NONE, secondTrailer.getTractor(), "Trailers behind the drop come off with it");
        assertEquals(Entity.NONE, secondTrailer.getTowedBy());
    }

    @Test
    void detachingAMidTrainTrailerClearsItFromTheTractor() throws Exception {
        Tank tractor = buildTrain(3);
        Entity leadTrailer = trailerAt(tractor, 0);
        Entity middleTrailer = trailerAt(tractor, 1);
        Entity lastTrailer = trailerAt(tractor, 2);

        // The lobby detaches through the unit in front of the trailer, which mid-train is another trailer.
        leadTrailer.disconnectUnit(middleTrailer.getId());

        assertEquals(List.of(leadTrailer.getId()), tractor.getAllTowedUnits(),
              "Only the lead trailer is still part of the train");
        assertEquals(Entity.NONE, middleTrailer.getTractor());
        assertEquals(Entity.NONE, lastTrailer.getTractor(), "The tail comes off with the trailer in front of it");
        assertEquals(Entity.NONE, leadTrailer.getTowing(), "The trailer left in place no longer tows anything");
        assertFalse(leadTrailer.getConnectedUnits().contains(middleTrailer.getId()),
              "The remaining trailer forgets the units that left");
        assertFalse(leadTrailer.getConnectedUnits().contains(lastTrailer.getId()),
              "The remaining trailer forgets the units that left");
    }

    @Test
    void aDetachedTrailerCanBeHitchedAgainWithoutSelfReference() throws Exception {
        Tank tractor = buildTrain(2);
        Entity leadTrailer = trailerAt(tractor, 0);
        Entity secondTrailer = trailerAt(tractor, 1);

        tractor.disconnectUnit(leadTrailer.getId());
        tractor.towUnit(leadTrailer.getId());
        tractor.towUnit(secondTrailer.getId());

        assertEquals(List.of(leadTrailer.getId(), secondTrailer.getId()), tractor.getAllTowedUnits(),
              "Rebuilding the train lists each trailer exactly once");
        assertFalse(leadTrailer.getConnectedUnits().contains(leadTrailer.getId()),
              "A trailer must never be recorded as trailing itself");
        assertFalse(secondTrailer.getConnectedUnits().contains(secondTrailer.getId()),
              "A trailer must never be recorded as trailing itself");
    }

    @Test
    void detachingARebuiltTrainDoesNotThrow() throws Exception {
        // The playtest sequence: build the train, detach through the middle, put the same train back together,
        // then detach again. The stale membership left by the first detach used to make the second trailer trail
        // itself, and dropping it walked the list it was clearing.
        Tank tractor = buildTrain(2);
        Entity leadTrailer = trailerAt(tractor, 0);
        Entity secondTrailer = trailerAt(tractor, 1);

        leadTrailer.disconnectUnit(secondTrailer.getId());
        tractor.disconnectUnit(leadTrailer.getId());
        tractor.towUnit(leadTrailer.getId());
        tractor.towUnit(secondTrailer.getId());

        assertDoesNotThrow(() -> tractor.disconnectUnit(secondTrailer.getId()),
              "Detaching a rebuilt train must not throw ConcurrentModificationException");
        assertDoesNotThrow(() -> tractor.disconnectUnit(leadTrailer.getId()),
              "Detaching a rebuilt train must not throw ConcurrentModificationException");
        assertTrue(tractor.getAllTowedUnits().isEmpty(), "The train is empty once both trailers are detached");
    }

    @Test
    void aTrailerAlreadyInTheTrainIsNotHitchedTwice() throws Exception {
        Tank tractor = buildTrain(1);
        Entity trailer = trailerAt(tractor, 0);

        tractor.towUnit(trailer.getId());

        assertEquals(List.of(trailer.getId()), tractor.getAllTowedUnits(),
              "A repeated tow request leaves the train as it was");
        assertTrue(trailer.getConnectedUnits().isEmpty(), "No self-reference is recorded");
    }

    @Test
    void aTractorCannotTowItself() throws Exception {
        Tank tractor = buildTrain(0);

        tractor.towUnit(tractor.getId());

        assertTrue(tractor.getAllTowedUnits().isEmpty(), "A tractor never becomes its own trailer");
    }
}
