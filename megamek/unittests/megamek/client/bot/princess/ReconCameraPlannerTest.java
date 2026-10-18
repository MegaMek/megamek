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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import megamek.common.Player;
import megamek.common.actions.ReconCameraSpotAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * How a bot (Princess or CASPAR) uses a Recon Camera in the Off-Board phase: a ground unit spots the target it is
 * likeliest to hit; an aerospace unit spots only when it has no weapon whose attacks the spot would cost it.
 */
class ReconCameraPlannerTest {

    private static final int GROUND_BOARD = 0;
    private static final int SKY_BOARD = 1;
    private static final Coords SKY_HEX_OVER_GROUND = new Coords(3, 3);

    private Game game;
    private Player bot;
    private Player enemy;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        bot = new Player(0, "Bot");
        bot.setTeam(1);
        enemy = new Player(1, "Enemy");
        enemy.setTeam(2);
        game.addPlayer(0, bot);
        game.addPlayer(1, enemy);
        Board groundBoard = BoardLoader.initializeBoard("""
              size 20 20
              end""");
        Board skyBoard = Board.getSkyBoard(8, 8);
        groundBoard.setBoardId(GROUND_BOARD);
        skyBoard.setBoardId(SKY_BOARD);
        game.setBoard(GROUND_BOARD, groundBoard);
        game.setBoard(SKY_BOARD, skyBoard);
        skyBoard.setEmbeddedBoard(GROUND_BOARD, SKY_HEX_OVER_GROUND);
        groundBoard.setEnclosingBoard(SKY_BOARD);
        game.setPhase(GamePhase.OFFBOARD);
    }

    private BipedMek placeMek(String name, Player owner, Coords position) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(game.getNextEntityId());
        mek.setChassis(name);
        mek.setModel("T-1");
        Crew crew = new Crew(CrewType.SINGLE);
        crew.setGunnery(4, 0);
        mek.setCrew(crew);
        mek.setOwner(owner);
        mek.setBoardId(GROUND_BOARD);
        mek.setPosition(position);
        mek.setDeployed(true);
        game.addEntity(mek);
        return mek;
    }

    private AeroSpaceFighter placeCameraFighter() throws LocationFullException {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setGame(game);
        fighter.setId(game.getNextEntityId());
        fighter.setChassis("Spotter");
        fighter.setModel("S-1");
        fighter.setCrew(new Crew(CrewType.SINGLE));
        fighter.setOwner(bot);
        fighter.setBoardId(SKY_BOARD);
        fighter.setPosition(SKY_HEX_OVER_GROUND);
        fighter.setAltitude(7);
        fighter.setDeployed(true);
        fighter.addEquipment(EquipmentType.get("ISReconCamera"), Aero.LOC_NOSE);
        game.addEntity(fighter);
        return fighter;
    }

    @Test
    void testAGroundCameraSpotsTheEasiestTarget() throws LocationFullException {
        BipedMek camera = placeMek("Camera", bot, new Coords(5, 5));
        camera.addEquipment(EquipmentType.get("ISReconCamera"), Mek.LOC_CENTER_TORSO);
        placeMek("Far enemy", enemy, new Coords(5, 12));
        Entity nearEnemy = placeMek("Near enemy", enemy, new Coords(5, 8));

        ReconCameraSpotAction spot = ReconCameraPlanner.planSpot(game, camera);

        assertNotNull(spot);
        assertEquals(nearEnemy.getId(), spot.getTargetId(), "short range beats medium range");
    }

    @Test
    void testAUnitWithNothingToSpotDoesNotSpot() throws LocationFullException {
        BipedMek camera = placeMek("Camera", bot, new Coords(5, 5));
        camera.addEquipment(EquipmentType.get("ISReconCamera"), Mek.LOC_CENTER_TORSO);

        assertNull(ReconCameraPlanner.planSpot(game, camera));
    }

    @Test
    void testAnUnarmedSpotterPlaneSpots() throws LocationFullException {
        AeroSpaceFighter fighter = placeCameraFighter();
        Entity groundEnemy = placeMek("Ground enemy", enemy, new Coords(4, 4));

        ReconCameraSpotAction spot = ReconCameraPlanner.planSpot(game, fighter);

        assertNotNull(spot);
        assertEquals(groundEnemy.getId(), spot.getTargetId());
    }

    @Test
    void testAnArmedFighterKeepsItsAttacks() throws LocationFullException {
        AeroSpaceFighter fighter = placeCameraFighter();
        fighter.addEquipment(EquipmentType.get("ISMediumLaser"), Aero.LOC_NOSE);
        placeMek("Ground enemy", enemy, new Coords(4, 4));

        assertNull(ReconCameraPlanner.planSpot(game, fighter), "spotting from the air would cost its attacks");
    }
}
