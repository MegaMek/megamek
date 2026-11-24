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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Tank;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.server.Server;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * An immobile vehicle on unbroken ice must not sink at the end of the phase (issue 9633: a hover tank whose engine was
 * destroyed over frozen water died to "a watery grave" in the Physical phase).
 */
class ImmobileTankOnIceTest {

    private static final int BOARD_WIDTH = 16;
    private static final int BOARD_HEIGHT = 17;
    private static final Coords TANK_POSITION = new Coords(4, 4);

    private TWGameManager gameManager;
    private Server server;
    private Game game;
    private Player player;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void buildGame() throws IOException {
        gameManager = new TWGameManager();
        server = ServerFactory.createServer(gameManager);
        game = gameManager.getGame();
        Hex[] hexes = new Hex[BOARD_WIDTH * BOARD_HEIGHT];
        for (int i = 0; i < hexes.length; i++) {
            hexes[i] = new Hex();
        }
        game.setBoard(new Board(BOARD_WIDTH, BOARD_HEIGHT, hexes));

        player = new Player(0, "Player");
        player.setTeam(1);
        game.addPlayer(player.getId(), player);
        game.setupTeams();
    }

    @AfterEach
    void tearDown() {
        server.die();
    }

    private void setTankHexTerrain(boolean withIce) {
        Hex hex = game.getBoard().getHex(TANK_POSITION);
        hex.addTerrain(new Terrain(Terrains.WATER, 2));
        if (withIce) {
            hex.addTerrain(new Terrain(Terrains.ICE, 1));
        }
    }

    private Tank addImmobileTank(EntityMovementMode movementMode) {
        Tank tank = new Tank();
        tank.setChassis("Test");
        tank.setModel("Tank");
        tank.setCrew(new Crew(CrewType.SINGLE));
        tank.setId(game.getNextEntityId());
        tank.setOwner(player);
        tank.setMovementMode(movementMode);
        tank.setDeployed(true);
        tank.setPosition(TANK_POSITION);
        game.addEntity(tank);
        tank.immobilize();
        tank.applyDamage();
        return tank;
    }

    private boolean isSunk(Tank tank) {
        return tank.isDoomed() || tank.isDestroyed();
    }

    @Test
    @DisplayName("An immobile hover tank on unbroken ice does not sink")
    void immobileHoverOnIceSurvives() {
        setTankHexTerrain(true);
        Tank tank = addImmobileTank(EntityMovementMode.HOVER);
        assertTrue(tank.isImmobile(), "the test needs an immobile tank");

        gameManager.resolveSinkVees();

        assertFalse(isSunk(tank), "unbroken ice holds the tank up");
    }

    @Test
    @DisplayName("An immobile tracked tank on unbroken ice does not sink")
    void immobileTrackedOnIceSurvives() {
        setTankHexTerrain(true);
        Tank tank = addImmobileTank(EntityMovementMode.TRACKED);
        assertTrue(tank.isImmobile(), "the test needs an immobile tank");

        gameManager.resolveSinkVees();

        assertFalse(isSunk(tank), "unbroken ice holds the tank up");
    }

    @Test
    @DisplayName("An immobile hover tank on open water still sinks")
    void immobileHoverOnOpenWaterSinks() {
        setTankHexTerrain(false);
        Tank tank = addImmobileTank(EntityMovementMode.HOVER);
        assertTrue(tank.isImmobile(), "the test needs an immobile tank");

        gameManager.resolveSinkVees();

        assertTrue(isSunk(tank), "with no ice the tank sinks");
    }
}
