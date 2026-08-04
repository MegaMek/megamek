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
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests how towed trailers slow a tractor down.
 *
 * <p>"if the Trailers weigh up to a quarter of the Tractor's own weight, the Tractor must subtract 3 from its
 * Cruising MP or half of its Cruising MP (round down), whichever is less. If the Trailers weigh more than a quarter
 * of the Tractor's tonnage, the Tractor may only move at half its Cruising MP (round down)." (TM, Tractors)</p>
 *
 * <p>Flank MP is not adjusted separately: it is derived from the reduced Cruising MP and already rounds .5 up.</p>
 */
class TractorMovementTest {

    /** Matches the Mobile Long Tom LT-MOB-25: 75 tons, tracked, Cruising 3. */
    private static final double TRACTOR_TONS = 75.0;
    private static final int TRACTOR_CRUISE = 3;
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

    private Tank buildVehicle(double tonnage, int cruiseMP, boolean isTrailer) throws Exception {
        Tank vehicle = new Tank();
        vehicle.setId(nextId++);
        vehicle.setOwner(owner);
        vehicle.setWeight(tonnage);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        vehicle.setTrailer(isTrailer);
        vehicle.setOriginalWalkMP(cruiseMP);
        int engineRating = Math.max(10, (cruiseMP * (int) tonnage) - Tank.getSuspensionFactor(
              EntityMovementMode.TRACKED, tonnage));
        vehicle.setEngine(new Engine(engineRating, Engine.COMBUSTION_ENGINE, 0));
        vehicle.addEquipment(EquipmentType.get(EquipmentTypeLookup.HITCH), Tank.LOC_BODY);
        vehicle.setTrailerHitches();
        game.addEntity(vehicle);
        return vehicle;
    }

    private Tank tractorTowing(int carriageCount) throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, TRACTOR_CRUISE, false);
        for (int index = 0; index < carriageCount; index++) {
            Tank carriage = buildVehicle(CARRIAGE_TONS, 0, true);
            tractor.towUnit(carriage.getId());
        }
        return tractor;
    }

    @Test
    void anUnladenTractorKeepsItsFullMovement() throws Exception {
        Tank tractor = tractorTowing(0);

        assertEquals(TRACTOR_CRUISE, tractor.getWalkMP(), "No trailers, no penalty");
        assertEquals(5, tractor.getRunMP(), "Flank is Cruising times 1.5, rounding .5 up");
    }

    @Test
    void aLightLoadCostsTheLesserOfThreeAndHalfCruising() throws Exception {
        // 10 tons is under a quarter of 75, so the reduction is min(3, floor(3/2)) = 1.
        Tank tractor = tractorTowing(1);

        assertEquals(2, tractor.getWalkMP(), "Cruising 3 loses 1");
        assertEquals(3, tractor.getRunMP(), "Flank follows the reduced Cruising MP");
    }

    @Test
    void aHeavyLoadHalvesCruisingMovement() throws Exception {
        // 20 tons is over a quarter of 75, so Cruising is halved outright.
        Tank tractor = tractorTowing(2);

        assertEquals(1, tractor.getWalkMP(), "Cruising 3 halves to 1");
        assertEquals(2, tractor.getRunMP());
    }

    @Test
    void aLongTrainDoesNotDriveMovementToZero() throws Exception {
        // The penalty depends on the quarter-weight threshold, not on the total, so piling on carriages cannot
        // strand the train. Eight carriages weigh more than the tractor, which canTow would refuse, but the
        // movement rule still has to behave.
        Tank tractor = tractorTowing(8);

        assertEquals(1, tractor.getWalkMP(), "Halved Cruising MP is the floor, not zero");
    }

    @Test
    void theQuarterWeightThresholdIsExclusive() throws Exception {
        // Exactly a quarter of the tractor's weight still counts as the lighter case.
        Tank tractor = buildVehicle(TRACTOR_TONS, TRACTOR_CRUISE, false);
        Tank carriage = buildVehicle(TRACTOR_TONS / 4.0, 0, true);
        tractor.towUnit(carriage.getId());

        assertEquals(2, tractor.getWalkMP(), "A load of exactly a quarter takes the smaller penalty");
    }

    @Test
    void aFastTractorLosesAtMostThree() throws Exception {
        // With Cruising 8, half is 4, so the lighter case caps the loss at 3.
        Tank tractor = buildVehicle(TRACTOR_TONS, 8, false);
        Tank carriage = buildVehicle(CARRIAGE_TONS, 0, true);
        tractor.towUnit(carriage.getId());

        assertEquals(5, tractor.getWalkMP(), "Cruising 8 loses 3, not 4");
    }
}
