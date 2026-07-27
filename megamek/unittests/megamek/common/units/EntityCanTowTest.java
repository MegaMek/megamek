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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.TankTrailerHitch;
import megamek.common.equipment.Transporter;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the towing capacity rule in {@link Entity#canTow(int)}.
 *
 * <p>"Wheeled and tracked Tractors may pull one or more Trailers whose combined weight is less than or equal to the
 * Tractor's own weight" (TM, Tractors). The limit therefore belongs to the powered tractor at the head of the train,
 * not to whichever unit a new trailer happens to be hitched to. Because trailers are always appended at the tail, the
 * attach point is usually a light trailer, and measuring against it produced a limit far below the real one.</p>
 */
class EntityCanTowTest {

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

    /** A tracked vehicle with a rear trailer hitch, so it can act as a tractor or an attach point. */
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

    @Test
    void tractorMayTowUpToItsOwnTonnage() throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, false);
        Tank carriage = buildVehicle(CARRIAGE_TONS, true);

        assertTrue(tractor.canTow(carriage.getId()),
              "A 75 ton tractor should tow a 10 ton trailer");
    }

    @Test
    void tractorMayNotTowMoreThanItsOwnTonnage() throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, false);
        Tank tooHeavy = buildVehicle(TRACTOR_TONS + 5.0, true);

        assertFalse(tractor.canTow(tooHeavy.getId()),
              "Combined trailer weight above the tractor's own tonnage must be refused");
    }

    @Test
    void capacityIsMeasuredAgainstTheTractorNotTheAttachPoint() throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, false);
        Tank carriage = buildVehicle(CARRIAGE_TONS, true);
        tractor.towUnit(carriage.getId());

        // Hitching to the 10 ton carriage at the tail. The train is 10 of the tractor's 75 tons used, so a 50 ton
        // trailer still fits. Measuring against the carriage instead would cap the whole train at 10 tons.
        Tank secondTrailer = buildVehicle(50.0, true);

        assertTrue(carriage.canTow(secondTrailer.getId()),
              "Remaining capacity comes from the tractor, not from the trailer being hitched to");
    }

    /** A trailer with no hitch of its own: it can be towed, but nothing can be hitched behind it. */
    private Tank buildHitchlessTrailer(double tonnage) throws Exception {
        Tank vehicle = new Tank();
        vehicle.setId(nextId++);
        vehicle.setOwner(owner);
        vehicle.setWeight(tonnage);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        vehicle.setTrailer(true);
        game.addEntity(vehicle);
        return vehicle;
    }

    @Test
    void aTrainEndingInAHitchlessTrailerCannotTakeAnother() throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, false);
        Tank hitchless = buildHitchlessTrailer(CARRIAGE_TONS);
        tractor.towUnit(hitchless.getId());

        Tank another = buildVehicle(CARRIAGE_TONS, true);

        // A free hitch elsewhere in the train is not one this trailer could use, because towUnit appends at the tail.
        assertFalse(tractor.canTow(another.getId()),
              "Nothing at the back of the train can hold another trailer");
    }

    @Test
    void aTrailerThatCannotBeHitchedIsNotLeftInTheTrain() throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, false);
        Tank hitchless = buildHitchlessTrailer(CARRIAGE_TONS);
        tractor.towUnit(hitchless.getId());

        Tank another = buildVehicle(CARRIAGE_TONS, true);
        tractor.towUnit(another.getId());

        assertFalse(tractor.getAllTowedUnits().contains(another.getId()),
              "A trailer with no hitch holding it must not count as part of the train");
        assertEquals(Entity.NONE, another.getTractor(), "and must not think it has a tractor");
        assertEquals(Entity.NONE, another.getTowedBy(), "and must not think it is hitched");
    }

    @Test
    void aTrailerIsHeldByOneHitchOnly() throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, false);
        Tank carriage = buildVehicle(CARRIAGE_TONS, true);

        tractor.towUnit(carriage.getId());

        int holdingHitches = 0;
        for (Transporter transporter : tractor.getTransports()) {
            if ((transporter instanceof TankTrailerHitch hitch) && !hitch.getLoadedUnits().isEmpty()) {
                holdingHitches++;
            }
        }
        assertEquals(1, holdingHitches, "The trailer occupies a single hitch, not every hitch on the tractor");
    }

    @Test
    void trailersAlreadyInTheTrainCountAgainstCapacity() throws Exception {
        Tank tractor = buildVehicle(TRACTOR_TONS, false);
        Tank carriage = buildVehicle(CARRIAGE_TONS, true);
        tractor.towUnit(carriage.getId());

        // 10 tons already towed, so a 70 ton trailer would put the train at 80 of a 75 ton limit.
        Tank tooHeavy = buildVehicle(70.0, true);

        assertFalse(carriage.canTow(tooHeavy.getId()),
              "Weight already in the train must count against the tractor's capacity");
    }
}
