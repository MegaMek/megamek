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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

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
 * Tests the all-or-nothing build-train request.
 *
 * <p>A rejected request must leave every unit unattached rather than producing a partially built train, and trailers
 * must end up in the order the client asked for, because that order fixes the hitch chain and so where each unit
 * sits on the board.</p>
 */
class TrainBuildHandlerTest {

    private static final double TRACTOR_TONS = 75.0;
    private static final double CARRIAGE_TONS = 10.0;

    private Game game;
    private Player owner;
    private Player otherPlayer;
    private TWGameManager gameManager;
    private TrainBuildHandler handler;
    private int nextId = 1;

    @BeforeEach
    void setUp() {
        EquipmentType.initializeTypes();
        owner = new Player(0, "Owner");
        owner.setTeam(1);
        otherPlayer = new Player(1, "Other");
        otherPlayer.setTeam(2);

        game = new Game();
        game.setPhase(GamePhase.LOUNGE);
        game.addPlayer(0, owner);
        game.addPlayer(1, otherPlayer);

        gameManager = mock(TWGameManager.class);
        doNothing().when(gameManager).entityUpdate(anyInt());
        doNothing().when(gameManager).sendServerChat(anyString());
        when(gameManager.getGame()).thenReturn(game);
        doCallRealMethod().when(gameManager).setGame(any(Game.class));
        gameManager.setGame(game);

        handler = new TrainBuildHandler(gameManager);
    }

    private Tank buildVehicle(double tonnage, boolean isTrailer, Player vehicleOwner) throws Exception {
        Tank vehicle = new Tank();
        vehicle.setId(nextId++);
        vehicle.setOwner(vehicleOwner);
        vehicle.setWeight(tonnage);
        vehicle.setMovementMode(EntityMovementMode.TRACKED);
        vehicle.setTrailer(isTrailer);
        vehicle.addEquipment(EquipmentType.get(EquipmentTypeLookup.HITCH), Tank.LOC_BODY);
        vehicle.setTrailerHitches();
        game.addEntity(vehicle);
        return vehicle;
    }

    private Tank tractor() throws Exception {
        return buildVehicle(TRACTOR_TONS, false, owner);
    }

    private Tank carriage() throws Exception {
        return buildVehicle(CARRIAGE_TONS, true, owner);
    }

    private void assertNoTrain(Entity tractor, Entity... trailers) {
        assertTrue(tractor.getAllTowedUnits().isEmpty(), "Tractor should tow nothing");
        assertEquals(Entity.NONE, tractor.getTowing(), "Tractor should have nothing hitched");
        for (Entity trailer : trailers) {
            assertEquals(Entity.NONE, trailer.getTractor(), "Trailer should not belong to a train");
            assertEquals(Entity.NONE, trailer.getTowedBy(), "Trailer should not be hitched");
        }
    }

    @Test
    void buildsTheTrainInTheRequestedOrder() throws Exception {
        Tank tractor = tractor();
        Tank first = carriage();
        Tank second = carriage();

        handler.buildTrain(tractor.getId(), List.of(first.getId(), second.getId()), owner);

        assertEquals(List.of(first.getId(), second.getId()), tractor.getAllTowedUnits(),
              "Train order must match the requested order");
        assertEquals(first.getId(), tractor.getTowing(), "The first trailer rides directly behind the tractor");
        assertEquals(second.getId(), first.getTowing(), "The second trailer rides behind the first");
        assertEquals(tractor.getId(), first.getTractor());
        assertEquals(tractor.getId(), second.getTractor());
    }

    @Test
    void reversedOrderIsHonoured() throws Exception {
        Tank tractor = tractor();
        Tank first = carriage();
        Tank second = carriage();

        handler.buildTrain(tractor.getId(), List.of(second.getId(), first.getId()), owner);

        assertEquals(List.of(second.getId(), first.getId()), tractor.getAllTowedUnits(),
              "The client's ordering is taken verbatim");
    }

    @Test
    void aNonTrailerInTheListIsRejectedWholesale() throws Exception {
        Tank tractor = tractor();
        Tank good = carriage();
        Tank notATrailer = buildVehicle(CARRIAGE_TONS, false, owner);

        handler.buildTrain(tractor.getId(), List.of(good.getId(), notATrailer.getId()), owner);

        assertNoTrain(tractor, good, notATrailer);
    }

    @Test
    void aTrailerOwnedByAnotherPlayerIsRejected() throws Exception {
        Tank tractor = tractor();
        Tank good = carriage();
        Tank enemyTrailer = buildVehicle(CARRIAGE_TONS, true, otherPlayer);

        handler.buildTrain(tractor.getId(), List.of(good.getId(), enemyTrailer.getId()), owner);

        assertNoTrain(tractor, good, enemyTrailer);
    }

    @Test
    void aDuplicatedTrailerIsRejected() throws Exception {
        Tank tractor = tractor();
        Tank carriage = carriage();

        handler.buildTrain(tractor.getId(), List.of(carriage.getId(), carriage.getId()), owner);

        assertNoTrain(tractor, carriage);
    }

    @Test
    void exceedingTowingCapacityRollsBackTheWholeTrain() throws Exception {
        Tank tractor = tractor();
        Tank light = carriage();
        // 10 + 70 tons is over the 75 ton tractor's capacity, so the second link fails and the first must be undone.
        Tank tooHeavy = buildVehicle(70.0, true, owner);

        handler.buildTrain(tractor.getId(), List.of(light.getId(), tooHeavy.getId()), owner);

        assertNoTrain(tractor, light, tooHeavy);
    }

    @Test
    void aTrailerAlreadyInATrainIsRejected() throws Exception {
        Tank firstTractor = tractor();
        Tank carriage = carriage();
        firstTractor.towUnit(carriage.getId());

        Tank secondTractor = tractor();
        handler.buildTrain(secondTractor.getId(), List.of(carriage.getId()), owner);

        assertTrue(secondTractor.getAllTowedUnits().isEmpty(), "The second tractor should tow nothing");
        assertEquals(firstTractor.getId(), carriage.getTractor(), "The carriage stays with its original train");
    }

    @Test
    void anEmptyTrailerListIsRejected() throws Exception {
        Tank tractor = tractor();

        handler.buildTrain(tractor.getId(), List.of(), owner);

        assertTrue(tractor.getAllTowedUnits().isEmpty());
    }
}
