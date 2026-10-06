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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import megamek.common.OffBoardDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests which units take a deployment turn of their own once they are part of a train.
 *
 * <p>A trailer normally rides along with its tractor and takes no turn. That only holds while the tractor is itself
 * deploying onto the board: a tractor that starts off board never takes a deployment turn, so a trailer behind it
 * would never be placed at all.</p>
 */
class TrailerDeploymentTurnTest {

    private static final int FIRST_ROUND = 0;

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

    private Tank buildVehicle(boolean isTrailer) throws Exception {
        Tank vehicle = new Tank();
        vehicle.setId(nextId++);
        vehicle.setOwner(owner);
        vehicle.setWeight(isTrailer ? 10.0 : 75.0);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        vehicle.setTrailer(isTrailer);
        vehicle.addEquipment(EquipmentType.get(EquipmentTypeLookup.HITCH), Tank.LOC_BODY);
        vehicle.setTrailerHitches();
        game.addEntity(vehicle);
        return vehicle;
    }

    @Test
    void anUnattachedTrailerDeploysOnItsOwn() throws Exception {
        Tank looseTrailer = buildVehicle(true);

        assertTrue(looseTrailer.shouldDeploy(FIRST_ROUND),
              "A trailer with no tractor has to place itself");
    }

    @Test
    void aTrailerBehindAnOnBoardTractorDoesNotDeployOnItsOwn() throws Exception {
        Tank tractor = buildVehicle(false);
        Tank carriage = buildVehicle(true);
        tractor.towUnit(carriage.getId());

        assertTrue(tractor.shouldDeploy(FIRST_ROUND), "The tractor still deploys");
        assertFalse(carriage.shouldDeploy(FIRST_ROUND),
              "The tractor places this trailer, so it takes no turn of its own");
    }

    @Test
    void aTrailerBehindAnOffBoardTractorStillDeploysItself() throws Exception {
        Tank tractor = buildVehicle(false);
        Tank carriage = buildVehicle(true);
        tractor.towUnit(carriage.getId());

        tractor.setOffBoard(17, OffBoardDirection.NORTH);

        assertFalse(tractor.shouldDeploy(FIRST_ROUND), "An off board tractor takes no deployment turn");
        assertTrue(carriage.shouldDeploy(FIRST_ROUND),
              "Nothing will place this trailer, so it must still deploy itself rather than be stranded");
    }
}
