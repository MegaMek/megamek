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

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers working out where a unit sits in its train.
 *
 * <p>A convoy can carry several identical carriages, so the unit name alone does not say which is which, and the
 * order decides which hex each one occupies. The label is used in the lobby unit list and in the weapon panel's
 * ammo dropdown, where ammo can come from any unit in the train.</p>
 */
class TrainPositionTest {

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

    private Tank buildTrain(int trailerCount) throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, false);
        for (int index = 0; index < trailerCount; index++) {
            tractor.towUnit(buildVehicle(CARRIAGE_TONS, true).getId());
        }
        return tractor;
    }

    private Entity trailerAt(Tank tractor, int trailerNumber) {
        return game.getEntity(tractor.getAllTowedUnits().get(trailerNumber));
    }

    @Test
    void theTractorIsLabelledTR() throws Exception {
        Tank tractor = buildTrain(2);

        assertEquals(TrainLayout.TRACTOR_POSITION, TrainLayout.trainPosition(tractor));
    }

    @Test
    void trailersAreNumberedFromTheFront() throws Exception {
        Tank tractor = buildTrain(3);

        assertEquals(1, TrainLayout.trainPosition(trailerAt(tractor, 0)));
        assertEquals(2, TrainLayout.trainPosition(trailerAt(tractor, 1)));
        assertEquals(3, TrainLayout.trainPosition(trailerAt(tractor, 2)));
    }

    @Test
    void aUnitInNoTrainHasNoLabel() throws Exception {
        Tank loneVehicle = buildVehicle(TRACTOR_TONS, false);
        Tank looseTrailer = buildVehicle(CARRIAGE_TONS, true);

        assertEquals(TrainLayout.NOT_IN_TRAIN, TrainLayout.trainPosition(loneVehicle),
              "A unit towing nothing and towed by nothing is not in a train");
        assertEquals(TrainLayout.NOT_IN_TRAIN, TrainLayout.trainPosition(looseTrailer),
              "An unhitched trailer has no place in a train yet");
    }

    @Test
    void nullIsHandled() {
        assertEquals(TrainLayout.NOT_IN_TRAIN, TrainLayout.trainPosition(null));
    }

    @Test
    void labelsFollowTheTrainAfterATrailerIsDropped() throws Exception {
        Tank tractor = buildTrain(3);
        Entity leadTrailer = trailerAt(tractor, 0);

        // Dropping the lead trailer takes the two behind it out of the train as well.
        tractor.disconnectUnit(leadTrailer.getId());

        assertEquals(TrainLayout.NOT_IN_TRAIN, TrainLayout.trainPosition(leadTrailer), "It is no longer in a train");
        assertEquals(TrainLayout.NOT_IN_TRAIN, TrainLayout.trainPosition(tractor), "and the tractor tows nothing now");
    }
}
