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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers a trailer that is set to deploy off board while its tractor deploys onto the board.
 *
 * <p>A towed gun trailer is hauled into position and then emplaced, so a trailer flagged for off board deployment
 * leaves the train rather than being dragged onto the board with its tractor. Only a run at the very back can go: a
 * trailer dropped from the middle takes every trailer behind it out of the train too, stranding units that were
 * meant to deploy with the tractor.</p>
 */
class OffBoardTrailerDeploymentTest {

    private static final double TRACTOR_TONS = 75.0;
    private static final double CARRIAGE_TONS = 10.0;
    private static final int OFF_BOARD_DISTANCE = 17;
    private static final int FIRST_ROUND = 0;

    private Game game;
    private Player owner;
    private DeploymentProcessor deploymentProcessor;
    private int nextId = 1;

    @BeforeEach
    void setUp() {
        EquipmentType.initializeTypes();
        owner = new Player(0, "Owner");
        owner.setTeam(1);

        game = new Game();
        game.setPhase(GamePhase.DEPLOYMENT);
        game.addPlayer(0, owner);

        TWGameManager gameManager = mock(TWGameManager.class);
        doNothing().when(gameManager).entityUpdate(anyInt());
        when(gameManager.getGame()).thenReturn(game);
        doCallRealMethod().when(gameManager).setGame(any(Game.class));
        gameManager.setGame(game);

        deploymentProcessor = new DeploymentProcessor(gameManager);
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
            Tank carriage = buildVehicle(CARRIAGE_TONS, true);
            tractor.towUnit(carriage.getId());
        }
        return tractor;
    }

    private Entity trailerAt(Tank tractor, int trailerNumber) {
        return game.getEntity(tractor.getAllTowedUnits().get(trailerNumber));
    }

    private static void setOffBoard(Entity entity) {
        entity.setOffBoard(OFF_BOARD_DISTANCE, OffBoardDirection.NORTH);
    }

    @Test
    void aTrailerWithNoTractorDeploysOffBoardOnItsOwn() throws Exception {
        // The simplest way to field a towed gun: emplace it off board and never hitch it to anything. It has no
        // engine, but an emplaced artillery piece does not need one.
        Tank looseTrailer = buildVehicle(CARRIAGE_TONS, true);
        setOffBoard(looseTrailer);

        assertFalse(looseTrailer.shouldDeploy(FIRST_ROUND),
              "Off board units never take a deployment turn");
        assertTrue(looseTrailer.shouldOffBoardDeploy(FIRST_ROUND),
              "but it does deploy itself off board, with no tractor involved");
    }

    @Test
    void aLoneOffBoardTrailerIsUntouchedByTheTrainCode() throws Exception {
        Tank tractor = buildTrain(1);
        Tank looseTrailer = buildVehicle(CARRIAGE_TONS, true);
        setOffBoard(looseTrailer);

        deploymentProcessor.clearTrailerOffBoardSettings(tractor);

        assertEquals(Entity.NONE, looseTrailer.getTractor(), "It was never part of anyone's train");
        assertTrue(looseTrailer.isOffBoard(), "and its off board setting is left alone");
        assertEquals(1, tractor.getAllTowedUnits().size(), "The unrelated train is unaffected");
    }

    @Test
    void aHitchedTrailersOwnOffBoardSettingIsCleared() throws Exception {
        Tank tractor = buildTrain(2);
        Entity leadTrailer = trailerAt(tractor, 0);
        Entity gunTrailer = trailerAt(tractor, 1);
        setOffBoard(gunTrailer);

        deploymentProcessor.clearTrailerOffBoardSettings(tractor);

        assertFalse(gunTrailer.isOffBoard(),
              "A train deploys where its tractor does, so a trailer keeps no deployment of its own");
        assertEquals(2, tractor.getAllTowedUnits().size(), "and the train stays whole");
        assertEquals(tractor.getId(), leadTrailer.getTractor());
    }
}
