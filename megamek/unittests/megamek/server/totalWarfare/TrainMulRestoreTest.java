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

package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers rebuilding a train from a loaded MUL.
 *
 * <p>A tractor read from a file carries the trailer ids the saving game used. The server assigns its own ids when
 * the units are added, so the saved ids have to be translated before the train can be hitched again.</p>
 */
class TrainMulRestoreTest {

    private static final double TRACTOR_TONS = 75.0;
    private static final double CARRIAGE_TONS = 10.0;

    private Game game;
    private TWGameManager gameManager;
    private Player owner;

    @BeforeEach
    void setUp() {
        EquipmentType.initializeTypes();
        game = new Game();
        owner = new Player(0, "Owner");
        owner.setTeam(1);
        game.addPlayer(0, owner);
        gameManager = new TWGameManager();
        gameManager.setGame(game);
    }

    /** Adds a vehicle with a server-given id, as receiveEntityAdd would. */
    private Tank addVehicle(double tonnage, boolean isTrailer) throws Exception {
        Tank vehicle = new Tank();
        vehicle.setId(game.getNextEntityId());
        vehicle.setOwner(owner);
        vehicle.setWeight(tonnage);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        vehicle.setTrailer(isTrailer);
        vehicle.addEquipment(EquipmentType.get(EquipmentTypeLookup.HITCH), Tank.LOC_BODY);
        vehicle.setTrailerHitches();
        game.addEntity(vehicle);
        return vehicle;
    }

    @Test
    void savedTrailerIdsAreTranslatedAndTheTrainIsHitched() throws Exception {
        Tank tractor = addVehicle(TRACTOR_TONS, false);
        Tank leadTrailer = addVehicle(CARRIAGE_TONS, true);
        Tank secondTrailer = addVehicle(CARRIAGE_TONS, true);

        // The file said this tractor towed units 51 then 52, which are not the ids they were given here.
        tractor.addTowedUnit(51);
        tractor.addTowedUnit(52);
        Map<Integer, Integer> idMap = new HashMap<>();
        idMap.put(51, leadTrailer.getId());
        idMap.put(52, secondTrailer.getId());

        gameManager.restoreTrains(List.of(tractor, leadTrailer, secondTrailer), idMap);

        assertEquals(List.of(leadTrailer.getId(), secondTrailer.getId()), tractor.getAllTowedUnits(),
              "The train is hitched with real ids, in the saved order");
        assertEquals(tractor.getId(), leadTrailer.getTractor());
        assertEquals(tractor.getId(), secondTrailer.getTractor());
        assertEquals(tractor.getId(), leadTrailer.getTowedBy(), "The lead trailer hangs off the tractor");
        assertEquals(leadTrailer.getId(), secondTrailer.getTowedBy(), "The second hangs off the first");
    }

    @Test
    void aRestoredTrailerSitsOnAHitch() throws Exception {
        Tank tractor = addVehicle(TRACTOR_TONS, false);
        Tank trailer = addVehicle(CARRIAGE_TONS, true);

        tractor.addTowedUnit(51);
        gameManager.restoreTrains(List.of(tractor, trailer), Map.of(51, trailer.getId()));

        assertEquals(trailer.getId(), tractor.getTowing(), "The tractor tows it");
        assertTrue(tractor.getHitchCarrying(trailer.getId()) != null,
              "The trailer is held by a hitch, not just listed in the train");
    }

    @Test
    void aTrailerMissingFromTheFileIsSkipped() throws Exception {
        Tank tractor = addVehicle(TRACTOR_TONS, false);
        Tank trailer = addVehicle(CARRIAGE_TONS, true);

        // 52 was towed when the file was saved but is not in it, so nothing maps to it.
        tractor.addTowedUnit(51);
        tractor.addTowedUnit(52);

        gameManager.restoreTrains(List.of(tractor, trailer), Map.of(51, trailer.getId()));

        assertEquals(List.of(trailer.getId()), tractor.getAllTowedUnits(),
              "The trailers that are present still hitch; the missing one is dropped");
    }

    @Test
    void savedIdsNeverSurviveAsRealIds() throws Exception {
        Tank tractor = addVehicle(TRACTOR_TONS, false);
        Tank trailer = addVehicle(CARRIAGE_TONS, true);

        // A saved id can collide with a real one. Nothing may be left listing the raw file id.
        int savedId = trailer.getId();
        tractor.addTowedUnit(savedId);

        gameManager.restoreTrains(List.of(tractor, trailer), new HashMap<>());

        assertTrue(tractor.getAllTowedUnits().isEmpty(),
              "An unmapped saved id is dropped even when it looks like a real entity id");
    }

    @Test
    void aTrailerThatCannotBeHitchedIsLeftLoose() throws Exception {
        // A tractor with no hitch at all, which is what a file describing a train the data no longer supports
        // amounts to. towUnit has nothing to load the trailer into.
        Tank tractor = new Tank();
        tractor.setId(game.getNextEntityId());
        tractor.setOwner(owner);
        tractor.setWeight(TRACTOR_TONS);
        tractor.setMovementMode(EntityMovementMode.TRACKED);
        game.addEntity(tractor);

        Tank trailer = addVehicle(CARRIAGE_TONS, true);

        tractor.addTowedUnit(51);
        gameManager.restoreTrains(List.of(tractor, trailer), Map.of(51, trailer.getId()));

        assertEquals(Entity.NONE, trailer.getTractor(),
              "A trailer nothing can hold is loaded loose rather than listed as towed");
        assertFalse(tractor.getAllTowedUnits().contains(trailer.getId()),
              "and the tractor does not report it as part of the train");
    }

    @Test
    void aTowThatThrowsDoesNotAbortTheLoad() throws Exception {
        // TankTrailerHitch.load throws when a hitch cannot take the trailer, and towUnit calls it without checking
        // first. Escaping here would abort receiveEntityAdd and lose every unit in the file. Occupy the tractor's
        // only hitch with a trailer that is not part of the restore, so the saved one has nothing to hang off.
        Tank tractor = addVehicle(TRACTOR_TONS, false);
        Tank blockingTrailer = addVehicle(CARRIAGE_TONS, true);
        tractor.towUnit(blockingTrailer.getId());

        Tank savedTrailer = addVehicle(CARRIAGE_TONS, true);
        tractor.addTowedUnit(51);

        assertDoesNotThrow(() -> gameManager.restoreTrains(List.of(tractor, savedTrailer),
                    Map.of(51, savedTrailer.getId())),
              "A train the data can no longer build must not take the rest of the file down with it");
        assertEquals(Entity.NONE, savedTrailer.getTractor(),
              "and the trailer it could not hitch is loaded loose");
    }

    @Test
    void aUnitTowingNothingIsLeftAlone() throws Exception {
        Tank loneVehicle = addVehicle(TRACTOR_TONS, false);

        gameManager.restoreTrains(new ArrayList<>(List.of(loneVehicle)), new HashMap<>());

        assertTrue(loneVehicle.getAllTowedUnits().isEmpty());
        assertEquals(Entity.NONE, loneVehicle.getTowing());
    }
}
