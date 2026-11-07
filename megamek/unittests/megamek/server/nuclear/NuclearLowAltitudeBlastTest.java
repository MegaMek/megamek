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
package megamek.server.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.io.File;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.NuclearWeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.packets.Packet;
import megamek.common.options.GameOptions;
import megamek.common.units.Entity;
import megamek.common.units.Terrains;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * A nuclear blast on the Low-Altitude Map (IO:AE p.166): a ground map inside a sky map, with fighters flying above it.
 */
class NuclearLowAltitudeBlastTest {
    private static final String UNIT_FILES = "testresources/megamek/common/units/";
    private static final String FIGHTER = "Mosquito Light Fighter.blk";
    private static final int GROUND_BOARD = 0;
    private static final int SKY_BOARD = 1;
    private static final int GROUND_SIZE = 16;
    private static final int SKY_SIZE = 20;
    /** The sky hex that holds the ground map. */
    private static final Coords GROUND_MAP_IN_THE_SKY = new Coords(5, 5);
    private static final Coords GROUND_ZERO = new Coords(8, 8);

    private TWGameManager gameManager;
    private Game game;
    private Server server;
    private Player player;
    private Board skyBoard;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).send(anyInt(), any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), anyInt());
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());
        game = gameManager.getGame();
        game.setOptions(new GameOptions());
        server = ServerFactory.createServer(gameManager);
        player = new Player(0, "Test");
        game.addPlayer(0, player);

        Hex[] hexes = new Hex[GROUND_SIZE * GROUND_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex(0);
        }
        Board groundBoard = new Board();
        groundBoard.newData(GROUND_SIZE, GROUND_SIZE, hexes, null);
        groundBoard.setBoardId(GROUND_BOARD);
        game.setBoard(GROUND_BOARD, groundBoard);

        skyBoard = Board.getSkyBoard(SKY_SIZE, SKY_SIZE);
        skyBoard.setBoardId(SKY_BOARD);
        game.setBoard(SKY_BOARD, skyBoard);
        skyBoard.setEmbeddedBoard(GROUND_BOARD, GROUND_MAP_IN_THE_SKY);
        groundBoard.setEnclosingBoard(SKY_BOARD);
    }

    @AfterEach
    void afterEach() {
        server.die();
    }

    private Entity placeFighter(Coords skyHex, int altitude) throws Exception {
        Entity fighter = new MekFileParser(new File(UNIT_FILES + FIGHTER)).getEntity();
        fighter.setId(game.getNextEntityId());
        game.addEntity(fighter);
        fighter.setOwner(player);
        fighter.setBoardId(SKY_BOARD);
        fighter.setPosition(skyHex);
        fighter.setAltitude(altitude);
        fighter.setDeployed(true);
        return fighter;
    }

    /** A sky hex straight south of the ground map's hex, so its distance is exactly {@code distance}. */
    private static Coords skyHexSouth(int distance) {
        return new Coords(GROUND_MAP_IN_THE_SKY.getX(), GROUND_MAP_IN_THE_SKY.getY() + distance);
    }

    private void detonateOnTheGround() {
        new NuclearDetonationResolver(gameManager).resolve(GROUND_ZERO, GROUND_BOARD,
              NuclearBlastProfile.airBurst(NuclearWeaponType.ALAMO), new Vector<Report>());
    }

    private static boolean isHit(Entity fighter, int armorBefore) {
        return fighter.isDoomed() || fighter.isDestroyed() || (fighter.getTotalArmor() < armorBefore);
    }

    @Test
    void groundMapDetonationIsPlacedAtTheSkyHexHoldingThatMap() {
        BoardLocation skyGroundZero = NuclearLowAltitudeBlast.lowAltitudeGroundZero(game, GROUND_ZERO, GROUND_BOARD);

        assertEquals(BoardLocation.of(GROUND_MAP_IN_THE_SKY, SKY_BOARD), skyGroundZero);
    }

    @Test
    void groundMapWithNoSkyMapAboveItHasNoLowAltitudeBlast() {
        game.getBoard(GROUND_BOARD).setEnclosingBoard(Board.BOARD_NONE);

        assertNull(NuclearLowAltitudeBlast.lowAltitudeGroundZero(game, GROUND_ZERO, GROUND_BOARD));
    }

    @Test
    void fighterThreeSkyHexesAwayBelowBlastHeightIsHit() throws Exception {
        // The Stuka example of IO:AE p.166: 3 hexes out at altitude 4 takes 541
        Entity fighter = placeFighter(skyHexSouth(3), 4);
        int armorBefore = fighter.getTotalArmor();

        detonateOnTheGround();

        assertTrue(isHit(fighter, armorBefore), "inside 4 sky hexes and below blast height 8");
    }

    @Test
    void fighterAboveBlastHeightIsUntouched() throws Exception {
        Entity fighter = placeFighter(skyHexSouth(3), 9);
        int armorBefore = fighter.getTotalArmor();

        detonateOnTheGround();

        assertFalse(isHit(fighter, armorBefore), "altitude 9 is above the Alamo's blast height 8");
    }

    @Test
    void fighterBeyondTheSkyRadiusIsUntouched() throws Exception {
        Entity fighter = placeFighter(skyHexSouth(5), 4);
        int armorBefore = fighter.getTotalArmor();

        detonateOnTheGround();

        assertFalse(isHit(fighter, armorBefore), "5 sky hexes out, beyond 58 / 17 = 4");
    }

    @Test
    void detonationOnTheSkyMapHitsFightersButLeavesTheMapAlone() throws Exception {
        Coords skyGroundZero = new Coords(12, 12);
        Entity fighter = placeFighter(skyGroundZero, 4);
        int armorBefore = fighter.getTotalArmor();
        Hex hexBefore = skyBoard.getHex(skyGroundZero).duplicate();

        // A ground burst with a crater: on the sky map it digs nothing
        new NuclearDetonationResolver(gameManager).resolve(skyGroundZero, SKY_BOARD,
              NuclearBlastProfile.groundBurst(NuclearWeaponType.ALAMO), new Vector<Report>());

        assertTrue(isHit(fighter, armorBefore), "in the detonation's own sky hex");
        Hex hexAfter = skyBoard.getHex(skyGroundZero);
        assertEquals(hexBefore.getLevel(), hexAfter.getLevel(), "no crater");
        assertFalse(hexAfter.containsTerrain(Terrains.RUBBLE), "no rubble");
    }
}
