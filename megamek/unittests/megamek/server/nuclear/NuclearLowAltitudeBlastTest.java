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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.io.File;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
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
 * A nuclear blast on the Low-Altitude Map (IO:AE p.166): a detonation on a sky map, with fighters flying around it.
 */
class NuclearLowAltitudeBlastTest {
    private static final String UNIT_FILES = "testresources/megamek/common/units/";
    private static final String FIGHTER = "Mosquito Light Fighter.blk";
    private static final int SKY_BOARD = 0;
    private static final int SKY_SIZE = 20;
    private static final Coords GROUND_ZERO = new Coords(5, 5);

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

        skyBoard = Board.getSkyBoard(SKY_SIZE, SKY_SIZE);
        skyBoard.setBoardId(SKY_BOARD);
        game.setBoard(SKY_BOARD, skyBoard);
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

    /** A sky hex straight south of Ground Zero, so its distance is exactly {@code distance}. */
    private static Coords skyHexSouth(int distance) {
        return new Coords(GROUND_ZERO.getX(), GROUND_ZERO.getY() + distance);
    }

    private void detonateAirBurst() {
        new NuclearDetonationResolver(gameManager).resolve(GROUND_ZERO, SKY_BOARD,
              NuclearBlastProfile.airBurst(NuclearWeaponType.ALAMO), new Vector<Report>());
    }

    /**
     * Runs only the blast step, not the secondary effects, which can also damage or destroy a fighter.
     *
     * @return the ids of the units the blast struck
     */
    private Vector<Integer> blastOnly() {
        Vector<Integer> struckUnits = new Vector<>();
        new NuclearLowAltitudeBlast(gameManager, SKY_BOARD).apply(GROUND_ZERO,
              NuclearBlastProfile.airBurst(NuclearWeaponType.ALAMO), new Vector<Report>(), struckUnits);
        return struckUnits;
    }

    private static boolean isHit(Entity fighter, int armorBefore) {
        return fighter.isDoomed() || fighter.isDestroyed() || (fighter.getTotalArmor() < armorBefore);
    }

    @Test
    void fighterThreeSkyHexesAwayBelowBlastHeightIsHit() throws Exception {
        // The Stuka example of IO:AE p.166: 3 hexes out at altitude 4 takes 541
        Entity fighter = placeFighter(skyHexSouth(3), 4);

        Vector<Integer> struckUnits = blastOnly();

        assertTrue(struckUnits.contains(fighter.getId()), "inside 4 sky hexes and below blast height 8");
    }

    @Test
    void fighterAboveBlastHeightIsUntouched() throws Exception {
        Entity fighter = placeFighter(skyHexSouth(3), 9);

        Vector<Integer> struckUnits = blastOnly();

        assertFalse(struckUnits.contains(fighter.getId()), "altitude 9 is above the Alamo's blast height 8");
    }

    @Test
    void fighterBeyondTheSkyRadiusIsUntouched() throws Exception {
        Entity fighter = placeFighter(skyHexSouth(5), 4);

        Vector<Integer> struckUnits = blastOnly();

        assertFalse(struckUnits.contains(fighter.getId()), "5 sky hexes out, beyond 58 / 17 = 4");
    }

    @Test
    void fighterInTheSecondaryRadiusSuffersEmi() throws Exception {
        // 6 sky hexes out: beyond the blast (4) but inside the secondary radius (114 / 17 = 7), below height 9
        Entity fighter = placeFighter(skyHexSouth(6), 9);
        int armorBefore = fighter.getTotalArmor();

        detonateAirBurst();

        assertEquals(armorBefore, fighter.getTotalArmor(), "no blast damage this far out");
        assertTrue(fighter.isSufferingEMI(), "secondary effects reach it");
    }

    @Test
    void fighterAboveSecondaryHeightEscapesTheSecondaryEffects() throws Exception {
        Entity fighter = placeFighter(skyHexSouth(6), 10);

        detonateAirBurst();

        assertFalse(fighter.isSufferingEMI(), "altitude 10 is above the Alamo's secondary effect height 9");
    }

    @Test
    void fighterBeyondTheSkySecondaryRadiusEscapesTheSecondaryEffects() throws Exception {
        Entity fighter = placeFighter(skyHexSouth(8), 4);

        detonateAirBurst();

        assertFalse(fighter.isSufferingEMI(), "8 sky hexes out, beyond 7");
    }

    @Test
    void groundBurstOnTheSkyMapHitsFightersButLeavesTheMapAlone() throws Exception {
        Entity fighter = placeFighter(GROUND_ZERO, 4);
        int armorBefore = fighter.getTotalArmor();
        Hex hexBefore = skyBoard.getHex(GROUND_ZERO).duplicate();

        // A ground burst with a crater: on the sky map it digs nothing
        new NuclearDetonationResolver(gameManager).resolve(GROUND_ZERO, SKY_BOARD,
              NuclearBlastProfile.groundBurst(NuclearWeaponType.ALAMO), new Vector<Report>());

        assertTrue(isHit(fighter, armorBefore), "in the detonation's own sky hex");
        Hex hexAfter = skyBoard.getHex(GROUND_ZERO);
        assertEquals(hexBefore.getLevel(), hexAfter.getLevel(), "no crater");
        assertFalse(hexAfter.containsTerrain(Terrains.RUBBLE), "no rubble");
    }
}
