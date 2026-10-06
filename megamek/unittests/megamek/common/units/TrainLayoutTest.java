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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the hex-packing rule for tractor-and-trailer trains.
 *
 * <p>"Small and medium Trailers act as part of the Tractor Support Vehicle for purposes of movement, stacking and
 * firing (two small and medium Trailers together act as a single Support Vehicle)" (TM, Trailers). The first trailer
 * shares the tractor's hex and the rest pack two to a hex behind it.</p>
 */
class TrainLayoutTest {

    private static final double TRACTOR_TONS = 75.0;
    private static final double CARRIAGE_TONS = 10.0;
    private static final int FACING_NORTH = 0;

    /** The tractor sits in the last hex; earlier entries lie progressively further back along the train. */
    private static final Coords THIRD_HEX_BACK = new Coords(5, 8);
    private static final Coords SECOND_HEX_BACK = new Coords(5, 7);
    private static final Coords HEX_BEHIND = new Coords(5, 6);
    private static final Coords TRACTOR_HEX = new Coords(5, 5);

    private Game game;
    private Player owner;
    private int nextId = 1;
    private List<Coords> trainPath;
    private List<Integer> trainFacings;

    @BeforeEach
    void setUp() {
        EquipmentType.initializeTypes();
        owner = new Player(0, "Owner");
        owner.setTeam(1);
        game = new Game();
        game.addPlayer(0, owner);

        trainPath = List.of(THIRD_HEX_BACK, SECOND_HEX_BACK, HEX_BEHIND, TRACTOR_HEX);
        trainFacings = new ArrayList<>();
        for (int index = 0; index < trainPath.size(); index++) {
            trainFacings.add(FACING_NORTH);
        }
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

    /** A tractor sitting in TRACTOR_HEX with the requested number of trailers hitched behind it, in order. */
    private Tank buildTrain(int trailerCount) throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, false);
        tractor.setPosition(TRACTOR_HEX);
        tractor.setFacing(FACING_NORTH);
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
    void firstTrailerSharesTheTractorHex() throws Exception {
        Tank tractor = buildTrain(1);

        TrainLayout.layOutTrain(game, tractor, trainPath, trainFacings);

        assertEquals(TRACTOR_HEX, trailerAt(tractor, 0).getPosition(),
              "The first trailer rides in the tractor's own hex");
        assertEquals(FACING_NORTH, trailerAt(tractor, 0).getFacing());
    }

    @Test
    void secondAndThirdTrailersShareTheHexBehind() throws Exception {
        Tank tractor = buildTrain(3);

        TrainLayout.layOutTrain(game, tractor, trainPath, trainFacings);

        assertEquals(TRACTOR_HEX, trailerAt(tractor, 0).getPosition());
        assertEquals(HEX_BEHIND, trailerAt(tractor, 1).getPosition(),
              "Two trailers to a hex behind the tractor");
        assertEquals(HEX_BEHIND, trailerAt(tractor, 2).getPosition(),
              "Two trailers to a hex behind the tractor");
    }

    @Test
    void fourthTrailerStartsTheNextHexBack() throws Exception {
        Tank tractor = buildTrain(4);

        TrainLayout.layOutTrain(game, tractor, trainPath, trainFacings);

        assertEquals(HEX_BEHIND, trailerAt(tractor, 2).getPosition());
        assertEquals(SECOND_HEX_BACK, trailerAt(tractor, 3).getPosition(),
              "The fourth trailer moves one hex further back");
    }

    @Test
    void deploymentPathRunsBackwardsFromTheTractorHex() {
        // Facing 0 is north, so the train trails away to the south.
        List<Coords> path = TrainLayout.deploymentPath(TRACTOR_HEX, FACING_NORTH, 3);

        assertEquals(4, path.size(), "One hex per trailer plus the tractor's own");
        assertEquals(TRACTOR_HEX, path.get(path.size() - 1), "The tractor's hex is last");
        assertEquals(HEX_BEHIND, path.get(path.size() - 2), "Earlier entries run backwards along the train");
        assertEquals(SECOND_HEX_BACK, path.get(path.size() - 3));
    }

    @Test
    void deploymentFootprintCoversEveryOccupiedHex() throws Exception {
        Tank tractor = buildTrain(3);

        Set<Coords> footprint = TrainLayout.deploymentFootprint(game, tractor, TRACTOR_HEX, FACING_NORTH);

        // Trailer 0 rides in the tractor's hex, trailers 1 and 2 share the hex behind it.
        assertEquals(Set.of(TRACTOR_HEX, HEX_BEHIND), footprint);
    }

    @Test
    void aLoneTractorFootprintIsJustItsOwnHex() throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, false);
        tractor.setPosition(TRACTOR_HEX);
        tractor.setFacing(FACING_NORTH);

        Set<Coords> footprint = TrainLayout.deploymentFootprint(game, tractor, TRACTOR_HEX, FACING_NORTH);

        assertEquals(Set.of(TRACTOR_HEX), footprint);
    }

    @Test
    void trailersTakeTheFacingOfTheHexTheyOccupy() throws Exception {
        Tank tractor = buildTrain(2);
        List<Integer> turningFacings = List.of(3, 2, 1, FACING_NORTH);

        TrainLayout.layOutTrain(game, tractor, trainPath, turningFacings);

        assertEquals(FACING_NORTH, trailerAt(tractor, 0).getFacing(),
              "The first trailer matches the tractor");
        assertEquals(1, trailerAt(tractor, 1).getFacing(),
              "A trailing unit keeps the facing the tractor had in that hex");
    }
}
